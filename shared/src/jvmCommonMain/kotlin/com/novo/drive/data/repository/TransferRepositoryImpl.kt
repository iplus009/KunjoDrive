package com.novo.drive.data.repository

import com.novo.drive.core.common.AppError
import com.novo.drive.core.common.AppResult
import com.novo.drive.core.common.SafeLog
import com.novo.drive.core.files.StorageInspector
import com.novo.drive.core.telegram.TelegramClient
import com.novo.drive.core.telegram.TelegramException
import com.novo.drive.core.telegram.TelegramLimits
import com.novo.drive.core.transfer.BackupSessionTracker
import com.novo.drive.core.transfer.FileParts
import com.novo.drive.core.transfer.TransferScheduler
import com.novo.drive.data.local.dao.FileDao
import com.novo.drive.data.local.dao.TransferDao
import com.novo.drive.data.local.entity.TransferEntity
import com.novo.drive.data.mapper.toDomain
import com.novo.drive.domain.model.BackupState
import com.novo.drive.domain.model.TransferSection
import com.novo.drive.domain.model.TransferState
import com.novo.drive.domain.model.TransferTask
import com.novo.drive.domain.model.TransferType
import com.novo.drive.domain.repository.SettingsRepository
import com.novo.drive.domain.repository.TransferRepository
import com.novo.drive.domain.usecase.ValidateUploadUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID

