package com.novo.drive.presentation.platform

import androidx.compose.runtime.staticCompositionLocalOf
import com.novo.drive.core.files.DeleteConsentRequest

fun interface DeleteConsentLauncher {

    fun launch(request: DeleteConsentRequest, onResult: (Boolean) -> Unit)
}

val LocalDeleteConsentLauncher = staticCompositionLocalOf<DeleteConsentLauncher> {
    error("DeleteConsentLauncher is not provided")
}
