package com.novo.drive.presentation.preview

import android.content.pm.ActivityInfo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ScreenLockLandscape
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.ui.graphics.vector.ImageVector

internal enum class IplusPlayerRotation(
    val icon: ImageVector,
    val description: String,
    val orientation: Int
) {
    AUTO(
        Icons.Filled.ScreenRotation,
        "Rotation: follow device",
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    ),
    LANDSCAPE(
        Icons.Filled.ScreenLockLandscape,
        "Rotation: locked landscape",
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    ),
    PORTRAIT(
        Icons.Filled.ScreenLockPortrait,
        "Rotation: locked portrait",
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    );

    fun next(): IplusPlayerRotation = entries[(ordinal + 1) % entries.size]
}

