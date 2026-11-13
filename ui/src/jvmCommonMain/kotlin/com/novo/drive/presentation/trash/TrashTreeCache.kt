package com.novo.drive.presentation.trash

import com.novo.drive.domain.model.TrashItem

internal data class TrashTreeCache(
    val children: Map<String, List<TrashItem>> = emptyMap(),
    val counts: Map<String, Int> = emptyMap()
)
