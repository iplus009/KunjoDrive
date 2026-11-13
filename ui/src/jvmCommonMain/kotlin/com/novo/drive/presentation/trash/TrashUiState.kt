package com.novo.drive.presentation.trash

import com.novo.drive.domain.model.TrashItem
import com.novo.drive.presentation.common.UiText

data class TrashUiState(
    val items: List<TrashItem> = emptyList(),
    val rows: List<TrashRow> = emptyList(),
    val selection: Set<String> = emptySet(),
    val autoClearDays: Int = 30,
    val working: UiText? = null,
    val loading: Boolean = true
) {
    val selectionMode: Boolean get() = selection.isNotEmpty()
}
