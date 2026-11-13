package com.novo.drive.domain.usecase

data class ExistingBackupRecord(
    val sizeBytes: Long,
    val modifiedAt: Long,
    val contentHash: String?
)
