package com.novo.drive.presentation.home

import com.novo.drive.domain.model.DriveChannel
import com.novo.drive.domain.model.UserPreferences

internal data class HomeSettings(
    val prefs: UserPreferences,
    val syncing: Boolean,
    val activeDrive: DriveChannel?,
    val charging: Boolean
)
