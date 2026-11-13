package com.novo.drive.core.transfer

import android.content.Context
import com.novo.drive.R
import com.novo.drive.core.common.AppNotifications
import com.novo.drive.data.local.dao.BackupDao
import com.novo.drive.data.local.dao.TransferDao
import com.novo.drive.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class NotifyingBackupSessionTracker(
    transferDao: TransferDao,
    backupDao: BackupDao,
    private val settingsRepository: SettingsRepository,
    private val appNotifications: AppNotifications,
    private val context: Context
) : CountingBackupSessionTracker(transferDao, backupDao) {

    override suspend fun onSessionSettled(counts: BackupSessionCounts) {
        if (!settingsRepository.preferences.first().backupNotifications) return
        appNotifications.notifyBackupResult(
            title = context.getString(R.string.notification_backup_complete),
            message = context.getString(
                R.string.notification_backup_summary,
                counts.completedFiles,
                counts.totalFiles
            )
        )
    }
}
