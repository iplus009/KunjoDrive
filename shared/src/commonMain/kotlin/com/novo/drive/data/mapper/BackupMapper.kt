package com.novo.drive.data.mapper

import com.novo.drive.data.local.entity.BackupSessionEntity
import com.novo.drive.domain.model.BackupSession

fun BackupSessionEntity.toDomain(): BackupSession = BackupSession(
    id = id,
    trigger = trigger,
    status = status,
    totalFiles = totalFiles,
    completedFiles = completedFiles,
    failedFiles = failedFiles,
    skippedFiles = skippedFiles,
    totalBytes = totalBytes,
    transferredBytes = transferredBytes,
    startedAt = startedAt,
    completedAt = completedAt
)
