package com.novo.drive.core.ocr

import com.novo.drive.data.local.dao.FileDao
import com.novo.drive.data.local.dao.OcrTextDao
import com.novo.drive.data.local.entity.OcrTextEntity
import com.novo.drive.data.repository.ActiveChannel
import com.novo.drive.domain.model.FileCategory
import com.novo.drive.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class OcrIndexService(
    private val fileDao: FileDao,
    private val ocrTextDao: OcrTextDao,
    private val activeChannel: ActiveChannel,
    private val settingsRepository: SettingsRepository,
    private val ocrExtractor: OcrExtractor
) {

    private val mutex = Mutex()

    fun observeIndexedCount(): Flow<Int> = ocrTextDao.observeIndexedCount()

    fun observeTextForFile(fileId: String): Flow<String?> =
        ocrTextDao.observeByFileId(fileId).map { it?.extractedText?.takeIf(String::isNotBlank) }

    suspend fun indexSingleFile(fileId: String): String? {
        val entity = fileDao.byId(fileId) ?: return null
        if (entity.category != FileCategory.IMAGE) return null
        val path = entity.localPath ?: return null
        if (!File(path).exists()) return null

        val existing = ocrTextDao.byFileId(fileId)
        if (existing != null) return existing.extractedText.takeIf { it.isNotBlank() }

        val extracted = runCatching { ocrExtractor.extractText(path) }.getOrNull()
            ?.trim()
            .orEmpty()
        ocrTextDao.upsert(
            OcrTextEntity(
                fileId = fileId,
                extractedText = extracted,
                indexedAt = System.currentTimeMillis()
            )
        )
        return extracted.takeIf { it.isNotBlank() }
    }

    suspend fun indexPendingBatch(batchSize: Int = 25): Int = mutex.withLock {
        val prefs = settingsRepository.preferences.first()
        if (!prefs.ocrIndexingEnabled) return@withLock 0

        val chatId = activeChannel.id()
        val pending = fileDao.filesMissingOcr(chatId, batchSize)
        var indexed = 0
        for (file in pending) {
            val path = file.localPath ?: continue
            if (!File(path).exists()) continue
            val text = runCatching { ocrExtractor.extractText(path) }.getOrNull()?.trim().orEmpty()
            ocrTextDao.upsert(
                OcrTextEntity(
                    fileId = file.id,
                    extractedText = text,
                    indexedAt = System.currentTimeMillis()
                )
            )
            if (text.isNotBlank()) indexed++
        }
        indexed
    }
}

