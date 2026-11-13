package com.novo.drive.core.ocr

import com.novo.drive.data.local.entity.FileEntity
import com.novo.drive.data.repository.FakeSettings
import com.novo.drive.data.repository.SyncHarness
import com.novo.drive.data.repository.TEST_CHAT
import com.novo.drive.domain.model.BackupState
import com.novo.drive.domain.model.FileCategory
import com.novo.drive.domain.model.UserPreferences
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** OCR indexing must cache results, respect the on/off setting, and only touch local images. */
class OcrIndexServiceTest {

    private val harness = SyncHarness()
    private val imageFile = File.createTempFile("novodrive-ocr", ".jpg").apply {
        writeBytes(byteArrayOf(1, 2, 3))
        deleteOnExit()
    }
    private val extractor = object : OcrExtractor {
        var calls = 0
        override suspend fun extractText(localPath: String): String? {
            calls++
            return if (localPath == imageFile.absolutePath) "  invoice #42  " else null
        }
    }

    @After
    fun tearDown() {
        harness.close()
        imageFile.delete()
    }

    @Test
    fun `single file indexes once and caches the trimmed text`() = runBlocking {
        seedImage("img1")
        val service = service(ocrIndexingEnabled = true)

        assertEquals("invoice #42", service.indexSingleFile("img1"))
        assertEquals("invoice #42", service.indexSingleFile("img1"))
        assertEquals(1, extractor.calls)
        assertEquals("invoice #42", service.observeTextForFile("img1").first())
    }

    @Test
    fun `non-image or missing-local files are skipped`() = runBlocking {
        seedImage("img1", category = FileCategory.VIDEO)
        seedImage("img2", localPath = null)
        val service = service(ocrIndexingEnabled = true)

        assertNull(service.indexSingleFile("img1"))
        assertNull(service.indexSingleFile("img2"))
        assertNull(service.indexSingleFile("missing"))
        assertEquals(0, extractor.calls)
    }

    @Test
    fun `pending batch indexes only images without ocr rows and honours the setting`() = runBlocking {
        seedImage("img1")
        seedImage("img2")
        seedImage("video1", category = FileCategory.VIDEO)

        val off = service(ocrIndexingEnabled = false)
        assertEquals(0, off.indexPendingBatch())

        val on = service(ocrIndexingEnabled = true)
        assertEquals(2, on.indexPendingBatch())
        // second batch finds nothing left
        assertEquals(0, on.indexPendingBatch())
        assertEquals(2, extractor.calls)
    }

    private fun service(ocrIndexingEnabled: Boolean) = OcrIndexService(
        fileDao = harness.database.fileDao(),
        ocrTextDao = harness.database.ocrTextDao(),
        activeChannel = harness.activeChannel,
        settingsRepository = FakeSettings(
            UserPreferences(storageChatId = TEST_CHAT, ocrIndexingEnabled = ocrIndexingEnabled)
        ),
        ocrExtractor = extractor
    )

    private suspend fun seedImage(
        id: String,
        category: FileCategory = FileCategory.IMAGE,
        localPath: String? = imageFile.absolutePath
    ) {
        harness.database.fileDao().upsert(
            FileEntity(
                id = id,
                folderId = null,
                name = "$id.jpg",
                sizeBytes = 3,
                mimeType = "image/jpeg",
                category = category,
                localPath = localPath,
                contentHash = null,
                chatId = TEST_CHAT,
                messageId = 40,
                remoteFileId = "remote-$id",
                remoteUniqueId = "unique-$id",
                backupState = BackupState.BACKED_UP,
                createdAt = 1_000,
                modifiedAt = 1_000,
                addedAt = 1_000
            )
        )
    }
}
