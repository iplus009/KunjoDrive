package com.novo.drive.presentation.proxy

import com.novo.drive.domain.model.ProxyServer

data class ProxyUiState(
    val proxies: List<ProxyServer> = emptyList(),
    val enabled: Boolean = false,
    val loading: Boolean = true,
    val reachability: Map<String, ProxyReachability> = emptyMap()
)
