package com.novo.drive.presentation.home

data class PendingSync(
    val files: Int = 0,
    val folders: Boolean = false
) {
    val visible: Boolean get() = files > 0 || folders
}
