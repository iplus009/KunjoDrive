package com.novo.drive.presentation.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File

object IplusProtectedZipBundler {

    /**
     * Packages [localFilePaths] into a ZIP archive with optional AES-256 encryption protected by [password].
     * Compression level: 0 = Store (fastest), 1 = Normal, 2 = Maximum.
     * Returns the created archive [File] on success, or null if no valid local files exist.
     */
    suspend fun createZip(
        outputDir: File,
        archiveBaseName: String,
        localFilePaths: List<String>,
        password: String = "",
        encryptAes256: Boolean = true,
        compressionLevel: Int = 1
    ): File? = withContext(Dispatchers.IO) {
        val sources = localFilePaths.map(::File).filter { it.exists() && it.isFile }
        if (sources.isEmpty()) return@withContext null

        if (encryptAes256 && password.isBlank()) return@withContext null

        outputDir.mkdirs()
        val safeBase = archiveBaseName
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim()
            .ifBlank { "iPlusBundle" }
        val target = File(outputDir, "$safeBase-protected.zip")
        if (target.exists()) target.delete()

        val baseParams = ZipParameters().apply {
            when (compressionLevel) {
                0 -> {
                    compressionMethod = CompressionMethod.STORE
                }
                2 -> {
                    compressionMethod = CompressionMethod.DEFLATE
                    this.compressionLevel = CompressionLevel.MAXIMUM
                }
                else -> {
                    compressionMethod = CompressionMethod.DEFLATE
                    this.compressionLevel = CompressionLevel.NORMAL
                }
            }

            if (encryptAes256) {
                isEncryptFiles = true
                encryptionMethod = EncryptionMethod.AES
                aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
            }
        }

        val zipFile = if (encryptAes256) {
            ZipFile(target, password.toCharArray())
        } else {
            ZipFile(target)
        }

        val usedEntryNames = mutableSetOf<String>()
        try {
            zipFile.use { zip ->
                for (src in sources) {
                    val baseName = src.name
                    var entryName = baseName
                    if (!usedEntryNames.add(entryName)) {
                        val dot = baseName.lastIndexOf('.')
                        val stem = if (dot > 0) baseName.substring(0, dot) else baseName
                        val ext = if (dot > 0) baseName.substring(dot) else ""
                        var counter = 2
                        while (!usedEntryNames.add("${stem}_$counter$ext")) {
                            counter++
                        }
                        entryName = "${stem}_$counter$ext"
                    }
                    val entryParams = ZipParameters(baseParams).apply {
                        fileNameInZip = entryName
                    }
                    zip.addFile(src, entryParams)
                }
            }
        } catch (t: Throwable) {
            target.delete()
            throw t
        }
        target.takeIf { it.exists() && it.length() > 0L }
    }

    /**
     * Backward-compatible helper for AES-256 encrypted archive creation.
     */
    suspend fun createAes256Zip(
        outputDir: File,
        archiveBaseName: String,
        localFilePaths: List<String>,
        password: String
    ): File? = createZip(
        outputDir = outputDir,
        archiveBaseName = archiveBaseName,
        localFilePaths = localFilePaths,
        password = password,
        encryptAes256 = true,
        compressionLevel = 1
    )
}

typealias ProtectedZipBundler = IplusProtectedZipBundler

