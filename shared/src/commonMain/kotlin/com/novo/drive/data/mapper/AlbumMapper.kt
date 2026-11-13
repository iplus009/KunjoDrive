package com.novo.drive.data.mapper

import com.novo.drive.data.local.entity.AlbumSummary
import com.novo.drive.domain.model.MediaAlbum

fun AlbumSummary.toDomain(): MediaAlbum = MediaAlbum(
    folderId = folderId,
    name = name,
    itemCount = itemCount,
    coverFileId = coverFileId,
    latestAt = latestAt
)
