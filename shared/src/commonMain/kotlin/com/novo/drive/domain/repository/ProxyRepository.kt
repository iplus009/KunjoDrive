package com.novo.drive.domain.repository

import com.novo.drive.core.common.AppResult
import com.novo.drive.core.proxy.ProxyProbeResult
import com.novo.drive.domain.model.ProxyServer
import kotlinx.coroutines.flow.Flow

interface ProxyRepository {

    fun observeProxies(): Flow<List<ProxyServer>>

    suspend fun save(proxy: ProxyServer): AppResult<Unit>

    suspend fun delete(id: String): AppResult<Unit>

    suspend fun select(id: String): AppResult<Unit>

    suspend fun setEnabled(enabled: Boolean): AppResult<Unit>

    suspend fun applyActive()

    /** Returns false when there is nothing else to try. */
    suspend fun rotate(): AppResult<Boolean>

    suspend fun test(proxy: ProxyServer): AppResult<ProxyProbeResult>
}
