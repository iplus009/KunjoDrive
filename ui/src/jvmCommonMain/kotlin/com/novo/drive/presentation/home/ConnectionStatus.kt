package com.novo.drive.presentation.home

import com.novo.drive.presentation.components.ConnectionIndicator
import org.jetbrains.compose.resources.StringResource

internal data class ConnectionStatus(
    val indicator: ConnectionIndicator,
    val labelRes: StringResource
)
