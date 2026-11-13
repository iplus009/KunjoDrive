package com.novo.drive.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ocr_texts",
    indices = [Index("extractedText")]
)
data class OcrTextEntity(
    @PrimaryKey val fileId: String,
    val extractedText: String,
    val indexedAt: Long
)

