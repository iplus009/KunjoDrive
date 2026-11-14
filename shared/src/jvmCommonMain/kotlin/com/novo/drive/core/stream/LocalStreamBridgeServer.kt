package com.novo.drive.core.stream

import com.novo.drive.core.crypto.CryptoKeys
import com.novo.drive.core.crypto.WrappedKeyRepository
import com.novo.drive.core.telegram.TelegramClient
import com.novo.drive.data.local.dao.FileDao
import com.novo.drive.data.local.entity.FileEntity
import com.novo.drive.data.repository.ActiveChannel
import com.novo.drive.domain.model.FileQuerySpec
import com.novo.drive.domain.repository.SettingsRepository
import com.novo.drive.data.local.FileQueryBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.Base64

data class StreamBridgeStatus(
    val running: Boolean = false,
    val lanIp: String = "127.0.0.1",
    val port: Int = 8765,
    val pin: String = "",
    val activeConnections: Int = 0
) {
    val browserUrl: String get() = "http://$lanIp:$port/?pin=$pin"
    val webDavUrl: String get() = "http://$lanIp:$port/dav/"
}

/**
 * Zero-dependency LAN HTTP Range + WebDAV Stream Bridge running on both Android & Desktop.
 * Allows Smart TVs, VLC, Kodi, and PC browsers on the same Wi-Fi to browse and stream cloud media
 * protected by a 4-digit session PIN.
 */
