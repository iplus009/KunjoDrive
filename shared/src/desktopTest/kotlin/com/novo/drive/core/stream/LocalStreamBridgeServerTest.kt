package com.novo.drive.core.stream

import com.novo.drive.data.local.entity.FileEntity
import com.novo.drive.data.repository.FakeSettings
import com.novo.drive.data.repository.SyncHarness
import com.novo.drive.data.repository.TEST_CHAT
import com.novo.drive.domain.model.BackupState
import com.novo.drive.domain.model.FileCategory
import com.novo.drive.domain.model.UserPreferences
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * The LAN Stream Bridge is a hand-written HTTP server; TVs and VLC depend on its Range
 * responses being spec-correct, so these tests pin the wire behaviour.
 */
class LocalStreamBridgeServerTest {

    private val harness = SyncHarness()
    private val payload = ByteArray(32) { (it * 7 + 3).toByte() }
    private val localFile = File.createTempFile("novodrive-stream", ".bin").apply {
        writeBytes(payload)
        deleteOnExit()
    }
    private val settings = FakeSettings(
        UserPreferences(storageChatId = TEST_CHAT, streamBridgePin = "2468")
    )
    private val server = LocalStreamBridgeServer(
        fileDao = harness.database.fileDao(),
        activeChannel = harness.activeChannel,
        telegramClient = harness.telegram,
        settingsRepository = settings,
        wrappedKeyRepository = com.novo.drive.testing.unused()
    )

    @After
    fun tearDown() {
        server.stop()
        harness.close()
        localFile.delete()
    }

    @Test
    fun `catalog lists streamable files and requires the pin`() = runBlocking {
        seedFile("movie1")
        val status = server.start(0)
        assertTrue(status.running)

        val ok = get("/?pin=2468")
        assertEquals(200, ok.code)
        assertTrue(ok.body.contains("movie1.mkv"))

        // Wrong or missing PIN gets the login page (HTTP 200 by design for browsers; WebDAV gets 401),
        // never the file list.
        val noPin = get("/")
        assertEquals(200, noPin.code)
        assertTrue(noPin.body.contains("4-digit PIN"))
        assertTrue(!noPin.body.contains("movie1.mkv"))
        val wrongPin = get("/?pin=0000")
        assertTrue(!wrongPin.body.contains("movie1.mkv"))
    }

    @Test
    fun `range request returns exactly the requested bytes`() = runBlocking {
        seedFile("movie1")
        server.start(0)

        val res = get("/stream/movie1?pin=2468", range = "bytes=4-9")
        assertEquals(206, res.code)
        assertEquals("bytes 4-9/${payload.size}", res.header("Content-Range"))
        assertEquals("6", res.header("Content-Length"))
        assertEquals(payload.sliceArray(4..9).toList(), res.bodyBytes.toList())
    }

    @Test
    fun `suffix range returns the last N bytes`() = runBlocking {
        seedFile("movie1")
        server.start(0)

        val res = get("/stream/movie1?pin=2468", range = "bytes=-8")
        assertEquals(206, res.code)
        assertEquals("bytes 24-31/${payload.size}", res.header("Content-Range"))
        assertEquals(payload.sliceArray(24..31).toList(), res.bodyBytes.toList())
    }

    @Test
    fun `open-ended range streams the tail of the file`() = runBlocking {
        seedFile("movie1")
        server.start(0)

        val res = get("/stream/movie1?pin=2468", range = "bytes=30-")
        assertEquals(206, res.code)
        assertEquals("bytes 30-31/${payload.size}", res.header("Content-Range"))
        assertEquals(payload.sliceArray(30..31).toList(), res.bodyBytes.toList())
    }

    @Test
    fun `hidden trashed foreign and encrypted files are not served`() = runBlocking {
        seedFile("ok")
        seedFile("hiddenOne", isHidden = true)
        seedFile("gone", trashedAt = 5_000)
        seedFile("secret", isEncrypted = true)
        server.start(0)

        assertEquals(404, get("/stream/hiddenOne?pin=2468").code)
        assertEquals(404, get("/stream/gone?pin=2468").code)
        assertEquals(404, get("/stream/secret?pin=2468").code)
        val ok = get("/stream/ok?pin=2468", range = "bytes=0-3")
        assertEquals(206, ok.code)
        assertEquals(payload.sliceArray(0..3).toList(), ok.bodyBytes.toList())
    }

    private suspend fun seedFile(
        id: String,
        isHidden: Boolean = false,
        isEncrypted: Boolean = false,
        trashedAt: Long? = null
    ) {
        harness.database.fileDao().upsert(
            FileEntity(
                id = id,
                folderId = null,
                name = "$id.mkv",
                sizeBytes = payload.size.toLong(),
                mimeType = "video/x-matroska",
                category = FileCategory.VIDEO,
                localPath = localFile.absolutePath,
                contentHash = null,
                chatId = TEST_CHAT,
                messageId = 40,
                remoteFileId = "remote-$id",
                remoteUniqueId = "unique-$id",
                backupState = BackupState.BACKED_UP,
                isHidden = isHidden,
                isEncrypted = isEncrypted,
                trashedAt = trashedAt,
                createdAt = 1_000,
                modifiedAt = 1_000,
                addedAt = 1_000
            )
        )
    }

    private fun get(path: String, range: String? = null): Response {
        val url = URL("http://127.0.0.1:${server.status.value.port}$path")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5_000
        connection.readTimeout = 10_000
        if (range != null) connection.setRequestProperty("Range", range)
        val code = connection.responseCode
        val raw = try {
            connection.inputStream.use { it.readBytes() }
        } catch (_: Throwable) {
            ByteArray(0)
        }
        val headers = connection.headerFields
        connection.disconnect()
        return Response(code, raw, headers)
    }

    private data class Response(
        val code: Int,
        val rawBytes: ByteArray,
        val headers: Map<String, List<String>>
    ) {
        val body: String get() = rawBytes.decodeToString()
        val bodyBytes: List<Byte> get() = rawBytes.toList()

        fun header(name: String): String? =
            headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.firstOrNull()
    }
}
