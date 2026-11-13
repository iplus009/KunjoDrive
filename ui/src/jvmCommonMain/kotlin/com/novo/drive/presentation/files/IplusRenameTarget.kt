package com.novo.drive.presentation.files

data class IplusRenameTarget(
    val id: String,
    val name: String,
    val isFolder: Boolean
)

typealias RenameTarget = IplusRenameTarget

