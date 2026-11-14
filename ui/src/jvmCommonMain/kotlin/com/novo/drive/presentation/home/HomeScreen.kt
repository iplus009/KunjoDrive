package com.novo.drive.presentation.home

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLocaleList
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.novo.drive.domain.model.AppTheme
import com.novo.drive.domain.model.DriveFolder
import com.novo.drive.core.bridge.KunjoBridge
import com.novo.drive.presentation.components.custom.DynamicStorageIsland
import com.novo.drive.presentation.music.IplusMiniPlayer
import com.novo.drive.presentation.navigation.LocalBottomBarInset
import org.koin.compose.koinInject
import com.novo.drive.presentation.components.FolderInfoSheet
import com.novo.drive.presentation.components.RenameDialog
import com.novo.drive.presentation.platform.LocalFileSharer

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import org.jetbrains.compose.resources.painterResource
import com.novo.drive.resources.ic_launcher

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import com.novo.drive.domain.model.FileCategory
import com.novo.drive.presentation.components.glass.LiquidGlassCard
import com.novo.drive.presentation.components.glass.SecureVaultPinDialog
import com.novo.drive.presentation.components.glass.CyberStorageGaugeCard
import com.novo.drive.presentation.components.glass.FolderActionBottomSheet
import com.novo.drive.presentation.theme.LocalIplusPalette
import androidx.compose.material.icons.filled.MoreVert
import com.novo.drive.presentation.components.glass.LiquidGlassButton
import com.novo.drive.presentation.components.glass.liquidGlass
import com.novo.drive.presentation.components.glass.liquidGlassBackground
import com.novo.drive.presentation.components.chartColor
import androidx.compose.ui.graphics.Color
import com.novo.drive.resources.common_category_archives
import com.novo.drive.resources.common_category_audio
import com.novo.drive.resources.common_category_documents
import com.novo.drive.resources.common_category_other
import com.novo.drive.resources.common_category_photos
import com.novo.drive.resources.common_category_videos
import com.novo.drive.resources.common_done
import com.novo.drive.resources.family_drive_how_it_works
import com.novo.drive.resources.family_drive_step1_desc
import com.novo.drive.resources.family_drive_step1_title
import com.novo.drive.resources.family_drive_step2_desc
import com.novo.drive.resources.family_drive_step2_title
import com.novo.drive.resources.family_drive_step3_desc
import com.novo.drive.resources.family_drive_step3_title
import com.novo.drive.resources.family_drive_tagline
import com.novo.drive.resources.family_drive_title
import com.novo.drive.resources.home_family_drive_subtitle
import com.novo.drive.resources.home_section_categories
import com.novo.drive.resources.home_section_family_drive
import com.novo.drive.resources.settings_channels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.LinearWavyProgressIndicator

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novo.drive.core.telegram.TelegramConnectionState
import com.novo.drive.domain.model.BackupHold
import com.novo.drive.domain.model.BackupSessionStatus
import com.novo.drive.domain.model.FileSortField
import com.novo.drive.domain.model.SortDirection
import com.novo.drive.domain.model.StorageSlice
import com.novo.drive.presentation.collection.CollectionType
import com.novo.drive.presentation.common.AgeBucket
import com.novo.drive.presentation.common.CollectSnackbarMessages
import com.novo.drive.presentation.common.Formatters
import com.novo.drive.presentation.common.add
import com.novo.drive.presentation.components.BottomBarSnackbarHost
import com.novo.drive.presentation.components.ConfirmDialog
import com.novo.drive.presentation.components.ConnectionDot
import com.novo.drive.presentation.components.ConnectionIndicator
import com.novo.drive.presentation.components.DriveAvatarSwitcher
import com.novo.drive.presentation.components.FileThumbnail
import com.novo.drive.presentation.components.GroupedList
import com.novo.drive.presentation.components.LoadingState
import com.novo.drive.presentation.components.PermissionWarningCard
import com.novo.drive.presentation.components.keyboardCollection
import com.novo.drive.presentation.components.keyboardCollectionItem
import com.novo.drive.presentation.components.label
import com.novo.drive.presentation.components.rememberKeyboardCollection
import com.novo.drive.presentation.navigation.LocalBottomBarInset
import com.novo.drive.presentation.platform.LocalPermissionRequester
import com.novo.drive.presentation.platform.LocalPlatformCapabilities
import com.novo.drive.presentation.preview.PreviewSequence
import com.novo.drive.resources.Res
import com.novo.drive.resources.app_backed_up
import com.novo.drive.resources.app_name
import com.novo.drive.resources.backup_age_days
import com.novo.drive.resources.backup_age_hours
import com.novo.drive.resources.backup_age_minutes
import com.novo.drive.resources.collection_offline_on_device
import com.novo.drive.resources.common_cancel
import com.novo.drive.resources.common_pause
import com.novo.drive.resources.common_resume
import com.novo.drive.resources.file_count
import com.novo.drive.resources.home_active_transfers
import com.novo.drive.resources.home_back_up_pending
import com.novo.drive.resources.home_backing_up
import com.novo.drive.resources.home_backup_all_in_telegram
import com.novo.drive.resources.home_backup_at
import com.novo.drive.resources.home_backup_auto_off
import com.novo.drive.resources.home_backup_just_now
import com.novo.drive.resources.home_backup_never
import com.novo.drive.resources.home_backup_nothing_to_back_up
import com.novo.drive.resources.home_backup_paused
import com.novo.drive.resources.home_backup_waiting_charger
import com.novo.drive.resources.home_backup_waiting_wifi
import com.novo.drive.resources.home_cancel_backup_action
import com.novo.drive.resources.home_cancel_backup_title
import com.novo.drive.resources.home_choose_folders
import com.novo.drive.resources.home_deleted_files_kept_removal
import com.novo.drive.resources.home_failed_count
import com.novo.drive.resources.home_lock_app
import com.novo.drive.resources.home_not_backed_up_count
import com.novo.drive.resources.home_open_drives
import com.novo.drive.resources.home_queued_uploads_discarded_files
import com.novo.drive.resources.home_saving_changes
import com.novo.drive.resources.home_saving_changes_left
import com.novo.drive.resources.home_saving_folder_changes
import com.novo.drive.resources.home_scan_now
import com.novo.drive.resources.home_scanning
import com.novo.drive.resources.home_section_favorites
import com.novo.drive.resources.home_section_recent
import com.novo.drive.resources.home_section_storage
import com.novo.drive.resources.home_session_progress
import com.novo.drive.resources.home_status_connected
import com.novo.drive.resources.home_status_connecting
import com.novo.drive.resources.home_status_offline
import com.novo.drive.resources.home_status_rebuilding
import com.novo.drive.resources.home_status_syncing
import com.novo.drive.resources.home_storage_percent
import com.novo.drive.resources.home_storage_percent_min
import com.novo.drive.resources.home_sync_now
import com.novo.drive.resources.home_transfer_history
import com.novo.drive.resources.home_transfer_history_subtitle
import com.novo.drive.resources.home_waiting_count
import com.novo.drive.resources.trash
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    onOpenFile: (String, PreviewSequence) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenRootFiles: () -> Unit,
    onOpenCategory: (FileCategory) -> Unit,
    onOpenTransfers: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenChannels: () -> Unit,
    onOpenCollection: (CollectionType) -> Unit,
    onOpenBackupSettings: () -> Unit,
    onOpenFamilyDashboard: () -> Unit,
    onOpenStorageCleaner: () -> Unit,
    onOpenMusicPlayer: () -> Unit,
    onOpenStreamBridge: () -> Unit,

    onOpenDecoyVault: () -> Unit,
    onOpenLocalFileManager: () -> Unit = {},
    onOpenCinema: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel()
) {
    val clipboard = LocalClipboardManager.current
    val fileSharer = LocalFileSharer.current
    val isBn = LocalLocaleList.current.firstOrNull()?.language?.startsWith("bn") == true
    var showVaultPinDialog by remember { mutableStateOf(false) }
    var selectedFolderForSheet by remember { mutableStateOf<DriveFolder?>(null) }
    var renameTargetFolder by remember { mutableStateOf<DriveFolder?>(null) }
    var infoTargetFolder by remember { mutableStateOf<DriveFolder?>(null) }
    var deleteTargetFolder by remember { mutableStateOf<DriveFolder?>(null) }
    var zipShareTargetFolder by remember { mutableStateOf<DriveFolder?>(null) }
    var showCreateRootFolderDialog by remember { mutableStateOf(false) }

    if (showVaultPinDialog) {
        SecureVaultPinDialog(
            onDismiss = { showVaultPinDialog = false },
            onUnlocked = {
                showVaultPinDialog = false
                onOpenCollection(CollectionType.HIDDEN)
            },
            onDecoyUnlocked = {
                showVaultPinDialog = false
                onOpenDecoyVault()
            }
        )
    }

    selectedFolderForSheet?.let { folder ->
        FolderActionBottomSheet(
            title = folder.name,
            subtitle = "${Formatters.bytes(folder.totalBytes)} • ${folder.folderCount} Folders • ${folder.fileCount} Files",
            isArchived = folder.isArchived,
            onDismiss = { selectedFolderForSheet = null },
            onRename = { renameTargetFolder = folder },
            onMove = { viewModel.moveFolderToRoot(folder) },
            onDuplicate = { viewModel.duplicateFolder(folder) },
            onArchive = { viewModel.toggleArchiveFolder(folder) },
            onShare = { zipShareTargetFolder = folder },
            onCopyLink = {
                clipboard.setText(AnnotatedString("kunjo://drive/folder/${folder.id}/${folder.name}"))
                viewModel.notifyMessage(
                    if (isBn) "'${folder.name}' এর কুঞ্জ ক্লাউড পাথ কপি হয়েছে" else "Copied Kunjo cloud path for '${folder.name}'"
                )
            },
            onDetail = { infoTargetFolder = folder },
            onDelete = { deleteTargetFolder = folder }
        )
    }

    renameTargetFolder?.let { folder ->
        RenameDialog(
            title = if (isBn) "ফোল্ডারের নাম পরিবর্তন" else "Rename folder",
            initialValue = folder.name,
            confirmLabel = if (isBn) "সেভ করুন" else "Save",
            onConfirm = { newName ->
                renameTargetFolder = null
                viewModel.renameFolder(folder.id, newName)
            },
            onDismiss = { renameTargetFolder = null }
        )
    }

    infoTargetFolder?.let { folder ->
        FolderInfoSheet(
            folder = folder,
            onDismiss = { infoTargetFolder = null }
        )
    }

    deleteTargetFolder?.let { folder ->
        ConfirmDialog(
            title = if (isBn) "'${folder.name}' মুছে ফেলবেন?" else "Delete '${folder.name}'?",
            message = if (isBn) {
                "ফোল্ডারটি ট্র্যাশে সরানো হবে। প্রয়োজনে ট্র্যাশ থেকে পুনরুদ্ধার করতে পারবেন।"
            } else {
                "This folder will be moved to trash. You can restore it later if needed."
            },
            confirmLabel = if (isBn) "ট্র্যাশে পাঠান" else "Move to Trash",
            destructive = true,
            onConfirm = {
                deleteTargetFolder = null
                viewModel.trashFolder(folder)
            },
            onDismiss = { deleteTargetFolder = null }
        )
    }

    zipShareTargetFolder?.let { folder ->
        var zipPassword by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { zipShareTargetFolder = null },
            title = {
                Text(
                    if (isBn) "'${folder.name}' • পাসওয়ার্ড লকড .ZIP" else "'${folder.name}' • Password-Protected .ZIP"
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isBn) {
                            "এই ফোল্ডারের ডাউনলোডকৃত ফাইলগুলোকে AES-256 পাসওয়ার্ডযুক্ত .ZIP বান্ডেলে শেয়ার করুন:"
                        } else {
                            "Lock downloaded files in this folder into an AES-256 password-protected .ZIP bundle to share:"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = zipPassword,
                        onValueChange = { zipPassword = it },
                        label = { Text(if (isBn) "পাসওয়ার্ড (কমপক্ষে ৪ অক্ষর)" else "Password (min 4 characters)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = folder
                        val pass = zipPassword
                        zipShareTargetFolder = null
                        viewModel.createProtectedFolderZip(target, pass) { zipPath ->
                            fileSharer.share(listOf(zipPath), "application/zip", "Share Protected Folder ZIP")
                        }
                    },
                    enabled = zipPassword.length >= 4,
                    shapes = ButtonDefaults.shapes()
                ) { Text(if (isBn) "লক ও শেয়ার" else "Lock & Share") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { zipShareTargetFolder = null },
                    shapes = ButtonDefaults.shapes()
                ) { Text(if (isBn) "বাতিল" else "Cancel") }
            }
        )
    }

    if (showCreateRootFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateRootFolderDialog = false },
            title = { Text(if (isBn) "নতুন ক্লাউড ফোল্ডার তৈরি" else "Create New Cloud Folder") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text(if (isBn) "ফোল্ডারের নাম" else "Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCreateRootFolderDialog = false
                        viewModel.createRootFolder(folderName)
                    },
                    enabled = folderName.isNotBlank(),
                    shapes = ButtonDefaults.shapes()
                ) { Text(if (isBn) "তৈরি করুন" else "Create") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCreateRootFolderDialog = false },
                    shapes = ButtonDefaults.shapes()
                ) { Text(if (isBn) "বাতিল" else "Cancel") }
            }
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recentState = rememberLazyListState()
    val recentKeyboard = rememberKeyboardCollection(recentState, horizontal = true)
    var lastConnectionStatus by remember { mutableStateOf<ConnectionStatus?>(null) }
    val scanning by viewModel.scanning.collectAsStateWithLifecycle()
    val pendingSync by viewModel.pendingSync.collectAsStateWithLifecycle()
    var shownSync by remember { mutableStateOf(pendingSync) }
    if (pendingSync.visible) shownSync = pendingSync
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionRequester = LocalPermissionRequester.current
    val capabilities = LocalPlatformCapabilities.current

    var confirmCancelBackup by remember { mutableStateOf<String?>(null) }
    confirmCancelBackup?.let { sessionId ->
        ConfirmDialog(
            title = stringResource(Res.string.home_cancel_backup_title),
            message = stringResource(Res.string.home_queued_uploads_discarded_files),
            confirmLabel = stringResource(Res.string.home_cancel_backup_action),
            destructive = true,
            onConfirm = {
                confirmCancelBackup = null
                viewModel.cancelBackup(sessionId)
            },
            onDismiss = { confirmCancelBackup = null }
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    CollectSnackbarMessages(viewModel.messages, snackbarHostState)

    val paletteTop = LocalIplusPalette.current
    val isLightTheme = paletteTop.theme == AppTheme.LIGHT
    val topPrimaryText = if (isLightTheme) Color(0xFF0F172A) else Color.White
    val topSecondaryText = if (isLightTheme) Color(0xFF475569) else Color.White.copy(alpha = 0.65f)
    val kunjoBridge = koinInject<KunjoBridge>()
    val activeMiniAudio by kunjoBridge.media.currentAudio.collectAsStateWithLifecycle()
    val miniPlayerExtraBottom = if (activeMiniAudio != null) 78.dp else 0.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { BottomBarSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(Res.drawable.ic_launcher),
                            contentDescription = null,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(Res.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = topPrimaryText
                            )
                            val status = rememberConnectionStatus(
                                offline = state.offline,
                                connection = state.connection,
                                known = !state.loading
                            )
                            val shown = status ?: lastConnectionStatus
                            if (shown != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ConnectionDot(status = shown.indicator)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(shown.labelRes),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = topSecondaryText
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    if (state.appLockEnabled) {
                        IconButton(onClick = viewModel::lockNow) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = stringResource(Res.string.home_lock_app),
                                tint = topPrimaryText
                            )
                        }
                    }
                    if (!state.isFamilyMember) {
                        state.activeChannel?.let { channel ->
                            val driveCount by viewModel.driveCount.collectAsStateWithLifecycle()
                            IconButton(
                                onClick = onOpenChannels,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                DriveAvatarSwitcher(
                                    channel = channel,
                                    canCycle = driveCount > 1,
                                    onCycle = viewModel::cycleDrive,
                                    size = AVATAR_SIZE,
                                    contentDescription = stringResource(
                                        Res.string.home_open_drives
                                    )
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = if (isLightTheme) Color.White.copy(alpha = 0.94f) else paletteTop.surfaceColor.copy(alpha = 0.92f)
                ),
                windowInsets = WindowInsets.statusBars
            )
        }
    ) { padding ->
        if (state.loading) {
            LoadingState(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding.add(
                horizontal = 16.dp,
                top = 8.dp,
                bottom = 36.dp + LocalBottomBarInset.current + navBarsBottom + miniPlayerExtraBottom
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.missingPermissions.isNotEmpty()) {
                item(key = "permissions") {
                        Column {
                        PermissionWarningCard(
                            missing = state.missingPermissions,
                            onGrant = {
                                permissionRequester.request(state.missingPermissions) {
                                    viewModel.refreshPermissions()
                                }
                            }
                        )
                    }
                }
            }
            if (capabilities.supportsAutoBackup) {
                item(key = "backup") {
                    Column {
                        BackupCard(
                            state = state,
                            scanning = scanning,
                            onChooseFolders = onOpenBackupSettings,
                            onScanNow = viewModel::scanNow,
                            onBackUpPending = viewModel::backUpPending,
                            onPause = viewModel::pauseBackup,
                            onResume = viewModel::resumeBackup,
                            onCancel = { confirmCancelBackup = it }
                        )
                    }
                }
            }
            if (pendingSync.visible) {
                item(key = "pending-sync") {
                    Column {
                        PendingSyncCard(pending = shownSync, onSyncNow = viewModel::syncNow)
                    }
                }
            }
            item(key = "storage-gauge-top") {
                Column {
                    DynamicStorageIsland(
                        usedBytes = state.storage.sumOf { it.totalBytes },
                        totalQuotaBytes = state.familyQuotaBytes,
                        fileCount = state.storage.sumOf { it.fileCount },
                        cloudStatusText = if (state.rebuilding) (if (isBn) "সিঙ্ক হচ্ছে..." else "Syncing...") else (if (isBn) "কুঞ্জ ক্লাউড সুরক্ষিত" else "Kunjo cloud secured"),
                        onSyncClick = viewModel::syncNow
                    )
                }
            }
            item(key = "kunjo-power-suite") {
                Column {
                    KunjoPowerSuiteDeck(
                        isFamilyMember = state.isFamilyMember,
                        primaryText = topPrimaryText,
                        secondaryText = topSecondaryText,
                        onOpenFamilyDashboard = onOpenFamilyDashboard,
                        onOpenStorageCleaner = onOpenStorageCleaner,
                        onOpenMusicPlayer = onOpenMusicPlayer,
                        onOpenStreamBridge = onOpenStreamBridge,
                        onOpenLocalFileManager = onOpenLocalFileManager,
                        onOpenCinema = onOpenCinema
                    )
                }
            }
            item(key = "smart-categories") {
                Column {
                    SmartCategoriesSection(
                        slices = state.storage,
                        primaryText = topPrimaryText,
                        secondaryText = topSecondaryText,
                        onOpenCategory = onOpenCategory
                    )
                }
            }
            item(key = "recently-folder-viewed") {
                val palette = LocalIplusPalette.current
                val folderColors = listOf(
                    palette.folderDocsColor,
                    palette.folderVideosColor,
                    palette.folderPhotosColor,
                    palette.folderSongsColor
                )
                val maxFolderBytes = remember(state.recentFolders) {
                    state.recentFolders.maxOfOrNull { it.totalBytes }?.coerceAtLeast(1L) ?: 1L
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBn) "সাম্প্রতিক ফোল্ডার" else "Recent folders",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = topPrimaryText
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val chipShape = RoundedCornerShape(8.dp)
                            Box(
                                modifier = Modifier
                                    .clip(chipShape)
                                    .background(palette.accentSecondary.copy(alpha = 0.14f))
                                    .border(1.dp, palette.accentSecondary.copy(alpha = 0.40f), chipShape)
                                    .clickable { showCreateRootFolderDialog = true }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isBn) "+ নতুন" else "+ New",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.accentSecondary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(chipShape)
                                    .background(palette.accentPrimary.copy(alpha = 0.14f))
                                    .border(1.dp, palette.accentPrimary.copy(alpha = 0.40f), chipShape)
                                    .clickable { onOpenRootFiles() }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isBn) "সব দেখুন" else "View all",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = palette.accentPrimary
                                )
                            }
                        }
                    }

                    if (state.recentFolders.isEmpty()) {
                        LiquidGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            onClick = { showCreateRootFolderDialog = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(palette.accentPrimary.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CreateNewFolder,
                                        contentDescription = null,
                                        tint = palette.accentPrimary
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isBn) "প্রথম ক্লাউড ফোল্ডার তৈরি করুন" else "Create your first cloud folder",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = topPrimaryText
                                    )
                                    Text(
                                        text = if (isBn) "ফাইলগুলো সুন্দরভাবে সাজাতে একটি নতুন ফোল্ডার খুলুন" else "Create a folder to organise your files",
                                        fontSize = 12.sp,
                                        color = topSecondaryText
                                    )
                                }
                            }
                        }
                    } else {
                        state.recentFolders.chunked(2).forEachIndexed { rowIdx, rowFolders ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowFolders.forEachIndexed { colIdx, folder ->
                                    val fColor = folderColors[(rowIdx * 2 + colIdx) % folderColors.size]
                                    val barFill = if (folder.totalBytes > 0L) {
                                        (folder.totalBytes.toFloat() / maxFolderBytes.toFloat()).coerceIn(0.12f, 1f)
                                    } else if (folder.fileCount > 0) 0.25f else 0.06f

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(18.dp))
                                            .liquidGlassBackground(
                                                shape = RoundedCornerShape(18.dp),
                                                tintAlpha = 0.72f,
                                                borderAlpha = 0.12f,
                                                borderWidth = 1.dp
                                            )
                                            .clickable(role = Role.Button) { onOpenFolder(folder.id) }
                                            .padding(14.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(fColor.copy(alpha = 0.16f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Folder,
                                                        contentDescription = null,
                                                        tint = fColor,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(CircleShape)
                                                        .clickable(role = Role.Button) {
                                                            selectedFolderForSheet = folder
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.MoreVert,
                                                        contentDescription = "Folder Actions",
                                                        tint = topSecondaryText,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Column {
                                                Text(
                                                    text = folder.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = topPrimaryText,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val folderFilesLabel = if (isBn) "${folder.fileCount}টি ফাইল" else "${folder.fileCount} files"
                                                Text(
                                                    text = "${Formatters.bytes(folder.totalBytes)} • $folderFilesLabel",
                                                    fontSize = 10.sp,
                                                    color = topSecondaryText,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(3.dp)
                                                    .clip(RoundedCornerShape(1.5.dp))
                                                    .background(topPrimaryText.copy(alpha = 0.10f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(barFill)
                                                        .fillMaxHeight()
                                                        .background(fColor)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (rowFolders.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
            item(key = "secure-vault") {
                Column {
                    SecureVaultCard(
                        onOpenVault = { showVaultPinDialog = true }
                    )
                }
            }
            if (state.favoriteFolders.isNotEmpty()) {
                item(key = "favorites") {
                    Column {
                        SectionHeader(stringResource(Res.string.home_section_favorites))
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            state.favoriteFolders.take(5).forEach { folder ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable(role = Role.Button) { onOpenFolder(folder.id) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        folder.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (state.showRecentSection && state.recentFiles.isNotEmpty()) {
                item(key = "recent") {
                    Column {
                        SectionHeader(stringResource(Res.string.home_section_recent))
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            state = recentState,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.keyboardCollection(
                                recentKeyboard,
                                state.recentFiles.size
                            )
                        ) {
                            itemsIndexed(state.recentFiles, key = { _, it -> it.id }) { index, file ->
                                Column(
                                    modifier = Modifier
                                        .keyboardCollectionItem(recentKeyboard, index)
                                        .width(112.dp)
                                        .clip(MaterialTheme.shapes.large)
                                        .liquidGlassBackground(
                                            shape = MaterialTheme.shapes.large,
                                            tintAlpha = 0.55f,
                                            borderAlpha = 0.30f
                                        )
                                        .clickable(role = Role.Button) {
                                            onOpenFile(
                                                file.id,
                                                PreviewSequence(
                                                    sortField = FileSortField.DATE_ADDED,
                                                    sortDirection = SortDirection.DESCENDING
                                                )
                                            )
                                        }
                                        .padding(6.dp)
                                ) {
                                    FileThumbnail(
                                        file = file,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(MaterialTheme.shapes.medium)
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "collections") {
                Column {
                    CollectionLinks(
                        showArchived = state.showArchivedSection,
                        showHidden = state.showHiddenSection,
                        activeTransferCount = state.activeTransferCount,
                        offlineBytes = state.offlineBytes,
                        onOpenCollection = onOpenCollection,
                        onOpenTransfers = onOpenTransfers,
                        onOpenTrash = onOpenTrash,
                        onOpenVault = { showVaultPinDialog = true }
                    )
                }
            }
        }
    }
        IplusMiniPlayer(
            bridge = kunjoBridge,
            onOpenStudio = onOpenMusicPlayer,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = LocalBottomBarInset.current + 8.dp)
        )
    }
}

@Composable
private fun CollectionLinks(
    showArchived: Boolean,
    showHidden: Boolean,
    activeTransferCount: Int,
    offlineBytes: Long,
    onOpenCollection: (CollectionType) -> Unit,
    onOpenTransfers: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenVault: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CollectionType.entries.forEach { collection ->
            val visible = when (collection) {
                CollectionType.ARCHIVED -> showArchived
                // Hidden Vault already has its dedicated SecureVaultCard directly above
                CollectionType.HIDDEN -> false
                else -> true
            }
            if (visible) {
                CollectionRow(
                    icon = collection.icon,
                    title = stringResource(collection.titleRes),
                    subtitle = if (
                        collection == CollectionType.AVAILABLE_OFFLINE && offlineBytes > 0
                    ) {
                        stringResource(
                            Res.string.collection_offline_on_device,
                            Formatters.bytes(offlineBytes)
                        )
                    } else {
                        stringResource(collection.subtitleRes)
                    },
                    onClick = {
                        if (collection == CollectionType.HIDDEN) {
                            onOpenVault()
                        } else {
                            onOpenCollection(collection)
                        }
                    }
                )
            }
        }
        CollectionRow(
            icon = Icons.Filled.SwapVert,
            title = stringResource(Res.string.home_transfer_history),
            subtitle = if (activeTransferCount > 0) {
                stringResource(Res.string.home_active_transfers, activeTransferCount)
            } else {
                stringResource(Res.string.home_transfer_history_subtitle)
            },
            onClick = onOpenTransfers
        )
        CollectionRow(
            icon = Icons.Filled.Delete,
            title = stringResource(Res.string.trash),
            subtitle = stringResource(Res.string.home_deleted_files_kept_removal),
            onClick = onOpenTrash
        )
    }
}

@Composable
private fun PendingSyncCard(pending: PendingSync, onSyncNow: () -> Unit) {
    GroupedList(horizontalPadding = 0.dp) {
        add {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.home_saving_changes),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = if (pending.files > 0) {
                            pluralStringResource(
                                Res.plurals.home_saving_changes_left,
                                pending.files,
                                pending.files
                            )
                        } else {
                            stringResource(Res.string.home_saving_folder_changes)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onSyncNow, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(Res.string.home_sync_now))
                }
            }
        }
    }
}

@Composable
private fun CollectionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassBackground(
                shape = MaterialTheme.shapes.large,
                tintAlpha = 0.52f,
                borderAlpha = 0.28f
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { heading() }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BackupCard(
    state: HomeUiState,
    scanning: Boolean,
    onChooseFolders: () -> Unit,
    onScanNow: () -> Unit,
    onBackUpPending: () -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = 6.dp,
        tintAlpha = 0.65f,
        borderWidth = 1.dp
    ) {
        val statusIcon = when {
            state.rebuilding -> Icons.Filled.CloudSync
            state.failedCount > 0 -> Icons.Outlined.ErrorOutline
            state.activeBackup?.status == BackupSessionStatus.PAUSED -> Icons.Filled.CloudSync
            state.pendingCount > 0 -> Icons.Filled.CloudUpload
            state.offline -> Icons.Filled.CloudOff
            else -> Icons.Filled.CloudDone
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
                        )
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = when {
                            state.activeBackup?.status == BackupSessionStatus.RUNNING ->
                                if (state.backupHold == BackupHold.BEDTIME_WINDOW) {
                                    if (com.novo.drive.presentation.common.isBengaliUi()) {
                                        "রাতের বেডটাইম সিঙ্কের অপেক্ষায় (12 AM – 6 AM)"
                                    } else {
                                        "Waiting for bedtime sync window (12 AM – 6 AM)"
                                    }
                                } else {
                                    stringResource(
                                        when (state.backupHold) {
                                            BackupHold.CHARGER -> Res.string.home_backup_waiting_charger
                                            BackupHold.WIFI -> Res.string.home_backup_waiting_wifi
                                            BackupHold.BEDTIME_WINDOW, null -> Res.string.home_backing_up
                                        }
                                    )
                                }

                            state.activeBackup?.status == BackupSessionStatus.PAUSED ->
                                stringResource(Res.string.home_backup_paused)

                            state.rebuilding -> stringResource(Res.string.home_status_rebuilding)
                            state.totalFiles == 0 ->
                                stringResource(Res.string.home_backup_nothing_to_back_up)

                            state.failedCount > 0 ->
                                stringResource(Res.string.home_failed_count, state.failedCount)

                            state.pendingCount > 0 ->
                                stringResource(Res.string.home_waiting_count, state.pendingCount)

                            state.backedUpCount < state.totalFiles -> stringResource(
                                Res.string.home_not_backed_up_count,
                                state.totalFiles - state.backedUpCount
                            )

                            else -> stringResource(Res.string.app_backed_up)
                        },
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = backupFreshnessLabel(
                            autoBackupEnabled = state.autoBackupEnabled,
                            lastBackupAt = state.lastBackupAt,
                            totalFiles = state.totalFiles,
                            backedUpCount = state.backedUpCount
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            var lastSession by remember { mutableStateOf(state.activeBackup) }
            state.activeBackup?.let { lastSession = it }
            AnimatedVisibility(
                visible = state.activeBackup != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                lastSession?.let { session ->
                    Column {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val sessionButtonColors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                contentColor = MaterialTheme.colorScheme.primaryContainer
                            )
                            if (session.status == BackupSessionStatus.PAUSED) {
                                Button(
                                    onClick = { onResume(session.id) },
                                    shapes = ButtonDefaults.shapes(),
                                    colors = sessionButtonColors
                                ) { Text(stringResource(Res.string.common_resume)) }
                            } else {
                                Button(
                                    onClick = { onPause(session.id) },
                                    shapes = ButtonDefaults.shapes(),
                                    colors = sessionButtonColors
                                ) { Text(stringResource(Res.string.common_pause)) }
                            }
                            OutlinedButton(
                                onClick = { onCancel(session.id) },
                                shapes = ButtonDefaults.shapes(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                        .copy(alpha = 0.5f)
                                )
                            ) { Text(stringResource(Res.string.common_cancel)) }
                        }
                        Spacer(Modifier.height(12.dp))
                        LinearWavyProgressIndicator(
                            progress = { session.progress },
                            amplitude = {
                                if (session.status == BackupSessionStatus.RUNNING) 1f else 0f
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(
                                Res.string.home_session_progress,
                                session.completedFiles,
                                session.totalFiles
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = state.rebuilding && state.activeBackup == null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            AnimatedVisibility(
                visible = state.activeBackup == null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    val foldersSelected = state.backupFoldersSelected
                    val pending = state.localOnlyCount
                    Button(
                        onClick = when {
                            pending > 0 -> onBackUpPending
                            foldersSelected -> onScanNow
                            else -> onChooseFolders
                        },
                        shapes = ButtonDefaults.shapes(),
                        enabled = !scanning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            contentColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            when {
                                scanning -> stringResource(Res.string.home_scanning)
                                pending > 0 -> pluralStringResource(
                                    Res.plurals.home_back_up_pending,
                                    pending,
                                    pending
                                )

                                foldersSelected -> stringResource(Res.string.home_scan_now)
                                else -> stringResource(Res.string.home_choose_folders)
                            }
                        )
                    }
                }
            }
        }
    }
}

/** Only shown when connectivity needs attention; a recovery shows briefly, then the row goes. */
@Composable
private fun rememberConnectionStatus(
    offline: Boolean,
    connection: TelegramConnectionState,
    known: Boolean
): ConnectionStatus? {
    if (!known) return null
    val settled = !offline && connection == TelegramConnectionState.READY
    var wasSettled by rememberSaveable { mutableStateOf(settled) }
    var showRecovered by remember { mutableStateOf(false) }

    LaunchedEffect(settled) {
        if (settled && !wasSettled) {
            showRecovered = true
            delay(RECOVERED_VISIBLE_MS.milliseconds)
            showRecovered = false
        }
        wasSettled = settled
    }

    return when {
        offline -> ConnectionStatus(
            ConnectionIndicator.OFFLINE,
            Res.string.home_status_offline
        )

        connection == TelegramConnectionState.UPDATING -> ConnectionStatus(
            ConnectionIndicator.WORKING,
            Res.string.home_status_syncing
        )

        connection != TelegramConnectionState.READY -> ConnectionStatus(
            ConnectionIndicator.WORKING,
            Res.string.home_status_connecting
        )

        showRecovered -> ConnectionStatus(
            ConnectionIndicator.CONNECTED,
            Res.string.home_status_connected
        )

        else -> null
    }
}

@Composable
private fun StorageCard(slices: List<StorageSlice>, modifier: Modifier = Modifier) {
    CyberStorageGaugeCard(
        slices = slices,
        modifier = modifier
    )
}

@Composable
private fun LegacyStorageCard(slices: List<StorageSlice>, modifier: Modifier = Modifier) {
    val totalBytes = remember(slices) { slices.sumOf { it.totalBytes } }
    val totalFiles = remember(slices) { slices.sumOf { it.fileCount } }

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = 6.dp,
        tintAlpha = 0.65f,
        borderWidth = 1.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Formatters.bytes(totalBytes),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = pluralStringResource(Res.plurals.file_count, totalFiles, totalFiles),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .liquidGlassBackground(
                            shape = CircleShape,
                            tintAlpha = 0.8f,
                            borderAlpha = 0.5f
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (com.novo.drive.presentation.common.isBengaliUi()) "আনলিমিটেড ক্লাউড" else "Unlimited cloud",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            StorageBar(slices = slices, totalBytes = totalBytes)
            Spacer(Modifier.height(16.dp))
            StorageLegend(slices = slices, totalBytes = totalBytes)
        }
    }
}

/** Every slice keeps a sliver, so nothing vanishes entirely. */
@Composable
private fun StorageBar(slices: List<StorageSlice>, totalBytes: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BAR_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(BAR_GAP)
    ) {
        slices.forEach { slice ->
            val fraction = (slice.totalBytes.toFloat() / totalBytes).coerceAtLeast(MIN_SLICE)
            Box(
                modifier = Modifier
                    .weight(fraction)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(slice.category.chartColor())
            )
        }
    }
}

private val LegendTextStyle: TextStyle
    @Composable get() = MaterialTheme.typography.bodyLarge

@Composable
private fun StorageLegend(slices: List<StorageSlice>, totalBytes: Long) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            slices.forEach { slice ->
                Row(
                    modifier = Modifier.padding(vertical = LEGEND_ROW_GAP),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(LEGEND_DOT)
                            .clip(CircleShape)
                            .background(slice.category.chartColor())
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = slice.category.label(),
                        style = LegendTextStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(horizontalAlignment = Alignment.End) {
            slices.forEach { slice ->
                val percent = slice.totalBytes * 100f / totalBytes
                Text(
                    text = if (percent < 1f) {
                        stringResource(Res.string.home_storage_percent_min)
                    } else {
                        stringResource(Res.string.home_storage_percent, percent.roundToInt())
                    },
                    style = LegendTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(vertical = LEGEND_ROW_GAP)
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(horizontalAlignment = Alignment.End) {
            slices.forEach { slice ->
                Text(
                    text = Formatters.bytes(slice.totalBytes),
                    style = LegendTextStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.padding(vertical = LEGEND_ROW_GAP)
                )
            }
        }
    }
}

/**
 * A disabled schedule outranks a stale timestamp, which means nothing while nothing is scheduled.
 */
@Composable
private fun backupFreshnessLabel(
    autoBackupEnabled: Boolean,
    lastBackupAt: Long?,
    totalFiles: Int,
    backedUpCount: Int
): String = when {
    !autoBackupEnabled -> stringResource(Res.string.home_backup_auto_off)
    /* Files indexed from an existing channel are already safe even though this
       device never ran a session, so claiming "no backup yet" would be wrong. */
    lastBackupAt == null && totalFiles > 0 && backedUpCount >= totalFiles ->
        stringResource(Res.string.home_backup_all_in_telegram)

    lastBackupAt == null -> stringResource(Res.string.home_backup_never)
    else -> when (val age = Formatters.relativeAge(lastBackupAt)) {
        AgeBucket.JustNow -> stringResource(Res.string.home_backup_just_now)
        is AgeBucket.Minutes -> stringResource(
            Res.string.home_backup_at,
            pluralStringResource(Res.plurals.backup_age_minutes, age.value, age.value)
        )

        is AgeBucket.Hours -> stringResource(
            Res.string.home_backup_at,
            pluralStringResource(Res.plurals.backup_age_hours, age.value, age.value)
        )

        is AgeBucket.Days -> stringResource(
            Res.string.home_backup_at,
            pluralStringResource(Res.plurals.backup_age_days, age.value, age.value)
        )

        is AgeBucket.Longer -> stringResource(
            Res.string.home_backup_at,
            Formatters.date(age.epochMillis)
        )
    }
}

private val AVATAR_SIZE = 32.dp
private const val RECOVERED_VISIBLE_MS = 2_000L
private val BAR_HEIGHT = 14.dp
private val BAR_GAP = 3.dp
private val LEGEND_DOT = 12.dp
private const val MIN_SLICE = 0.02f
private val LEGEND_ROW_GAP = 6.dp


@Composable
private fun SmartCategoriesSection(
    slices: List<StorageSlice>,
    primaryText: Color,
    secondaryText: Color,
    onOpenCategory: (FileCategory) -> Unit
) {
    val isBn = LocalLocaleList.current.firstOrNull()?.language?.startsWith("bn") == true
    val sliceMap = remember(slices) { slices.associateBy { it.category } }
    Column {
        SectionHeader(stringResource(Res.string.home_section_categories))
        Spacer(Modifier.height(10.dp))
        val categories = listOf(
            Triple(FileCategory.IMAGE, Icons.Filled.PhotoLibrary, Res.string.common_category_photos),
            Triple(FileCategory.VIDEO, Icons.Filled.VideoLibrary, Res.string.common_category_videos),
            Triple(FileCategory.DOCUMENT, Icons.Filled.Description, Res.string.common_category_documents),
            Triple(FileCategory.AUDIO, Icons.Filled.Audiotrack, Res.string.common_category_audio),
            Triple(FileCategory.ARCHIVE, Icons.Filled.Archive, Res.string.common_category_archives),
            Triple(FileCategory.OTHER, Icons.Filled.Android, Res.string.common_category_other)
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (row in categories.chunked(2)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for ((cat, icon, labelRes) in row) {
                        val color = cat.chartColor()
                        val slice = sliceMap[cat]
                        val count = slice?.fileCount ?: 0
                        val sizeBytes = slice?.totalBytes ?: 0L
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .liquidGlassBackground(
                                    shape = RoundedCornerShape(18.dp),
                                    tintAlpha = 0.70f,
                                    borderAlpha = 0.12f,
                                    borderWidth = 1.dp
                                )
                                .clickable(role = Role.Button) { onOpenCategory(cat) }
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(color.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = stringResource(labelRes),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val catCountLabel = if (isBn) "$count টি ফাইল" else "$count files"
                                Text(
                                    text = "$catCountLabel • ${Formatters.bytes(sizeBytes)}",
                                    fontSize = 11.sp,
                                    color = secondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KunjoPowerSuiteDeck(
    isFamilyMember: Boolean,
    primaryText: Color,
    secondaryText: Color,
    onOpenFamilyDashboard: () -> Unit,
    onOpenStorageCleaner: () -> Unit,
    onOpenMusicPlayer: () -> Unit,
    onOpenStreamBridge: () -> Unit,
    onOpenLocalFileManager: () -> Unit,
    onOpenCinema: () -> Unit
) {
    val palette = LocalIplusPalette.current
    val isBn = LocalLocaleList.current.firstOrNull()?.language?.startsWith("bn") == true
    val tools = listOf(
        Triple(
            if (isBn) (if (isFamilyMember) "ফ্যামিলি পাস" else "ফ্যামিলি ক্লাউড") else (if (isFamilyMember) "Family Pass" else "Family Cloud"),
            if (isBn) (if (isFamilyMember) "স্লট ও কোটা তথ্য" else "৩ জন সদস্য • QR ও PIN") else (if (isFamilyMember) "Slot & quota info" else "3 members • QR & PIN"),
            Pair(Icons.Filled.FamilyRestroom, palette.accentSecondary)
        ) to onOpenFamilyDashboard,
        Triple(
            if (isBn) "স্মার্ট ক্লিনার" else "Smart Cleaner",
            if (isBn) "ডুপ্লিকেট ও বড় ফাইল" else "Duplicates & large files",
            Pair(Icons.Filled.CleaningServices, palette.accentPrimary)
        ) to onOpenStorageCleaner,
        Triple(
            if (isBn) "ক্লাউড মিউজিক" else "Cloud Music",
            if (isBn) "ব্যাকগ্রাউন্ড প্লেয়ার" else "Background music player",
            Pair(Icons.Filled.LibraryMusic, palette.folderSongsColor)
        ) to onOpenMusicPlayer,
        Triple(
            if (isBn) "টিভি স্ট্রিম" else "Wi-Fi TV Stream",
            if (isBn) "স্মার্ট টিভি ও WebDAV" else "Smart TV & WebDAV",
            Pair(Icons.Filled.Tv, palette.folderPhotosColor)
        ) to onOpenStreamBridge,
        Triple(
            if (isBn) "ডিভাইস ফাইলস" else "Device Files",
            if (isBn) "মেমোরি ও ফোন ফাইল" else "SD card & internal storage",
            Pair(Icons.Filled.PhoneAndroid, palette.accentPrimary)
        ) to onOpenLocalFileManager,
        Triple(
            if (isBn) "কুঞ্জ সিনেমা" else "Kunjo Cinema",
            if (isBn) "২০০% সাউন্ড ও ডাবল স্পিড" else "200% sound & 2x speed",
            Pair(Icons.Filled.Movie, palette.accentSecondary)
        ) to onOpenCinema
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(if (isBn) "কুঞ্জ কমান্ড ডেক" else "Command deck")
        tools.chunked(2).forEach { rowTools ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowTools.forEach { (info, action) ->
                    val (title, subtitle, iconPair) = info
                    val (icon, tint) = iconPair
                    LiquidGlassCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        onClick = action
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(tint.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = subtitle,
                                fontSize = 11.sp,
                                color = secondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SecureVaultCard(
    onOpenVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalIplusPalette.current
    LiquidGlassCard(
        shape = MaterialTheme.shapes.extraLarge,
        elevation = 6.dp,
        tintAlpha = 0.65f,
        borderWidth = 1.dp,
        glowColor = palette.glowColor,
        modifier = modifier.fillMaxWidth(),
        onClick = onOpenVault
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(palette.accentPrimary, palette.accentSecondary)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color(0xFF090D16),
                    modifier = Modifier.size(24.dp)
                )
            }
            val isBn = LocalLocaleList.current.firstOrNull()?.language?.startsWith("bn") == true
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isBn) "সিকিউর ভল্ট" else "Secure vault",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .liquidGlassBackground(
                                shape = CircleShape,
                                tintAlpha = 0.7f,
                                borderAlpha = 0.4f
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isBn) "PIN সুরক্ষিত" else "PIN protected",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accentSecondary
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (isBn) "গোপন ও ব্যক্তিগত ফাইল পিন দিয়ে সুরক্ষিত রাখুন" else "Keep confidential files protected with a PIN",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FamilyDriveCard(
    onOpenGuide: () -> Unit,
    onOpenChannels: () -> Unit
) {
    val shape = MaterialTheme.shapes.extraLarge
    LiquidGlassCard(
        shape = shape,
        elevation = 6.dp,
        tintAlpha = 0.60f,
        borderWidth = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.People,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val isBn = LocalLocaleList.current.firstOrNull()?.language?.startsWith("bn") == true
                    Text(
                        text = if (isBn) "পারিবারিক ক্লাউড ভল্ট" else "Family cloud vault",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (isBn) "পরিবারের সবার জন্য সুরক্ষিত শেয়ার্ড ক্লাউড ও স্মৃতিসমূহ" else "Secure shared cloud for family files and memories",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidGlassButton(
                    onClick = onOpenGuide,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(Res.string.family_drive_how_it_works))
                }
                LiquidGlassButton(
                    onClick = onOpenChannels,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Filled.FolderShared,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(Res.string.settings_channels))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FamilyDriveGuideDialog(onDismiss: () -> Unit, onOpenChannels: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.People,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(stringResource(Res.string.family_drive_title))
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(Res.string.family_drive_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))

                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(Res.string.family_drive_step1_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.family_drive_step1_desc),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(Res.string.family_drive_step2_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.family_drive_step2_desc),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(Res.string.family_drive_step3_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.family_drive_step3_desc),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(if (com.novo.drive.presentation.common.isBengaliUi()) "বন্ধ করুন" else "Close")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onOpenChannels()
                },
                shapes = ButtonDefaults.shapes()
            ) {
                Text(if (com.novo.drive.presentation.common.isBengaliUi()) "ড্রাইভ চ্যানেল সেটআপ" else "Setup Drive Channels")
            }
        }
    )
}
