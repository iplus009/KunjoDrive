package com.novo.drive.domain.model

data class StorageSlice(
    val category: FileCategory,
    val fileCount: Int,
    val totalBytes: Long
)
