package com.novo.drive.desktop.network

import com.novo.drive.core.network.NetworkMonitor
import com.novo.drive.core.network.NetworkStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class DesktopNetworkMonitor : NetworkMonitor {

    private val state = MutableStateFlow(NetworkStatus.UNMETERED)

    override val status: Flow<NetworkStatus> = state

    override fun currentStatus(): NetworkStatus = state.value
}
