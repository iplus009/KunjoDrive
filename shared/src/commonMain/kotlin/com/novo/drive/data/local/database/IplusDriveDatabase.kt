package com.novo.drive.data.local.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import com.novo.drive.data.local.dao.BackupDao
import com.novo.drive.data.local.dao.CacheDao
import com.novo.drive.data.local.dao.ExclusionDao
import com.novo.drive.data.local.dao.FamilyDao
import com.novo.drive.data.local.dao.FileDao
import com.novo.drive.data.local.dao.FilePartDao
import com.novo.drive.data.local.dao.FolderDao
import com.novo.drive.data.local.dao.FolderTombstoneDao
import com.novo.drive.data.local.dao.IplusMessageDao
import com.novo.drive.data.local.dao.OcrTextDao
import com.novo.drive.data.local.dao.PendingDeleteDao
import com.novo.drive.data.local.dao.ProxyDao
import com.novo.drive.data.local.dao.StorageChannelDao
import com.novo.drive.data.local.dao.ThumbnailDao
import com.novo.drive.data.local.dao.TransferDao
import com.novo.drive.data.local.entity.BackupRecordEntity
import com.novo.drive.data.local.entity.BackupSessionEntity
import com.novo.drive.data.local.entity.CacheEntryEntity
import com.novo.drive.data.local.entity.ExclusionEntity
import com.novo.drive.data.local.entity.FamilyMemberEntity
import com.novo.drive.data.local.entity.FileEntity
import com.novo.drive.data.local.entity.FilePartEntity
import com.novo.drive.data.local.entity.FolderEntity
import com.novo.drive.data.local.entity.FolderTombstoneEntity
import com.novo.drive.data.local.entity.IplusMessageContactEntity
import com.novo.drive.data.local.entity.IplusMessageEntity
import com.novo.drive.data.local.entity.IplusMessageGroupEntity
import com.novo.drive.data.local.entity.OcrTextEntity
import com.novo.drive.data.local.entity.PendingDeleteEntity
import com.novo.drive.data.local.entity.ProxyEntity
import com.novo.drive.data.local.entity.StorageChannelEntity
import com.novo.drive.data.local.entity.ThumbnailEntity
import com.novo.drive.data.local.entity.TransferEntity

@Database(
    entities = [
        FileEntity::class,
        FolderEntity::class,
        TransferEntity::class,
        BackupSessionEntity::class,
        BackupRecordEntity::class,
        ExclusionEntity::class,
        ThumbnailEntity::class,
        CacheEntryEntity::class,
        StorageChannelEntity::class,
        PendingDeleteEntity::class,
        FilePartEntity::class,
        ProxyEntity::class,
        FolderTombstoneEntity::class,
        FamilyMemberEntity::class,
        OcrTextEntity::class,
        IplusMessageContactEntity::class,
        IplusMessageEntity::class,
        IplusMessageGroupEntity::class
    ],
    version = 21,
    exportSchema = true
)
@ConstructedBy(IplusDriveDatabaseConstructor::class)
abstract class IplusDriveDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao
    abstract fun folderDao(): FolderDao
    abstract fun transferDao(): TransferDao
    abstract fun backupDao(): BackupDao
    abstract fun exclusionDao(): ExclusionDao
    abstract fun thumbnailDao(): ThumbnailDao
    abstract fun cacheDao(): CacheDao
    abstract fun storageChannelDao(): StorageChannelDao
    abstract fun pendingDeleteDao(): PendingDeleteDao
    abstract fun filePartDao(): FilePartDao
    abstract fun proxyDao(): ProxyDao
    abstract fun folderTombstoneDao(): FolderTombstoneDao
    abstract fun familyDao(): FamilyDao
    abstract fun ocrTextDao(): OcrTextDao
    abstract fun iplusMessageDao(): IplusMessageDao

    companion object {
        const val NAME = "novodrive.db"
    }
}

