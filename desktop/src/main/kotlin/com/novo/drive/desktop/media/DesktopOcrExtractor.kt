package com.novo.drive.desktop.media

import com.novo.drive.core.ocr.OcrExtractor
import java.io.File

class DesktopOcrExtractor : OcrExtractor {
    override suspend fun extractText(localPath: String): String? {
        val file = File(localPath)
        if (!file.exists() || !file.isFile) return null
        // Lightweight offline metadata text scan for embedded ASCII captions/tEXt chunks on Desktop
        return runCatching {
            val maxRead = minOf(file.length(), 128L * 1024L).toInt()
            val bytes = ByteArray(maxRead)
            file.inputStream().use { it.read(bytes) }
            val text = buildString {
                var run = StringBuilder()
                for (b in bytes) {
                    val c = b.toInt() and 0xFF
                    if (c in 32..126) {
                        run.append(c.toChar())
                    } else {
                        if (run.length >= 6) {
                            append(run).append(' ')
                        }
                        run = StringBuilder()
                    }
                }
            }
            text.take(2048).trim().ifBlank { null }
        }.getOrNull()
    }
}

