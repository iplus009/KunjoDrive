package com.novo.drive.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.novo.drive.domain.model.BackupState
import com.novo.drive.domain.model.FileCategory

@Entity(
    tableName = "files",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("folderId"),
        Index("name"),
        Index("category"),
        Index("backupState"),
        Index("trashedAt"),
        Index("remoteUniqueId"),
        Index(value = ["localPath"], unique = false),
        Index(value = ["contentHash"]),
        Index("pendingPublish"),
        Index("versionGroupId"),
        // Composite index for the hottest list queries (Home recent, category counts, albums):
        // every list screen filters by chatId + trashedAt + isHidden + isArchived and orders by addedAt.
        Index(value = ["chatId", "trashedAt", "isHidden", "isArchived", "addedAt"])
    ]
)
data class FileEntity(
    @PrimaryKey val id: String,
    val folderId: String?,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val category: FileCategory,
    val localPath: String?,
    val contentHash: String?,
    val chatId: Long?,
    val messageId: Long?,
    val remoteFileId: String?,
    val remoteUniqueId: String?,
    val backupState: BackupState,
    val isHidden: Boolean = false,
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false,
    val isAvailableOffline: Boolean = false,
    val isEncrypted: Boolean = false,
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Long? = null,
    val trashedAt: Long? = null,
    val preTrashFolderId: String? = null,
    val pendingPublish: Boolean = false,
    val partCount: Int = 0,
    val iconFileId: String? = null,
    val createdAt: Long,
    val modifiedAt: Long,
    val addedAt: Long,
    val manifestExtra: String? = null,
    val remoteBase: String? = null,
    val versionGroupId: String? = null,
    val versionNumber: Int = 1
)
