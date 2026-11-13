package com.novo.drive.desktop.ui

import androidx.compose.runtime.Composable
import com.novo.drive.presentation.platform.PlatformScreens

object DesktopPlatformScreens : PlatformScreens {

    @Composable
    override fun Preview(
        onBack: () -> Unit,
        onEditNote: (String, String) -> Unit,
        onShareToDewanGram: ((String) -> Unit)?
    ) {
        DesktopPreviewScreen(onBack = onBack)
    }
}
