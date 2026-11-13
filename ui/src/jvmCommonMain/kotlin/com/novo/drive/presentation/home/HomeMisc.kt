package com.novo.drive.presentation.home

import com.novo.drive.core.network.NetworkStatus
import com.novo.drive.core.permissions.AppPermission
import com.novo.drive.core.telegram.TelegramConnectionState
import com.novo.drive.domain.model.BackupHold
import com.novo.drive.domain.model.BackupSession
import com.novo.drive.domain.model.DriveChannel

internal data class HomeMisc(
    val syncing: Boolean,
    val session: BackupSession?,
    val connection: TelegramConnectionState,
    val network: NetworkStatus,
    val missing: List<AppPermission>,
    val activeDrive: DriveChannel?,
    val autoBackupEnabled: Boolean,
    val appLockEnabled: Boolean,
    val showArchivedSection: Boolean,
    val showHiddenSection: Boolean,
    val showRecentSection: Boolean,
    val backupHold: BackupHold?,
    val isFamilyMember: Boolean,
    val familyQuotaBytes: Long,
    val encryptFiles: Boolean
)
