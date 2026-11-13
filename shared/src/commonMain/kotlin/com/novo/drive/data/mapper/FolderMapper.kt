package com.novo.drive.data.mapper

import com.novo.drive.data.local.dao.FolderWithCount
import com.novo.drive.data.local.dao.FolderWithStats
import com.novo.drive.data.local.entity.FolderEntity
import com.novo.drive.domain.model.DriveFolder

fun FolderEntity.toDomain(): DriveFolder = DriveFolder(
    id = id,
    parentId = parentId,
    name = name,
    isHidden = isHidden,
    isArchived = isArchived,
    isFavorite = isFavorite,
    isAvailableOffline = isAvailableOffline,
    trashedAt = trashedAt,
    createdAt = createdAt,
    modifiedAt = modifiedAt
)

fun FolderWithCount.toDomain(): DriveFolder = folder.toDomain().copy(
    fileCount = fileCount,
    folderCount = folderCount
)

fun FolderWithStats.toDomain(): DriveFolder = folder.toDomain().copy(
    fileCount = fileCount,
    folderCount = folderCount,
    totalBytes = totalBytes
)
