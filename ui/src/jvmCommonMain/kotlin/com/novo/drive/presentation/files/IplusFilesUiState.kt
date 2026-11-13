package com.novo.drive.presentation.files

import com.novo.drive.domain.model.DriveFolder
import com.novo.drive.domain.model.FileSortField
import com.novo.drive.domain.model.SortDirection
import com.novo.drive.domain.model.ViewMode
import com.novo.drive.presentation.common.UiText
import com.novo.drive.presentation.components.SelectionCapabilities

data class IplusFilesUiState(
    val folderId: String? = null,
    val folderName: UiText = UiText.Plain(""),
    val breadcrumbs: List<IplusFolderCrumb> = emptyList(),
    val folders: List<DriveFolder> = emptyList(),
    val selection: Set<String> = emptySet(),
    val folderSelection: Set<String> = emptySet(),
    val viewMode: ViewMode = ViewMode.GRID,
    val gridSize: Int = 3,
    val sortField: FileSortField = FileSortField.NAME,
    val sortDirection: SortDirection = SortDirection.ASCENDING,
    val showHidden: Boolean = false,
    val loaded: Boolean = false,
    val capabilities: SelectionCapabilities = SelectionCapabilities(),
    val selectedQuickFilter: IplusQuickFileFilter = IplusQuickFileFilter.ALL
) {
    val selectionMode: Boolean get() = selection.isNotEmpty() || folderSelection.isNotEmpty()
    val selectionCount: Int get() = selection.size + folderSelection.size
    val folderInSelection: Boolean get() = folderSelection.isNotEmpty()

    val allSelectedAvailableOffline: Boolean
        get() = selectionMode &&
                !capabilities.anyNotAvailableOffline &&
                folders.filter { it.id in folderSelection }.all { it.isAvailableOffline }

    val allSelectedFavorite: Boolean
        get() = selectionMode &&
                !capabilities.anyUnfavorited &&
                folders.filter { it.id in folderSelection }.all { it.isFavorite }
}

typealias FilesUiState = IplusFilesUiState

