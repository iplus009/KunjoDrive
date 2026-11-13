package com.novo.drive.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novo.drive.core.common.AppResult
import com.novo.drive.core.files.AppStoragePaths
import com.novo.drive.core.network.NetworkMonitor
import com.novo.drive.core.network.NetworkStatus
import com.novo.drive.core.permissions.PermissionChecker
import com.novo.drive.core.power.PowerMonitor
import com.novo.drive.core.publish.PublishScheduler
import com.novo.drive.core.security.AppLockManager
import com.novo.drive.core.transfer.BackupGate
import com.novo.drive.data.local.dao.FileDao
import com.novo.drive.data.local.dao.FolderDao
import com.novo.drive.data.repository.ActiveChannel
import com.novo.drive.domain.model.BackupTrigger
import com.novo.drive.domain.model.DriveChannel
import com.novo.drive.domain.model.DriveFolder
import com.novo.drive.domain.model.StorageSlice
import com.novo.drive.domain.repository.BackupRepository
import com.novo.drive.domain.repository.ChannelRepository
import com.novo.drive.domain.repository.FileRepository
import com.novo.drive.domain.repository.SettingsRepository
import com.novo.drive.domain.repository.SyncRepository
import com.novo.drive.domain.repository.TelegramAuthRepository
import com.novo.drive.domain.repository.TransferRepository
import com.novo.drive.domain.repository.TrashRepository
import com.novo.drive.presentation.common.IplusProtectedZipBundler
import com.novo.drive.presentation.common.UiText
import com.novo.drive.presentation.common.toUiText
import com.novo.drive.resources.Res
import com.novo.drive.resources.home_backing_up_new_files
import com.novo.drive.resources.home_nothing_new_to_back_up
import com.novo.drive.resources.home_queued_files
import com.novo.drive.resources.home_switched_drive
import java.io.File
import java.util.Calendar
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val fileRepository: FileRepository,
    private val transferRepository: TransferRepository,
    private val backupRepository: BackupRepository,
    private val syncRepository: SyncRepository,
    private val telegramAuthRepository: TelegramAuthRepository,
    private val networkMonitor: NetworkMonitor,
    private val permissionChecker: PermissionChecker,
    private val appLockManager: AppLockManager,
    private val settingsRepository: SettingsRepository,
    private val fileDao: FileDao,
    private val activeChannel: ActiveChannel,
    private val channelRepository: ChannelRepository,
    private val powerMonitor: PowerMonitor,
    private val folderDao: FolderDao,
    private val publishScheduler: PublishScheduler,
    private val trashRepository: TrashRepository,
    private val storagePaths: AppStoragePaths
) : ViewModel() {

    private val missingPermissions = MutableStateFlow(permissionChecker.missingCritical())
    private val _scanning = MutableStateFlow(false)
    private val _messages = MutableSharedFlow<UiText>(extraBufferCapacity = 4)

    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()
    val messages: SharedFlow<UiText> = _messages.asSharedFlow()

    private val activeDrive: Flow<DriveChannel?> = activeChannel.observe()
        .flatMapLatest { chatId ->
            channelRepository.observeChannels().map { channels ->
                channels.firstOrNull { it.chatId == chatId }
            }
        }

    val driveCount: StateFlow<Int> = channelRepository.observeChannels()
        .map { it.size }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1)

    fun cycleDrive(direction: Int) {
        viewModelScope.launch {
            if (settingsRepository.preferences.first().isFamilyMember) return@launch
            val channels = channelRepository.observeChannels().first()
            if (channels.size < 2) return@launch
            val index = channels.indexOfFirst { it.isActive }
            if (index < 0) return@launch
            val target = channels[(index + direction).mod(channels.size)]
            if (channelRepository.switchTo(target.chatId) is AppResult.Success) {
                _messages.tryEmit(
                    UiText.Resource(Res.string.home_switched_drive, target.displayName)
                )
            }
        }
    }

    @OptIn(FlowPreview::class)
    private val counts: Flow<HomeCounts> = activeChannel.observe().flatMapLatest { chatId ->
        fileDao.observeHomeAggregates(chatId)
            .debounce(COUNTS_DEBOUNCE_MS.milliseconds)
            .distinctUntilChanged()
            .map { aggregates ->
                HomeCounts(
                    total = aggregates.total,
                    remoteBytes = aggregates.remoteBytes,
                    backedUp = aggregates.backedUp,
                    pending = aggregates.queued,
                    failed = aggregates.failed,
                    localOnly = aggregates.localOnly,
                    offlineBytes = aggregates.offlineBytes
                )
            }
    }

    private val countsAndStorage: Flow<Triple<HomeCounts, List<StorageSlice>, Long?>> = combine(
        counts,
        fileRepository.observeStorageByCategory(),
        backupRepository.observeLastBackupAt()
    ) { totals, slices, lastBackupAt -> Triple(totals, slices, lastBackupAt) }

    private val recentFoldersFlow: Flow<List<DriveFolder>> = settingsRepository.preferences
        .map { it.showArchivedFiles }
        .distinctUntilChanged()
        .flatMapLatest { showArchived ->
            fileRepository.observeRecentFolders(
                showHidden = false,
                showArchived = showArchived,
                limit = 4
            )
        }

    private val recentsAndFolders: Flow<Triple<List<com.novo.drive.domain.model.DriveFile>, List<DriveFolder>, List<DriveFolder>>> =
        combine(
            fileRepository.observeRecent(12),
            recentFoldersFlow,
            fileRepository.observeFavoriteFolders()
        ) { recents, recentFolders, favorites -> Triple(recents, recentFolders, favorites) }

    val uiState: StateFlow<HomeUiState> = combine(
        countsAndStorage,
        recentsAndFolders,
        transferRepository.observeActiveCount(),
        combine(
            backupRepository.observeActiveSession(),
            telegramAuthRepository.connectionState,
            networkMonitor.status,
            missingPermissions,
            combine(
                settingsRepository.preferences,
                syncRepository.syncing,
                activeDrive,
                powerMonitor.charging,
                ::HomeSettings
            )
        ) { session, connection, network, missing, settings ->
            val (prefs, syncing, drive, charging) = settings
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            HomeMisc(
                syncing = syncing,
                session = session,
                connection = connection,
                network = network,
                missing = missing,
                activeDrive = drive,
                autoBackupEnabled = prefs.autoBackupEnabled,
                appLockEnabled = prefs.appLockEnabled,
                showArchivedSection = prefs.showArchivedFiles,
                showHiddenSection = prefs.showHiddenFiles,
                showRecentSection = prefs.showRecentFiles,
                backupHold = BackupGate.hold(prefs, network, charging, currentHour),
                isFamilyMember = prefs.isFamilyMember,
                familyQuotaBytes = prefs.familyQuotaBytes,
                encryptFiles = prefs.encryptFiles
            )
        }
    ) { countsWithStorage, folderGroup, activeTransfers, misc ->
        val (counts, storage, lastBackupAt) = countsWithStorage
        val (recents, recentFolders, favorites) = folderGroup
        val (syncing, session, connection, network, missing, drive) = misc
        HomeUiState(
            loading = false,
            connection = connection,
            offline = network == NetworkStatus.UNAVAILABLE,
            totalFiles = counts.total,
            remoteBytes = counts.remoteBytes,
            backedUpCount = counts.backedUp,
            pendingCount = counts.pending,
            localOnlyCount = counts.localOnly,
            failedCount = counts.failed,
            recentFiles = recents,
            recentFolders = recentFolders,
            favoriteFolders = favorites,
            activeBackup = session,
            missingPermissions = missing,
            backupFoldersSelected = drive?.backupFolders?.isNotEmpty() == true,
            activeChannel = drive,
            storage = storage,
            autoBackupEnabled = misc.autoBackupEnabled,
            lastBackupAt = lastBackupAt,
            appLockEnabled = misc.appLockEnabled,
            rebuilding = syncing,
            showArchivedSection = misc.showArchivedSection,
            showHiddenSection = misc.showHiddenSection,
            showRecentSection = misc.showRecentSection,
            activeTransferCount = activeTransfers,
            offlineBytes = counts.offlineBytes,
            backupHold = misc.backupHold,
            isFamilyMember = misc.isFamilyMember,
            familyQuotaBytes = misc.familyQuotaBytes,
            encryptFiles = misc.encryptFiles
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun createRootFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            when (val res = fileRepository.createFolder(null, name.trim())) {
                is AppResult.Success -> _messages.tryEmit(UiText.Plain("'${res.value.name}' ফোল্ডার তৈরি হয়েছে"))
                is AppResult.Failure -> _messages.tryEmit(res.error.toUiText())
            }
        }
    }

    fun renameFolder(folderId: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            when (val res = fileRepository.renameFolder(folderId, newName.trim())) {
                is AppResult.Success -> _messages.tryEmit(UiText.Plain("ফোল্ডারের নাম পরিবর্তন করা হয়েছে"))
                is AppResult.Failure -> _messages.tryEmit(res.error.toUiText())
            }
        }
    }

    fun moveFolderToRoot(folder: DriveFolder) {
        if (folder.parentId == null) {
            _messages.tryEmit(UiText.Plain("ফোল্ডারটি ইতিমধ্যেই মূল রুট ফোল্ডারে রয়েছে"))
            return
        }
        viewModelScope.launch {
            when (val res = fileRepository.moveFolder(folder.id, null)) {
                is AppResult.Success -> _messages.tryEmit(UiText.Plain("'${folder.name}' রুট ড্রাইভে সরানো হয়েছে"))
                is AppResult.Failure -> _messages.tryEmit(res.error.toUiText())
            }
        }
    }

    fun duplicateFolder(folder: DriveFolder) {
        viewModelScope.launch {
            when (val res = fileRepository.duplicateFolder(folder.id)) {
                is AppResult.Success -> _messages.tryEmit(UiText.Plain("'${res.value.name}' কপি তৈরি হয়েছে"))
                is AppResult.Failure -> _messages.tryEmit(res.error.toUiText())
            }
        }
    }

    fun toggleArchiveFolder(folder: DriveFolder) {
        viewModelScope.launch {
            val targetArchived = !folder.isArchived
            fileRepository.setFolderArchived(folder.id, targetArchived)
            _messages.tryEmit(
                UiText.Plain(
                    if (targetArchived) "'${folder.name}' আর্কাইভে সংরক্ষিত হয়েছে"
                    else "'${folder.name}' আর্কাইভ থেকে ফিরিয়ে আনা হয়েছে"
                )
            )
        }
    }

    fun trashFolder(folder: DriveFolder) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                trashRepository.moveFolderToTrash(folder.id)
            }
            _messages.tryEmit(UiText.Plain("'${folder.name}' ট্র্যাশে সরানো হয়েছে"))
        }
    }

    fun createProtectedFolderZip(
        folder: DriveFolder,
        password: String,
        onZipReady: (String) -> Unit
    ) {
        viewModelScope.launch {
            val ids = fileRepository.fileIdsInTree(folder.id)
            val files = fileRepository.filesByIds(ids)
            val localPaths = files.mapNotNull { it.localPath }.filter { File(it).exists() }
            if (localPaths.isEmpty()) {
                _messages.tryEmit(UiText.Plain("এই ফোল্ডারে কোনো লোকাল কপি নেই। আগে ফাইল অফলাইনে ডাউনলোড করুন।"))
                return@launch
            }
            val outDir = File(storagePaths.cacheDir, "protected_zips")
            val created = IplusProtectedZipBundler.createAes256Zip(
                outputDir = outDir,
                archiveBaseName = folder.name,
                localFilePaths = localPaths,
                password = password
            )
            if (created != null) {
                onZipReady(created.absolutePath)
            } else {
                _messages.tryEmit(UiText.Plain("পাসওয়ার্ড .ZIP তৈরি করা সম্ভব হয়নি"))
            }
        }
    }

    fun notifyMessage(text: String) {
        _messages.tryEmit(UiText.Plain(text))
    }

    /** A folder scan never reaches files added by hand, or canceled or failed on their way up. */
    fun backUpPending() {
        if (_scanning.value) return
        viewModelScope.launch {
            _scanning.value = true
            val message = when (val result = transferRepository.enqueuePendingUploads()) {
                is AppResult.Success -> if (result.value == 0) {
                    UiText.Resource(Res.string.home_nothing_new_to_back_up)
                } else {
                    UiText.PluralResource(
                        Res.plurals.home_queued_files, result.value,
                        result.value
                    )
                }

                is AppResult.Failure -> result.error.toUiText()
            }
            _scanning.value = false
            _messages.tryEmit(message)
        }
    }

    val pendingSync: StateFlow<PendingSync> = combine(
        fileDao.observePendingPublishCount(),
        folderDao.observePendingPublishCount()
    ) { files, folders -> PendingSync(files, folders > 0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PendingSync())

    fun syncNow() = publishScheduler.kick()

    fun scanNow() {
        if (_scanning.value) return
        viewModelScope.launch {
            _scanning.value = true
            val message = when (val result = backupRepository.startBackup(BackupTrigger.MANUAL)) {
                is AppResult.Success ->
                    if (result.value == null) {
                        UiText.Resource(Res.string.home_nothing_new_to_back_up)
                    } else {
                        UiText.Resource(Res.string.home_backing_up_new_files)
                    }

                is AppResult.Failure -> result.error.toUiText()
            }
            _scanning.value = false
            _messages.tryEmit(message)
        }
    }

    fun pauseBackup(sessionId: String) {
        viewModelScope.launch { backupRepository.pauseBackup(sessionId) }
    }

    fun resumeBackup(sessionId: String) {
        viewModelScope.launch { backupRepository.resumeBackup(sessionId) }
    }

    fun cancelBackup(sessionId: String) {
        viewModelScope.launch { backupRepository.cancelBackup(sessionId) }
    }

    fun lockNow() = appLockManager.lockNow()

    fun refreshPermissions() {
        missingPermissions.value = permissionChecker.missingCritical()
    }

    private companion object {
        const val COUNTS_DEBOUNCE_MS = 300L
    }
}
