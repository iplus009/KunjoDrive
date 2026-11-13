package com.novo.drive.presentation.files

import androidx.compose.runtime.Composable

/**
 * Returns true if the app can list arbitrary files on shared storage.
 * On Android 11+ this means MANAGE_EXTERNAL_STORAGE has been granted.
 * On older Android and desktop this is always true.
 */
@Composable
expect fun rememberStorageAccessGranted(): Boolean

/**
 * Returns a lambda that opens the system screen where the user can grant
 * all-files access. No-op on platforms where this is not needed.
 */
@Composable
expect fun openAllFilesAccessSettings(): () -> Unit
