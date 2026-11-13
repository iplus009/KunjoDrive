package com.novo.drive.data.mapper

import com.novo.drive.data.local.entity.TransferEntity
import com.novo.drive.domain.model.TransferTask

fun TransferEntity.toDomain(): TransferTask = TransferTask(
    id = id,
    type = type,
    fileId = fileId,
    displayName = displayName,
    sizeBytes = sizeBytes,
    transferredBytes = transferredBytes,
    state = state,
    priority = priority,
    errorMessage = errorMessage,
    speedBytesPerSecond = speedBytesPerSecond,
    stage = stage,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt
)
