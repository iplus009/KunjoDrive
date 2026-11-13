package com.novo.drive.data.repository

import com.novo.drive.core.common.AppError
import com.novo.drive.core.common.AppResult
import com.novo.drive.core.telegram.TelegramClient
import com.novo.drive.core.telegram.TelegramException
import com.novo.drive.core.transfer.FileParts
import com.novo.drive.data.local.dao.FilePartDao
import com.novo.drive.data.local.entity.FileEntity
import com.novo.drive.data.remote.telegram.ExtraFields
import com.novo.drive.data.remote.telegram.ManifestCodec
import com.novo.drive.data.remote.telegram.RemoteFileManifest

/** Local rows are the fast path; the caption is the durable copy that survives a wipe. */
class FileManifestPublisher(
    private val telegramClient: TelegramClient,
    private val manifestCodec: ManifestCodec,
    private val folderPathResolver: FolderPathResolver,
    private val filePartDao: FilePartDao
) {

    suspend fun publish(entity: FileEntity): AppResult<Unit> {
        val chatId = entity.chatId ?: return AppResult.Success(Unit)
        val messageId = entity.messageId ?: return AppResult.Success(Unit)
        val fileManifest = RemoteFileManifest(
            fileId = entity.id,
            name = entity.name,
            folderPath = folderPathResolver.pathOf(entity.folderId ?: entity.preTrashFolderId),
            folderId = entity.folderId ?: entity.preTrashFolderId,
            mimeType = entity.mimeType,
            sizeBytes = entity.sizeBytes,
            contentHash = entity.contentHash,
            hidden = entity.isHidden,
            archived = entity.isArchived,
            favorite = entity.isFavorite,
            trashedAt = entity.trashedAt,
            encrypted = entity.isEncrypted,
            createdAt = entity.createdAt,
            modifiedAt = entity.modifiedAt,
            width = entity.width,
            height = entity.height,
            durationMs = entity.durationMs,
            iconFileId = entity.iconFileId,
            extra = ExtraFields.fromText(entity.manifestExtra)
        )
        val partCount = maxOf(filePartDao.countOf(entity.id), entity.partCount)
        val manifest = if (partCount > 1) {
            FileParts.asFirstPart(fileManifest, partCount)
        } else {
            fileManifest
        }
        return try {
            telegramClient.editCaption(
                chatId,
                messageId,
                manifestCodec.encode(manifest, entity.isEncrypted)
            )
            AppResult.Success(Unit)
        } catch (e: TelegramException) {
            AppResult.Failure(
                if (e.isRateLimit) AppError.RateLimited(e.retryAfterSeconds ?: 0)
                else AppError.TelegramError(e.code, e.message)
            )
        }
    }

}
