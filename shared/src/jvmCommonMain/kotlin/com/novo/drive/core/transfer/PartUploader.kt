package com.novo.drive.core.transfer

import com.novo.drive.core.common.SafeLog
import com.novo.drive.core.crypto.CryptoKeys
import com.novo.drive.core.crypto.StreamCrypto
import com.novo.drive.core.crypto.WrappedKeyRepository
import com.novo.drive.core.dispatchers.DispatcherProvider
import com.novo.drive.core.files.AppStoragePaths
import com.novo.drive.core.media.ThumbnailStore
import com.novo.drive.core.telegram.TelegramClient
import com.novo.drive.core.telegram.TelegramUploadEvent
import com.novo.drive.data.local.dao.FilePartDao
import com.novo.drive.data.local.entity.FileEntity
import com.novo.drive.data.local.entity.FilePartEntity
import com.novo.drive.data.remote.telegram.ManifestCodec
import com.novo.drive.data.remote.telegram.RemoteFileManifest
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.Channels
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * IDM/Aria2-style multi-connection concurrent part uploader.
 * Uploads parts in parallel (up to MAX_PARALLEL_UPLOADS) using direct RandomAccessFile channel
 * positioning for zero-copy DMA I/O without sequential disk read penalties.
 */
class PartUploader(
    private val storagePaths: AppStoragePaths,
    private val telegramClient: TelegramClient,
    private val filePartDao: FilePartDao,
    private val manifestCodec: ManifestCodec,
    private val streamCrypto: StreamCrypto,
    private val wrappedKeyRepository: WrappedKeyRepository,
    private val thumbnailStore: ThumbnailStore,
    private val dispatchers: DispatcherProvider,
    private val apkIconUploader: ApkIconUploader
) {

    fun upload(
        entity: FileEntity,
        source: File,
        chatId: Long,
        manifest: RemoteFileManifest,
        encrypt: Boolean,
        thumbnailPath: String? = null
    ): Flow<PartUploadEvent> = channelFlow {
        val totalSize = source.length()
        val partCount = FileParts.countFor(totalSize)
        val done = filePartDao.partsOf(entity.id)
            .filter { it.messageId != null }
            .associateBy { it.partIndex }
            .toMutableMap()

        val doneLock = Mutex()

        // Track per-part progress concurrently across parallel streams
        val progressMap = ConcurrentHashMap<Int, Long>()
        for (i in 0 until partCount) {
            if (done.containsKey(i)) {
                progressMap[i] = FileParts.sizeOf(i, totalSize)
            } else {
                progressMap[i] = 0L
            }
        }

        fun sendAggregateProgress() {
            val totalTransferred = progressMap.values.sum()
            trySend(PartUploadEvent.Progress(totalTransferred))
        }

        sendAggregateProgress()

        val remainingIndices = (0 until partCount).filter { !done.containsKey(it) }

        if (remainingIndices.isNotEmpty()) {
            val concurrencyLimit = if (remainingIndices.size <= 1) 1 else MAX_PARALLEL_UPLOADS
            val semaphore = Semaphore(concurrencyLimit)

            coroutineScope {
                val jobs = remainingIndices.mapIndexed { workerIdx, index ->
                    async(dispatchers.io) {
                        // Stagger connection startup by 250ms to prevent Telegram FLOOD_WAIT bursts
                        if (workerIdx > 0) {
                            delay((workerIdx * STAGGER_INTERVAL_MS).coerceAtMost(MAX_STAGGER_DELAY_MS))
                        }

                        semaphore.withPermit {
                            val plainOffset = FileParts.offsetOf(index)
                            val plainSize = FileParts.sizeOf(index, totalSize)
                            val scratch = File(scratchDir(), "${entity.id}.${index}.part")

                            try {
                                if (encrypt) trySend(PartUploadEvent.Sealing(index))
                                writePart(source, plainOffset, plainSize, scratch, encrypt)
                                if (encrypt) trySend(PartUploadEvent.PartDone(index, partCount))

                                val iconFileId = if (index == 0) {
                                    apkIconUploader.uploadIconIfApk(entity, chatId, encrypt)
                                } else null

                                val partManifest = manifest.copy(
                                    version = RemoteFileManifest.PART_VERSION,
                                    partCount = partCount,
                                    partIndex = index,
                                    partOffset = plainOffset,
                                    partSize = plainSize,
                                    iconFileId = if (index == 0) iconFileId ?: manifest.iconFileId else null
                                )
                                val partName = if (encrypt) {
                                    FileParts.nameFor(entity.id, index)
                                } else {
                                    FileParts.nameFor(entity.name, index)
                                }

                                var stored: FilePartEntity? = null
                                telegramClient.uploadDocument(
                                    chatId = chatId,
                                    localPath = scratch.absolutePath,
                                    fileName = partName,
                                    mimeType = if (encrypt) OCTET_STREAM else entity.mimeType,
                                    caption = manifestCodec.encode(partManifest, encrypt),
                                    thumbnailPath = if (index == 0) thumbnailPath else null
                                ).collect { event ->
                                    when (event) {
                                        is TelegramUploadEvent.Started -> Unit
                                        is TelegramUploadEvent.Progress -> {
                                            progressMap[index] = event.transferredBytes.coerceAtMost(plainSize)
                                            sendAggregateProgress()
                                        }

                                        is TelegramUploadEvent.Completed -> {
                                            val document = event.document
                                            stored = FilePartEntity(
                                                fileId = entity.id,
                                                partIndex = index,
                                                chatId = document.chatId,
                                                messageId = document.messageId,
                                                remoteFileId = document.remoteFileId,
                                                remoteUniqueId = document.uniqueFileId,
                                                plainOffset = plainOffset,
                                                plainSize = plainSize,
                                                storedSize = scratch.length(),
                                                uploadedAt = System.currentTimeMillis()
                                            )
                                        }
                                    }
                                }

                                val part = stored ?: error("Part ${index + 1} did not finish")
                                doneLock.withLock {
                                    filePartDao.upsert(part)
                                    done[index] = part
                                }
                                progressMap[index] = plainSize
                                sendAggregateProgress()
                                trySend(PartUploadEvent.PartDone(index, partCount))
                            } finally {
                                withContext(NonCancellable + dispatchers.io) { scratch.delete() }
                            }
                        }
                    }
                }
                jobs.awaitAll()
            }
        }

        val completedParts = done.values.sortedBy { it.partIndex }
        trySend(PartUploadEvent.Completed(completedParts, manifest.contentHash))
    }

    suspend fun discardParts(fileId: String) {
        val parts = filePartDao.partsOf(fileId)
        for ((chatId, group) in parts.groupBy { it.chatId }) {
            if (chatId == null) continue
            val messageIds = group.mapNotNull { it.messageId }
            if (messageIds.isEmpty()) continue
            runCatching { telegramClient.deleteMessages(chatId, messageIds) }
                .onFailure { SafeLog.w(TAG, "Could not drop ${messageIds.size} orphan parts", it) }
        }
        filePartDao.deleteFor(listOf(fileId))
        // Sweep any residual scratch files on cancellation
        withContext(NonCancellable + dispatchers.io) {
            scratchDir().listFiles { _, name -> name.startsWith("$fileId.") }?.forEach { it.delete() }
        }
    }

    /**
     * Uses RandomAccessFile and FileChannel direct seek for O(1) instantaneous offset seeking.
     * Replaces previous O(N) sequential skipping that caused catastrophic I/O bottlenecks.
     */
    private fun writePart(
        source: File,
        plainOffset: Long,
        plainSize: Long,
        target: File,
        encrypt: Boolean
    ) {
        target.parentFile?.mkdirs()
        RandomAccessFile(source, "r").use { raf ->
            val channel = raf.channel
            channel.position(plainOffset)
            val inStream = Channels.newInputStream(channel)
            val ranged = RangeInputStream(inStream, plainSize)
            target.outputStream().buffered().use { output ->
                if (encrypt) {
                    val key = wrappedKeyRepository.getOrCreate(CryptoKeys.CONTENT)
                    streamCrypto.encryptStream(key, ranged, output)
                } else {
                    ranged.copyTo(output)
                }
            }
        }
    }

    private fun scratchDir(): File = File(storagePaths.cacheDir, SCRATCH_DIR).apply { mkdirs() }

    private companion object {
        const val TAG = "PartUploader"
        const val SCRATCH_DIR = "parts"
        const val OCTET_STREAM = "application/octet-stream"
        const val MAX_PARALLEL_UPLOADS = 3
        const val STAGGER_INTERVAL_MS = 250L
        const val MAX_STAGGER_DELAY_MS = 1_000L
    }
}
