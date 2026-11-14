package com.novo.drive.presentation.preview

import com.novo.drive.domain.model.FileCategory
import com.novo.drive.domain.model.FileSortField
import com.novo.drive.domain.model.SortDirection
import com.novo.drive.presentation.navigation.Route

/** So paging sideways walks the same files in the same order, not a freshly guessed query. */
data class PreviewSequence(
    val folderId: String? = null,
    val filterByFolder: Boolean = false,
    val nameQuery: String? = null,
    val categories: List<FileCategory> = emptyList(),
    val favoritesOnly: Boolean = false,
    val availableOfflineOnly: Boolean = false,
    val hiddenOnly: Boolean = false,
    val secondVaultOnly: Boolean = false,
    val archivedOnly: Boolean = false,
    val sortField: FileSortField = FileSortField.NAME,
    val sortDirection: SortDirection = SortDirection.ASCENDING
) {

    fun routeFor(fileId: String): Route.Preview = Route.Preview(
        fileId = fileId,
        folderId = folderId,
        filterByFolder = filterByFolder,
        nameQuery = nameQuery,
        categories = categories.takeIf { it.isNotEmpty() }
            ?.joinToString(CATEGORY_SEPARATOR) { it.name },
        favoritesOnly = favoritesOnly,
        availableOfflineOnly = availableOfflineOnly,
        hiddenOnly = hiddenOnly,
        secondVaultOnly = secondVaultOnly,
        archivedOnly = archivedOnly,
        sortField = sortField.name,
        sortDescending = sortDirection == SortDirection.DESCENDING
    )
}

const val CATEGORY_SEPARATOR = ","
