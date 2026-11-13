package com.novo.drive.presentation.components

sealed interface FolderListResult {
    data class Success(val items: List<LocalFolderItem>) : FolderListResult
    data object Unreadable : FolderListResult
}