class LocalStreamBridgeServer(
    private val fileDao: FileDao,
    private val activeChannel: ActiveChannel,
    private val telegramClient: TelegramClient,
    private val settingsRepository: SettingsRepository,
    private val wrappedKeyRepository: WrappedKeyRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val random = SecureRandom()
    private val failedAttempts = ConcurrentHashMap<String, Pair<Int, Long>>()
    private val activeConnectionCount = AtomicInteger(0)
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null

    private val _status = MutableStateFlow(StreamBridgeStatus())
    val status: StateFlow<StreamBridgeStatus> = _status.asStateFlow()

    suspend fun start(requestedPort: Int = 8765): StreamBridgeStatus {
        if (_status.value.running) return _status.value

        val prefs = settingsRepository.preferences.first()
        val pin = prefs.streamBridgePin.takeIf { it.length == 4 && it.all(Char::isDigit) }
            ?: "%04d".format(random.nextInt(10_000)).also { generated ->
                settingsRepository.update { it.copy(streamBridgePin = generated) }
            }

        val ip = detectLanIpv4()
        val socket = runCatching { ServerSocket(requestedPort) }
            .getOrElse { ServerSocket(0) }
        val actualPort = socket.localPort
        serverSocket = socket

        val active = StreamBridgeStatus(
            running = true,
            lanIp = ip,
            port = actualPort,
            pin = pin
        )
        _status.value = active

        acceptJob = scope.launch {
            while (isActive && !socket.isClosed) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                launch { handleClient(client) }
            }
        }
        return active
    }

    suspend fun regeneratePin(): String {
        val newPin = "%04d".format(random.nextInt(10_000))
        settingsRepository.update { it.copy(streamBridgePin = newPin) }
        _status.value = _status.value.copy(pin = newPin)
        return newPin
    }

    fun stop() {
        acceptJob?.cancel()
        acceptJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
        activeConnectionCount.set(0)
        _status.update { it.copy(running = false, activeConnections = 0) }
    }

    private fun handleClient(socket: Socket) {
        val currentConnections = activeConnectionCount.incrementAndGet()
        _status.update { it.copy(activeConnections = currentConnections) }
        try {
            socket.soTimeout = 30_000
            val input = BufferedInputStream(socket.getInputStream())
            val output = BufferedOutputStream(socket.getOutputStream())

            val requestLine = readAsciiLine(input) ?: return
            val parts = requestLine.split(' ')
            if (parts.size < 2) return
            val method = parts[0].uppercase()
            val rawTarget = parts[1]

            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = readAsciiLine(input) ?: break
                if (line.isEmpty()) break
                val colon = line.indexOf(':')
                if (colon > 0) {
                    headers[line.substring(0, colon).trim().lowercase()] =
                        line.substring(colon + 1).trim()
                }
            }

            val path = rawTarget.substringBefore('?')
            val query = rawTarget.substringAfter('?', "")
            val activePin = _status.value.pin

            val clientIp = socket.inetAddress?.hostAddress ?: "unknown"
            val now = System.currentTimeMillis()
            val lockout = failedAttempts[clientIp]?.second ?: 0L
            if (lockout > now) {
                sendSimple(output, "429 Too Many Requests", "text/plain", "Too many failed attempts. Try again later.")
                return
            }

            if (!isAuthorized(query, headers, activePin)) {
                val currentFails = (failedAttempts[clientIp]?.first ?: 0) + 1
                if (currentFails >= 10) {
                    failedAttempts[clientIp] = Pair(currentFails, now + 5 * 60 * 1000L) // 5 min lockout
                } else {
                    failedAttempts[clientIp] = Pair(currentFails, 0L)
                }
                sendUnauthorized(output, isDav = path.startsWith("/dav"))
                return
            }
            failedAttempts.remove(clientIp)

            val setPinCookie = query.contains("pin=$activePin")

            when {
                method == "OPTIONS" -> sendDavOptions(output)
                method == "PROPFIND" && path.startsWith("/dav") -> {
                    val files = runBlocking { loadStreamableFiles() }
                    sendDavPropfind(output, files)
                }
                path == "/" || path == "/index.html" -> {
                    val files = runBlocking { loadStreamableFiles() }
                    sendHtmlCatalog(output, files, activePin, setPinCookie)
                }
                path.startsWith("/stream/") -> {
                    val fileId = URLDecoder.decode(path.removePrefix("/stream/").substringBefore('/'), "UTF-8")
                    val entity = runBlocking { fileDao.byId(fileId) }
                    val currentChatId = runBlocking { activeChannel.id() }
                    if (entity == null || entity.isHidden || entity.trashedAt != null ||
                        entity.chatId != currentChatId || entity.isEncrypted) {
                        sendSimple(output, "404 Not Found", "text/plain", "File not found")
                    } else {
                        streamEntity(output, method, entity, headers["range"])
                    }
                }
                path.startsWith("/dav/") -> {
                    val fileName = URLDecoder.decode(path.removePrefix("/dav/").trim('/'), "UTF-8")
                    val files = runBlocking { loadStreamableFiles() }
                    val entity = files.firstOrNull { it.name == fileName }
                    if (entity == null) {
                        sendSimple(output, "404 Not Found", "text/plain", "Not found")
                    } else {
                        streamEntity(output, method, entity, headers["range"])
                    }
                }
                else -> sendSimple(output, "404 Not Found", "text/plain", "Not found")
            }
        } catch (_: Throwable) {
        } finally {
            runCatching { socket.close() }
            val remainingConnections = activeConnectionCount.decrementAndGet().coerceAtLeast(0)
            _status.update { it.copy(activeConnections = remainingConnections) }
        }
    }

    private suspend fun loadStreamableFiles(): List<FileEntity> {
        val chatId = activeChannel.id()
        val query = FileQueryBuilder.build(
            FileQuerySpec(
                chatId = chatId,
                filterByFolder = false,
                showHidden = false,
                showArchived = false
            )
        )
        return fileDao.observeList(query).first().filter { !it.isEncrypted }
    }

    private data class ByteRange(
        val start: Long,
        val end: Long,
        val isPartial: Boolean,
        val unsatisfiable: Boolean = false
    )

    private fun streamEntity(
        output: OutputStream,
        method: String,
        entity: FileEntity,
        rangeHeader: String?
    ) {
        val localFile = entity.localPath?.let(::File)?.takeIf { it.exists() && it.isFile }
        val totalSize = localFile?.length() ?: entity.sizeBytes
        if (totalSize <= 0L) {
            sendSimple(output, "404 Not Found", "text/plain", "Empty file")
            return
        }

        val range = parseRange(rangeHeader, totalSize)
        if (range.unsatisfiable) {
            val resp = "HTTP/1.1 416 Range Not Satisfiable\r\n" +
                    "Content-Range: bytes */$totalSize\r\n" +
                    "Content-Length: 0\r\nConnection: close\r\n\r\n"
            output.write(resp.encodeToByteArray())
            output.flush()
            return
        }
        val start = range.start
        val end = range.end
        val isPartial = range.isPartial
        val contentLength = (end - start + 1).coerceAtLeast(0L)
        val statusLine = if (isPartial) "206 Partial Content" else "200 OK"
        val safeMime = entity.mimeType.ifBlank { "application/octet-stream" }

        val headerText = buildString {
            append("HTTP/1.1 $statusLine\r\n")
            append("Content-Type: $safeMime\r\n")
            append("Accept-Ranges: bytes\r\n")
            append("Content-Length: $contentLength\r\n")
            if (isPartial) {
                append("Content-Range: bytes $start-$end/$totalSize\r\n")
            }
            // Let media players / browsers cache the manifest but revalidate
            append("ETag: \"${entity.id.hashCode().toUInt().toString(16)}-${totalSize}\"\r\n")
            append("Last-Modified: ${java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(java.time.Instant.ofEpochMilli(localFile?.lastModified() ?: 0L).atZone(java.time.ZoneOffset.UTC))}\r\n")
            append("Cache-Control: public, max-age=86400\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Connection: close\r\n\r\n")
        }
        output.write(headerText.encodeToByteArray())
        output.flush()
        if (method == "HEAD" || contentLength == 0L) return

        if (localFile != null) {
            RandomAccessFile(localFile, "r").use { raf ->
                raf.seek(start)
                val buf = ByteArray(64 * 1024)
                var remaining = contentLength
                while (remaining > 0) {
                    val toRead = minOf(buf.size.toLong(), remaining).toInt()
                    val read = raf.read(buf, 0, toRead)
                    if (read <= 0) break
                    output.write(buf, 0, read)
                    remaining -= read
                }
            }
            output.flush()
            return
        }

        val remoteFileId = entity.remoteFileId ?: return
        runBlocking {
            val resolved = telegramClient.resolveFile(remoteFileId)
            var cursor = start
            val chunkSize = 256L * 1024L
            var lastRequestedOffset = start
            telegramClient.requestFileRange(resolved.fileId, start, contentLength)
            while (cursor <= end) {
                val want = minOf(chunkSize, end - cursor + 1)
                var attempts = 0
                var chunk: ByteArray? = null
                while (attempts < 200) {
                    val info = telegramClient.getFileInfo(resolved.fileId)
                    val availableEnd = info.downloadOffset + info.downloadedPrefixSize
                    val availableBytes = (availableEnd - cursor).coerceAtLeast(0L)
                    if (info.isDownloadingCompleted || (info.downloadOffset <= cursor && availableBytes > 0L)) {
                        val readCount = if (info.isDownloadingCompleted) want else minOf(want, availableBytes)
                        chunk = runCatching {
                            telegramClient.readFilePart(resolved.fileId, cursor, readCount)
                        }.getOrNull()
                        if (chunk != null && chunk.isNotEmpty()) break
                    } else if (!info.isDownloadingActive || cursor < info.downloadOffset || cursor > availableEnd + chunkSize) {
                        if (lastRequestedOffset != cursor) {
                            lastRequestedOffset = cursor
                            telegramClient.requestFileRange(resolved.fileId, cursor, end - cursor + 1)
                        }
                    }
                    delay(150)
                    attempts++
                }
                val bytes = chunk ?: break
                output.write(bytes)
                cursor += bytes.size
            }
            output.flush()
        }
    }

    private fun parseRange(header: String?, total: Long): ByteRange {
        if (header.isNullOrBlank() || !header.startsWith("bytes=")) {
            return ByteRange(0L, total - 1, false)
        }
        val spec = header.removePrefix("bytes=").substringBefore(',')
        val dash = spec.indexOf('-')
        if (dash < 0) return ByteRange(0L, total - 1, false)
        val startPart = spec.substring(0, dash).trim()
        val endPart = spec.substring(dash + 1).trim()
        return if (startPart.isEmpty()) {
            // Suffix range "bytes=-N": the last N bytes of the file.
            val suffix = endPart.toLongOrNull() ?: return ByteRange(0L, total - 1, false, unsatisfiable = true)
            if (suffix <= 0L) return ByteRange(0L, total - 1, false, unsatisfiable = true)
            val start = (total - suffix).coerceAtLeast(0L)
            ByteRange(start, total - 1, true)
        } else {
            val rawStart = startPart.toLongOrNull() ?: return ByteRange(0L, total - 1, false, unsatisfiable = true)
            if (rawStart < 0L || rawStart >= total) {
                return ByteRange(0L, total - 1, false, unsatisfiable = true)
            }
            val end = if (endPart.isEmpty()) {
                total - 1
            } else {
                val rawEnd = endPart.toLongOrNull() ?: return ByteRange(0L, total - 1, false, unsatisfiable = true)
                if (rawEnd < rawStart) return ByteRange(0L, total - 1, false, unsatisfiable = true)
                rawEnd.coerceAtMost(total - 1)
            }
            ByteRange(rawStart, end, true)
        }
    }

    private fun isAuthorized(
        query: String,
        headers: Map<String, String>,
        pin: String
    ): Boolean {
        if (pin.isBlank()) return false
        val pinBytes = pin.encodeToByteArray()
        val queryPin = query.split('&').firstOrNull { it.startsWith("pin=") }?.substringAfter("pin=")
        if (queryPin != null && MessageDigest.isEqual(queryPin.encodeToByteArray(), pinBytes)) return true
        val cookie = headers["cookie"].orEmpty()
        val cookiePin = cookie.split(';')
            .map { it.trim() }
            .firstOrNull { it.startsWith("kunjo_pin=") || it.startsWith("novo_pin=") }
            ?.substringAfter('=')
        if (cookiePin != null && MessageDigest.isEqual(cookiePin.encodeToByteArray(), pinBytes)) return true
        val auth = headers["authorization"].orEmpty()
        if (auth.startsWith("Basic ", ignoreCase = true)) {
            val decoded = runCatching {
                Base64.getDecoder().decode(auth.substring(6).trim()).decodeToString()
            }.getOrDefault("")
            val pass = decoded.substringAfter(':', "")
            if (pass.isNotEmpty() && MessageDigest.isEqual(pass.encodeToByteArray(), pinBytes)) return true
            if (decoded.isNotEmpty() && MessageDigest.isEqual(decoded.encodeToByteArray(), pinBytes)) return true
        }
        return false
    }

    private fun sendUnauthorized(output: OutputStream, isDav: Boolean) {
        if (isDav) {
            val resp = "HTTP/1.1 401 Unauthorized\r\n" +
                    "WWW-Authenticate: Basic realm=\"Kunjo Drive Stream Bridge (Use 4-digit PIN as password)\"\r\n" +
                    "Content-Length: 0\r\nConnection: close\r\n\r\n"
            output.write(resp.encodeToByteArray())
            output.flush()
            return
        }
        val html = """
            <!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
            <title>Kunjo Drive • Wi-Fi Stream Bridge</title>
            <style>body{background:#060c18;color:#eaf4ff;font-family:system-ui,sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;margin:0}
            .card{background:rgba(16,28,48,.85);border:1px solid rgba(0,229,255,.35);border-radius:20px;padding:32px;max-width:360px;text-align:center}
            input{background:#091426;border:1px solid #00e5ff;color:#fff;font-size:22px;letter-spacing:8px;text-align:center;padding:12px;border-radius:12px;width:180px;margin:16px 0}
            button{background:linear-gradient(135deg,#00e5ff,#2979ff);border:none;color:#04101f;font-weight:700;padding:12px 28px;border-radius:12px;cursor:pointer;font-size:15px}</style>
            </head><body><div class="card"><h2>Kunjo Drive Stream</h2><p>Enter the 4-digit PIN shown on your Kunjo Drive screen</p>
            <form method="GET" action="/"><input name="pin" maxlength="4" placeholder="••••" autofocus/><br/><button type="submit">Unlock Stream</button></form></div></body></html>
        """.trimIndent()
        sendSimple(output, "200 OK", "text/html; charset=utf-8", html)
    }

    private fun sendHtmlCatalog(
        output: OutputStream,
        files: List<FileEntity>,
        pin: String,
        setCookie: Boolean
    ) {
        val rows = files.joinToString("\n") { f ->
            val encName = URLEncoder.encode(f.name, "UTF-8")
            val url = "/stream/${f.id}/$encName?pin=$pin"
            val sizeMb = "%.1f MB".format(f.sizeBytes / (1024.0 * 1024.0))
            val tag = when {
                f.mimeType.startsWith("video/") -> "🎬 Video"
                f.mimeType.startsWith("audio/") -> "🎵 Audio"
                f.mimeType.startsWith("image/") -> "🖼️ Image"
                else -> "📄 File"
            }
            """<a class="row" href="$url" target="_blank"><div><strong>${escapeHtml(f.name)}</strong><div class="sub">$tag • $sizeMb</div></div><span class="play">Stream ▶</span></a>"""
        }
        val html = """
            <!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
            <title>Kunjo Drive • Smart TV & PC Stream Bridge</title>
            <style>body{background:#060c18;color:#eaf4ff;font-family:system-ui,sans-serif;max-width:880px;margin:0 auto;padding:24px}
            h1{color:#00e5ff;margin-bottom:4px}.meta{color:#8aa4c8;margin-bottom:24px;font-size:14px}
            .row{display:flex;justify-content:space-between;align-items:center;background:rgba(16,28,48,.82);border:1px solid rgba(0,229,255,.18);padding:14px 18px;border-radius:14px;margin-bottom:10px;color:#eaf4ff;text-decoration:none}
            .row:hover{border-color:#00e5ff}.sub{color:#8aa4c8;font-size:12px;margin-top:4px}.play{color:#00e5ff;font-weight:700;font-size:14px}</style>
            </head><body><h1>Kunjo Drive Stream Bridge</h1>
            <div class="meta">${files.size} streamable cloud & local files • HTTP Range & WebDAV ready</div>
            ${rows.ifBlank { "<p>No unencrypted files in active drive yet.</p>" }}
            </body></html>
        """.trimIndent()
        val bytes = html.encodeToByteArray()
        val cookieHeader = if (setCookie) "Set-Cookie: novo_pin=$pin; Path=/; HttpOnly\r\n" else ""
        val header = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n" +
                cookieHeader +
                "Content-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
        output.write(header.encodeToByteArray())
        output.write(bytes)
        output.flush()
    }

    private fun sendDavOptions(output: OutputStream) {
        val resp = "HTTP/1.1 200 OK\r\n" +
                "DAV: 1\r\n" +
                "Allow: OPTIONS, GET, HEAD, PROPFIND\r\n" +
                "Content-Length: 0\r\nConnection: close\r\n\r\n"
        output.write(resp.encodeToByteArray())
        output.flush()
    }

    private fun sendDavPropfind(output: OutputStream, files: List<FileEntity>) {
        val itemsXml = files.joinToString("") { f ->
            val href = "/dav/" + URLEncoder.encode(f.name, "UTF-8").replace("+", "%20")
            """
            <D:response>
              <D:href>$href</D:href>
              <D:propstat>
                <D:prop>
                  <D:displayname>${escapeHtml(f.name)}</D:displayname>
                  <D:getcontentlength>${f.sizeBytes}</D:getcontentlength>
                  <D:getcontenttype>${escapeHtml(f.mimeType)}</D:getcontenttype>
                  <D:resourcetype/>
                </D:prop>
                <D:status>HTTP/1.1 200 OK</D:status>
              </D:propstat>
            </D:response>
            """.trimIndent()
        }
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <D:multistatus xmlns:D="DAV:">
              <D:response>
                <D:href>/dav/</D:href>
                <D:propstat>
                  <D:prop><D:displayname>Kunjo Drive</D:displayname><D:resourcetype><D:collection/></D:resourcetype></D:prop>
                  <D:status>HTTP/1.1 200 OK</D:status>
                </D:propstat>
              </D:response>
              $itemsXml
            </D:multistatus>
        """.trimIndent()
        sendSimple(output, "207 Multi-Status", "application/xml; charset=utf-8", xml)
    }

    private fun sendSimple(output: OutputStream, status: String, contentType: String, body: String) {
        val bytes = body.encodeToByteArray()
        val header = "HTTP/1.1 $status\r\nContent-Type: $contentType\r\n" +
                "Content-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
        output.write(header.encodeToByteArray())
        output.write(bytes)
        output.flush()
    }

    private fun readAsciiLine(input: BufferedInputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val b = input.read()
            if (b == -1) return if (sb.isEmpty()) null else sb.toString()
            if (b == '\n'.code) break
            if (b != '\r'.code) sb.append(b.toChar())
            if (sb.length > 8192) break
        }
        return sb.toString()
    }

    private fun escapeHtml(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun detectLanIpv4(): String = runCatching {
        val interfaces = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
        for (nif in interfaces) {
            if (!nif.isUp || nif.isLoopback) continue
            for (addr in nif.inetAddresses.toList()) {
                if (addr is Inet4Address && !addr.isLoopbackAddress && addr.isSiteLocalAddress) {
                    return@runCatching addr.hostAddress ?: "127.0.0.1"
                }
            }
        }
        "127.0.0.1"
    }.getOrDefault("127.0.0.1")
}

