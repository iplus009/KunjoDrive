package com.novo.drive.presentation.platform

import androidx.compose.runtime.Composable
import com.novo.drive.presentation.preview.PreviewScreen

object AndroidPlatformScreens : PlatformScreens {

    @Composable
    override fun Preview(
        onBack: () -> Unit,
        onEditNote: (String, String) -> Unit,
        onShareToDewanGram: ((String) -> Unit)?
    ) {
        PreviewScreen(
            onBack = onBack,
            onEditNote = onEditNote,
            onShareToDewanGram = onShareToDewanGram
        )
    }
}
