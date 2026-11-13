package com.novo.drive.core.ocr

interface OcrExtractor {
    /** Extracts visible text from an image file completely offline on-device. */
    suspend fun extractText(localPath: String): String?
}

