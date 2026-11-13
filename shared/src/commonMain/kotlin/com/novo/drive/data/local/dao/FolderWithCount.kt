package com.novo.drive.data.local.dao

import androidx.room.Embedded
import com.novo.drive.data.local.entity.FolderEntity

data class FolderWithCount(
    @Embedded val folder: FolderEntity,
    val fileCount: Int,
    val folderCount: Int
)
