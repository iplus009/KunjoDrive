package com.novo.drive.presentation.gallery

import com.novo.drive.domain.model.FileSortField
import com.novo.drive.domain.model.MediaAlbum
import com.novo.drive.domain.model.SortDirection
import com.novo.drive.domain.model.ViewMode
import com.novo.drive.presentation.components.SelectionCapabilities

data class GalleryUiState(
    val tab: GalleryTab = GalleryTab.ALL,
    val albums: List<MediaAlbum> = emptyList(),
    val albumTitle: String? = null,
    val viewMode: ViewMode = ViewMode.GRID,
    val isAlbumView: Boolean = false,
    val gridSize: Int = 3,
    val albumGridSize: Int = 3,
    val loaded: Boolean = false,
    val capabilities: SelectionCapabilities = SelectionCapabilities(),
    val sortField: FileSortField = FileSortField.DATE_MODIFIED,
    val sortDirection: SortDirection = SortDirection.DESCENDING,
    val selection: Set<String> = emptySet()
) {
    val selectionMode: Boolean get() = selection.isNotEmpty()
    val allSelectedFavorite: Boolean get() = selectionMode && !capabilities.anyUnfavorited
    val allSelectedAvailableOffline: Boolean get() = selectionMode && !capabilities.anyNotAvailableOffline
}
