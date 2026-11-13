package com.novo.drive.core.telegram

sealed interface TelegramDownloadEvent {

    data class Progress(val transferredBytes: Long, val totalBytes: Long) : TelegramDownloadEvent

    data class Completed(val localPath: String, val sizeBytes: Long) : TelegramDownloadEvent
}
