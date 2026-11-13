package com.novo.drive.presentation.files

import androidx.compose.runtime.Composable

@Composable
actual fun rememberStorageAccessGranted(): Boolean = true

@Composable
actual fun openAllFilesAccessSettings(): () -> Unit = {}
