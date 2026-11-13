package com.novo.drive.presentation.platform

import androidx.compose.runtime.staticCompositionLocalOf
import com.novo.drive.core.permissions.AppPermission

fun interface PermissionRequester {

    fun request(permissions: List<AppPermission>, onDone: () -> Unit)
}

val LocalPermissionRequester = staticCompositionLocalOf<PermissionRequester> {
    error("PermissionRequester is not provided")
}
