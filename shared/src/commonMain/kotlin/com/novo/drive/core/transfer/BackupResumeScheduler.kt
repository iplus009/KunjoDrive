package com.novo.drive.core.transfer

interface BackupResumeScheduler {
    fun resumeWhen(chargingOnly: Boolean, wifiOnly: Boolean)
}
