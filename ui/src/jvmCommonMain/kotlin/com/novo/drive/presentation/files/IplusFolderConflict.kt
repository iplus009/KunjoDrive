package com.novo.drive.presentation.files

import com.novo.drive.core.files.ImportSource

data class IplusFolderConflict(
    val names: List<String>,
    val renamed: Map<String, String>,
    val sources: List<ImportSource>,
    val target: String?
)

typealias FolderConflict = IplusFolderConflict

