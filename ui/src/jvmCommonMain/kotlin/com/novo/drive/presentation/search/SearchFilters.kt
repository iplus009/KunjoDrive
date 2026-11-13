package com.novo.drive.presentation.search

import com.novo.drive.domain.model.FileCategory
import com.novo.drive.domain.model.FileSortField
import com.novo.drive.domain.model.SortDirection

data class SearchFilters(
    val category: FileCategory? = null,
    val backedUpOnly: Boolean = false,
    val notBackedUpOnly: Boolean = false,
    val minSizeMb: Int? = null,
    val sortField: FileSortField = FileSortField.DATE_MODIFIED,
    val sortDirection: SortDirection = SortDirection.DESCENDING
)
