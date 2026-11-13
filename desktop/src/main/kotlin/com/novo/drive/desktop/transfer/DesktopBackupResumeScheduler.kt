package com.novo.drive.desktop.transfer

import com.novo.drive.core.transfer.BackupResumeScheduler

class DesktopBackupResumeScheduler : BackupResumeScheduler {
    override fun resumeWhen(chargingOnly: Boolean, wifiOnly: Boolean) = Unit
}
