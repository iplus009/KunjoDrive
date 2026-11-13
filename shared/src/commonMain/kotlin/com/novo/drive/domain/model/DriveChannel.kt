package com.novo.drive.domain.model

data class DriveChannel(
    val chatId: Long,
    val title: String,
    val fileCount: Int,
    val remoteFileCount: Int,
    val storedBytes: Long,
    val backupFolders: Set<String>,
    val photoPath: String?,
    val isActive: Boolean,
    val isIndexed: Boolean,
    val lastOpenedAt: Long
) {

    /** The part the user chose, empty for a drive that was never named. */
    val label: String
        get() = title.removePrefix(DRIVE_PREFIX)
            .removePrefix("NovoDrive")
            .removePrefix("নভো ড্রাইভ")
            .trim()

    val displayName: String
        get() = label.ifEmpty { "Novo Drive" }

    companion object {
        const val DRIVE_PREFIX = "Novo Drive"
    }
}
