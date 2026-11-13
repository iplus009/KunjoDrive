package com.novo.drive.core.transfer

import com.novo.drive.core.network.NetworkStatus
import com.novo.drive.domain.model.BackupHold
import com.novo.drive.domain.model.UserPreferences

/** Applies to backup transfers only; uploads and downloads the user starts are never held. */
object BackupGate {

    fun hold(
        prefs: UserPreferences,
        network: NetworkStatus,
        charging: Boolean,
        currentHour: Int = -1
    ): BackupHold? = when {
        prefs.backupChargingOnly && !charging -> BackupHold.CHARGER
        prefs.backupWifiOnly && network != NetworkStatus.UNMETERED -> BackupHold.WIFI
        prefs.bedtimeSyncEnabled && currentHour in 0..23 &&
                !isWithinWindow(currentHour, prefs.bedtimeStartHour, prefs.bedtimeEndHour) ->
            BackupHold.BEDTIME_WINDOW
        else -> null
    }

    fun isWithinWindow(hour: Int, startHour: Int, endHour: Int): Boolean {
        val s = startHour.coerceIn(0, 23)
        val e = endHour.coerceIn(0, 23)
        return when {
            s == e -> true
            s < e -> hour in s until e
            else -> hour >= s || hour < e
        }
    }
}
