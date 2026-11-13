package com.novo.drive.core.transfer

import com.novo.drive.core.crypto.CryptoKeys
import com.novo.drive.core.crypto.KeyUnavailableException
import com.novo.drive.core.crypto.StreamCrypto
import com.novo.drive.core.crypto.WrappedKeyRepository
import com.novo.drive.core.dispatchers.DispatcherProvider
import com.novo.drive.core.files.AppStoragePaths
import com.novo.drive.core.telegram.TelegramClient
import com.novo.drive.core.telegram.TelegramDownloadEvent
import com.novo.drive.data.local.dao.FilePartDao
import com.novo.drive.data.local.entity.FilePartEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.Channels
import java.util.concurrent.ConcurrentHashMap

/**
 * IDM/Aria2-style multi-connection concurrent part downloader.
 * Downloads parts in parallel (up to MAX_PARALLEL_DOWNLOADS) and writes directly
 * to respective byte offsets using FileChannel for zero-copy DMA performance.
 */
class PartDownloader(
    private val storagePaths: AppStoragePaths,
    private val telegramClient: TelegramClient,
    private val filePartDao: FilePartDao,
    private val streamCrypto: StreamCrypto,
    private val wrappedKeyRepository: WrappedKeyRepository,
    private val dispatchers: DispatcherProvider
) {

    fun download(fileId: String, encrypted: Boolean): Flow<PartDownloadEvent> = channelFlow {
        val parts = filePartDao.partsOf(fileId).sortedBy { it.partIndex }
        if (parts.isEmpty()) error("File has no parts on record")

        val target = File(assemblyDir(), fileId)
        val stateFile = File(assemblyDir(), "$fileId.parts_done")

        val totalExpectedSize = parts.maxOf { it.plainOffset + it.plainSize }

        // Read already completed parts for seamless resume
        val completedIndices = withContext(dispatchers.io) {
            readCompletedParts(stateFile, target, totalExpectedSize)
        }

        val remainingParts = parts.filter { it.partIndex !in completedIndices }

        val writeLock = Mutex()
        val stateLock = Mutex()

        // Track per-part transferred bytes for accurate aggregate progress
        val progressMap = ConcurrentHashMap<Int, Long>()
        for (part in parts) {
            if (part.partIndex in completedIndices) {
                progressMap[part.partIndex] = part.plainSize
            } else {
                progressMap[part.partIndex] = 0L
            }
        }

        fun sendAggregateProgress() {
            val totalTransferred = progressMap.values.sum()
            trySend(PartDownloadEvent.Progress(totalTransferred))
        }

        sendAggregateProgress()

        if (remainingParts.isNotEmpty()) {
            val concurrencyLimit = if (remainingParts.size <= 1) 1 else MAX_PARALLEL_DOWNLOADS
            val semaphore = Semaphore(concurrencyLimit)

            coroutineScope {
                val jobs = remainingParts.map { part ->
                    async(dispatchers.io) {
                        semaphore.withPermit {
                            val remoteFileId = part.remoteFileId
                                ?: error("Part ${part.partIndex + 1} is missing remoteFileId")
                            var downloadedPath: String? = null

                            telegramClient.downloadDocument(remoteFileId).collect { event ->
                                when (event) {
                                    is TelegramDownloadEvent.Progress -> {
                                        progressMap[part.partIndex] =
                                            event.transferredBytes.coerceAtMost(part.plainSize)
                                        sendAggregateProgress()
                                    }

                                    is TelegramDownloadEvent.Completed -> {
                                        downloadedPath = event.localPath
                                    }
                                }
                            }

                            val sourceFile = downloadedPath?.let(::File)?.takeIf { it.exists() }
                                ?: error("Part ${part.partIndex + 1} did not download")

                            trySend(PartDownloadEvent.Joining(part.partIndex))

                            // Direct offset write via FileChannel
                            writeLock.withLock {
                                RandomAccessFile(target, "rw").use { raf ->
                                    val channel = raf.channel
                                    channel.position(part.plainOffset)
                                    if (encrypted) {
                                        val key = wrappedKeyRepository.get(CryptoKeys.CONTENT)
                                            ?: throw KeyUnavailableException(CryptoKeys.CONTENT)
                                        val outStream = Channels.newOutputStream(channel)
                                        sourceFile.inputStream().buffered().use { input ->
                                            streamCrypto.decryptStream(key, input, outStream)
                                        }
                                    } else {
                                        sourceFile.inputStream().channel.use { inChannel ->
                                            inChannel.transferTo(0, inChannel.size(), channel)
                                        }
                                    }
                                }
                            }

                            // Record part as completed
                            stateLock.withLock {
                                stateFile.appendText("${part.partIndex}\n")
                            }

                            progressMap[part.partIndex] = part.plainSize
                            sendAggregateProgress()
                        }
                    }
                }
                jobs.awaitAll()
            }
        }

        // Cleanup state file upon 100% completion
        withContext(dispatchers.io) {
            stateFile.delete()
        }

        trySend(PartDownloadEvent.Completed(target.absolutePath))
    }

    suspend fun discardAssembly(fileId: String) {
        withContext(dispatchers.io) {
            File(assemblyDir(), fileId).delete()
            File(assemblyDir(), "$fileId.parts_done").delete()
        }
    }

    private fun readCompletedParts(stateFile: File, target: File, totalExpectedSize: Long): Set<Int> {
        if (!target.exists() || !stateFile.exists()) {
            if (!target.exists()) stateFile.delete()
            return emptySet()
        }
        return runCatching {
            stateFile.readLines()
                .mapNotNull { it.trim().toIntOrNull() }
                .toSet()
        }.getOrDefault(emptySet())
    }

    private fun assemblyDir(): File = File(storagePaths.cacheDir, ASSEMBLY_DIR).apply { mkdirs() }

    private companion object {
        const val ASSEMBLY_DIR = "assembly"
        const val MAX_PARALLEL_DOWNLOADS = 4
    }
}
