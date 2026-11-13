package com.novo.drive.desktop.permissions

import com.novo.drive.core.permissions.AppPermission
import com.novo.drive.core.permissions.PermissionChecker

class DesktopPermissionChecker : PermissionChecker {

    override fun isGranted(permission: AppPermission): Boolean = true

    override fun hasAllFilesAccess(): Boolean = true

    override fun statuses(): Map<AppPermission, Boolean> =
        AppPermission.entries.associateWith { true }

    override fun missingCritical(): List<AppPermission> = emptyList()

    override fun isRequestable(permission: AppPermission): Boolean = false
}
