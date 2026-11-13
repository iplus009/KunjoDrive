package com.novo.drive.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Live camera QR scanner. Shows a full-screen camera preview and calls
 * [onResult] exactly once when a QR code is decoded, then the caller
 * should remove this composable from composition.
 *
 * On platforms without a camera (desktop), this renders nothing and
 * never calls back — callers should gate it behind a platform check.
 */
@Composable
expect fun LiveQrScanner(
    onResult: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
)
