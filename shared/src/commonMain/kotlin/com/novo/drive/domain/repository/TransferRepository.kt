package com.novo.drive.domain.repository

import com.novo.drive.core.common.AppResult
import com.novo.drive.domain.model.TransferTask
import com.novo.drive.domain.model.TransferSection
import kotlinx.coroutines.flow.Flow

interface TransferRepository {

    /** Bounded, because a backup can queue tens of thousands of transfers. */
    fun observeSection(section: TransferSection, limit: Int): Flow<List<TransferTask>>

    fun observeSectionCount(section: TransferSection): Flow<Int>

    fun observeActiveCount(): Flow<Int>

    fun observeActiveForFile(fileId: String): Flow<TransferTask?>

    fun observeActiveDownloads(): Flow<List<TransferTask>>

    suspend fun enqueueUpload(fileId: String, priority: Int = 0): AppResult<String>

    suspend fun enqueuePendingUploads(): AppResult<Int>

    /**
     * Checks are answered once per run and rows written in batches, so a large scan avoids a round
     * trip per file.
     */
    suspend fun enqueueBackupBatch(fileIds: List<String>, sessionId: String): AppResult<Int>

    suspend fun enqueueDownload(fileId: String, priority: Int = 0): AppResult<String>

    suspend fun pause(id: String)

    suspend fun resume(id: String)

    suspend fun cancel(id: String)

    suspend fun cancelForFiles(fileIds: List<String>)

    suspend fun retry(id: String)

    suspend fun pauseAll()

    suspend fun resumeAll()

    suspend fun cancelAll()

    suspend fun clearFinished()

    suspend fun recoverOrphanedTransfers()
}
