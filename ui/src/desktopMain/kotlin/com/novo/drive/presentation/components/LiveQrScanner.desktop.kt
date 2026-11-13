package com.novo.drive.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun LiveQrScanner(
    onResult: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier,
) {
    // Desktop has no camera path — nothing to render.
}
