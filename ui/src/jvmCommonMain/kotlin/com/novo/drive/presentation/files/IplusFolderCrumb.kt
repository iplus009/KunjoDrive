package com.novo.drive.presentation.files

import com.novo.drive.presentation.common.UiText

/** A null id is the root. */
data class IplusFolderCrumb(
    val id: String?,
    val name: UiText
)

typealias FolderCrumb = IplusFolderCrumb

