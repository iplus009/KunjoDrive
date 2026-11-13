package com.novo.drive.presentation.platform

import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppVersion = staticCompositionLocalOf<String> {
    error("App version is not provided")
}
