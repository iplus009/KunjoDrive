package com.novo.drive.data.local.dao

import com.novo.drive.domain.model.FileCategory

data class CategoryUsage(
    val category: FileCategory,
    val fileCount: Int,
    val totalBytes: Long
)