class TransferRepositoryImpl(
    private val transferDao: TransferDao,
    private val fileDao: FileDao,
    private val telegramClient: TelegramClient,
    private val transferScheduler: TransferScheduler,
    private val settingsRepository: SettingsRepository,
    private val validateUpload: ValidateUploadUseCase,
    private val storageInspector: StorageInspector,
    private val backupSessionTracker: BackupSessionTracker
) : TransferRepository {

    private val enqueueLock = Mutex()

    override fun observeSection(
        section: TransferSection,
        limit: Int
    ): Flow<List<TransferTask>> = when (section) {
        TransferSection.ACTIVE -> transferDao.observeActive(limit)
        TransferSection.PAUSED -> transferDao.observePaused(limit)
        TransferSection.FAILED -> transferDao.observeFailed(limit)
        TransferSection.COMPLETED -> transferDao.observeCompleted(limit)
    }.map { list -> list.map { it.toDomain() } }

    override fun observeSectionCount(section: TransferSection): Flow<Int> = when (section) {
        TransferSection.ACTIVE -> transferDao.observeActiveCount()
        TransferSection.PAUSED -> transferDao.observeCountByState(TransferState.PAUSED)
        TransferSection.FAILED -> transferDao.observeCountByState(TransferState.FAILED)
        TransferSection.COMPLETED -> transferDao.observeCountByState(TransferState.COMPLETED)
    }

    override fun observeActiveCount(): Flow<Int> = transferDao.observeActiveCount()

    override fun observeActiveForFile(fileId: String): Flow<TransferTask?> =
        transferDao.observeActiveForFile(fileId).map { it?.toDomain() }

    override fun observeActiveDownloads(): Flow<List<TransferTask>> =
        transferDao.observeActiveDownloads().map { list -> list.map { it.toDomain() } }

    override suspend fun enqueueUpload(fileId: String, priority: Int): AppResult<String> =
        enqueue(fileId, TransferType.UPLOAD, priority)

    /**
     * Orphans whose bytes exist are queued again; ones with nothing on disk or in Telegram are
     * dropped.
     */
    override suspend fun enqueuePendingUploads(): AppResult<Int> {
        val prefs = settingsRepository.preferences.first()
        if (prefs.kunjoAccountType.equals("SUB", ignoreCase = true) || !prefs.kunjoCanUpload) {
            return AppResult.Success(0)
        }
        val chatId = prefs.storageChatId
        val pending = fileDao.localOnlyFileIds(chatId)
        val orphaned = fileDao.queuedWithoutTransferFileIds(chatId)
        val requeue = mutableListOf<String>()
        if (orphaned.isNotEmpty()) {
            val phantoms = mutableListOf<String>()
            fileDao.byIds(orphaned).forEach { entity ->
                val exists = entity.localPath?.let { File(it).exists() } == true
                when {
                    exists -> requeue.add(entity.id)
                    entity.remoteFileId == null -> phantoms.add(entity.id)
                    else -> fileDao.setBackupState(entity.id, BackupState.NONE)
                }
            }
            if (requeue.isNotEmpty()) {
                fileDao.setBackupStates(requeue, BackupState.NONE)
            }
            if (phantoms.isNotEmpty()) {
                SafeLog.w(TAG, "Dropping ${phantoms.size} queued records with no bytes left")
                fileDao.deleteByIds(phantoms)
            }
        }
        return AppResult.Success(
            enqueueBatch(pending + requeue, TransferType.UPLOAD, sessionId = null)
        )
    }

    override suspend fun enqueueDownload(fileId: String, priority: Int): AppResult<String> {
        val entity = fileDao.byId(fileId) ?: return AppResult.Failure(AppError.NotFound)
        if (entity.remoteFileId == null) {
            return AppResult.Failure(AppError.NoRemoteCopy)
        }
        val available = storageInspector.availableBytes()
        if (available < entity.sizeBytes + STORAGE_MARGIN) {
            return AppResult.Failure(
                AppError.InsufficientStorage(entity.sizeBytes + STORAGE_MARGIN, available)
            )
        }
        return enqueueLock.withLock {
            transferDao.unfinishedIdsForFiles(listOf(fileId)).firstOrNull()
                ?.let { return@withLock AppResult.Success(it) }
            insertTransfer(
                entity.id,
                entity.name,
                entity.sizeBytes,
                entity.chatId,
                TransferType.DOWNLOAD,
                priority,
                null
            )
        }
    }

    suspend fun enqueueBackup(
        fileId: String,
        sessionId: String,
        priority: Int = 0
    ): AppResult<String> = enqueue(fileId, TransferType.BACKUP, priority, sessionId)

    override suspend fun enqueueBackupBatch(
        fileIds: List<String>,
        sessionId: String
    ): AppResult<Int> = AppResult.Success(
        enqueueBatch(fileIds, TransferType.BACKUP, sessionId)
    )

    private suspend fun enqueueBatch(
        fileIds: List<String>,
        type: TransferType,
        sessionId: String?
    ): Int {
        if (fileIds.isEmpty()) return 0

        val limits = currentLimits()
        val prefs = settingsRepository.preferences.first()
        if (type != TransferType.DOWNLOAD &&
            (prefs.kunjoAccountType.equals("SUB", ignoreCase = true) || !prefs.kunjoCanUpload)
        ) {
            return 0
        }
        val availableBytes = storageInspector.availableBytes()
        var queued = 0

        for (chunk in fileIds.chunked(SQL_BATCH)) enqueueLock.withLock {
            val alreadyQueued = transferDao.unfinishedFileIds(chunk).toSet()
            if (sessionId != null && alreadyQueued.isNotEmpty()) {
                transferDao.adoptIntoSession(alreadyQueued.toList(), sessionId)
            }
            val now = System.currentTimeMillis()
            val transfers = mutableListOf<TransferEntity>()
            for (fileId in chunk) {
                if (fileId in alreadyQueued) continue
                val entity = fileDao.byId(fileId) ?: continue
                if (entity.localPath == null) continue
                val scratch = if (prefs.encryptFiles) {
                    minOf(entity.sizeBytes, FileParts.PART_SIZE)
                } else {
                    0
                }
                val rejected = validateUpload(
                    fileSizeBytes = entity.sizeBytes,
                    limits = limits,
                    availableLocalBytes = availableBytes,
                    requiredScratchBytes = scratch,
                    splitsIfTooLarge = true
                )
                if (rejected != null) continue
                fileDao.setBackupStateIfLocalOnly(entity.id, BackupState.QUEUED)
                transfers += TransferEntity(
                    id = UUID.randomUUID().toString(),
                    type = type,
                    fileId = entity.id,
                    displayName = entity.name,
                    localPath = null,
                    chatId = entity.chatId,
                    messageId = null,
                    remoteFileId = null,
                    sizeBytes = entity.sizeBytes,
                    state = TransferState.QUEUED,
                    priority = 0,
                    backupSessionId = sessionId,
                    createdAt = now,
                    updatedAt = now
                )
            }
            if (transfers.isNotEmpty()) {
                transferDao.upsertAll(transfers)
                queued += transfers.size
            }
        }
        if (queued > 0) kickWorker()
        return queued
    }

    private suspend fun enqueue(
        fileId: String,
        type: TransferType,
        priority: Int,
        sessionId: String? = null
    ): AppResult<String> {
        val entity = fileDao.byId(fileId) ?: return AppResult.Failure(AppError.NotFound)
        val prefs = settingsRepository.preferences.first()
        if (type != TransferType.DOWNLOAD &&
            (prefs.kunjoAccountType.equals("SUB", ignoreCase = true) || !prefs.kunjoCanUpload)
        ) {
            return AppResult.Failure(
                AppError.UnsupportedOperation("সাব-অ্যাকাউন্টে আপলোড অনুমতি নেই (Read-Only ও স্ট্রিমিং মোড সক্রিয়)")
            )
        }
        if (entity.localPath == null) {
            return AppResult.Failure(AppError.NoLocalCopy)
        }
        transferDao.unfinishedIdsForFiles(listOf(fileId)).firstOrNull()
            ?.let { return AppResult.Success(it) }


        val limits = currentLimits()
        val scratch = if (prefs.encryptFiles) {
            minOf(entity.sizeBytes, FileParts.PART_SIZE)
        } else {
            0
        }
        validateUpload(
            fileSizeBytes = entity.sizeBytes,
            limits = limits,
            availableLocalBytes = storageInspector.availableBytes(),
            requiredScratchBytes = scratch,
            splitsIfTooLarge = true
        )?.let { return AppResult.Failure(it) }

        return enqueueLock.withLock {
            transferDao.unfinishedIdsForFiles(listOf(fileId)).firstOrNull()
                ?.let { return@withLock AppResult.Success(it) }
            fileDao.setBackupStateIfLocalOnly(entity.id, BackupState.QUEUED)
            insertTransfer(
                entity.id,
                entity.name,
                entity.sizeBytes,
                entity.chatId,
                type,
                priority,
                sessionId
            )
        }
    }

    private suspend fun insertTransfer(
        fileId: String,
        name: String,
        sizeBytes: Long,
        chatId: Long?,
        type: TransferType,
        priority: Int,
        sessionId: String?
    ): AppResult<String> {
        val now = System.currentTimeMillis()
        val transfer = TransferEntity(
            id = UUID.randomUUID().toString(),
            type = type,
            fileId = fileId,
            displayName = name,
            localPath = null,
            chatId = chatId,
            messageId = null,
            remoteFileId = null,
            sizeBytes = sizeBytes,
            state = TransferState.QUEUED,
            priority = priority,
            backupSessionId = sessionId,
            createdAt = now,
            updatedAt = now
        )
        transferDao.upsert(transfer)
        kickWorker()
        return AppResult.Success(transfer.id)
    }

    override suspend fun pause(id: String) {
        val transfer = transferDao.byId(id) ?: return
        if (transfer.state == TransferState.QUEUED || transfer.state == TransferState.RUNNING) {
            transferDao.setState(id, TransferState.PAUSED, System.currentTimeMillis())
            backupSessionTracker.refresh(transfer.backupSessionId)
        }
    }

    override suspend fun resume(id: String) {
        val transfer = transferDao.byId(id) ?: return
        if (transfer.state == TransferState.PAUSED || transfer.state == TransferState.FAILED) {
            transferDao.setState(id, TransferState.QUEUED, System.currentTimeMillis())
            backupSessionTracker.refresh(transfer.backupSessionId)
            kickWorker()
        }
    }

    override suspend fun cancel(id: String) {
        val transfer = transferDao.byId(id) ?: return
        if (transfer.state.isTerminal) {
            transferDao.deleteByIds(listOf(id))
        } else {
            transferDao.setState(id, TransferState.CANCELLED, System.currentTimeMillis())
            transfer.fileId?.let { fileDao.setBackupStateIfLocalOnly(it, BackupState.NONE) }
        }
        syncFileStates()
        backupSessionTracker.refresh(transfer.backupSessionId)
    }

    override suspend fun cancelForFiles(fileIds: List<String>) {
        if (fileIds.isEmpty()) return
        fileIds.chunked(SQL_BATCH)
            .flatMap { transferDao.unfinishedIdsForFiles(it) }
            .forEach { cancel(it) }
    }

    override suspend fun pauseAll() {
        transferDao.pauseAll(System.currentTimeMillis())
        backupSessionTracker.refreshActive()
    }

    override suspend fun resumeAll() {
        transferDao.resumeAll(System.currentTimeMillis())
        backupSessionTracker.refreshActive()
        kickWorker()
    }

    override suspend fun cancelAll() {
        transferDao.cancelAll(System.currentTimeMillis())
        syncFileStates()
        backupSessionTracker.refreshActive()
    }

    override suspend fun retry(id: String) {
        val transfer = transferDao.byId(id) ?: return
        if (transfer.state == TransferState.FAILED || transfer.state == TransferState.CANCELLED) {
            transferDao.upsert(
                transfer.copy(
                    state = TransferState.QUEUED,
                    transferredBytes = 0,
                    errorMessage = null,
                    updatedAt = System.currentTimeMillis()
                )
            )
            kickWorker()
        }
    }

    override suspend fun clearFinished() {
        transferDao.clearFinished()
        syncFileStates()
    }

    override suspend fun recoverOrphanedTransfers() {
        transferDao.requeueRunning()
        syncFileStates()
        kickWorker()
    }

    /** A backup state only means something while a transfer backs the file up. */
    private suspend fun syncFileStates() {
        fileDao.repairBackedUpStates()
        fileDao.clearStaleQueuedStates()
        fileDao.clearStaleFailedStates()
        fileDao.deleteCancelledBackupEntries()
    }

    private suspend fun kickWorker() {
        val prefs = settingsRepository.preferences.first()
        transferScheduler.kick(prefs.allowMeteredTransfers)
    }

    private suspend fun currentLimits(): TelegramLimits = try {
        telegramClient.getLimits()
    } catch (_: TelegramException) {
        TelegramLimits.REGULAR
    }

    companion object {
        private const val TAG = "TransferRepository"
        private const val STORAGE_MARGIN = 100L * 1024 * 1024
        private const val SQL_BATCH = 500
    }
}
