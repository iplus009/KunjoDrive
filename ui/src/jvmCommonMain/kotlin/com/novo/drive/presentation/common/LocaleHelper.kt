package com.novo.drive.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLocaleList

@Composable
fun isBengaliUi(): Boolean =
    LocalLocaleList.current.firstOrNull()?.language?.startsWith("bn", ignoreCase = true) == true
