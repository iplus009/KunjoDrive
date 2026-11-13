package com.novo.drive.domain.repository

import com.novo.drive.domain.model.Exclusion
import com.novo.drive.domain.model.ExclusionType
import kotlinx.coroutines.flow.Flow

interface ExclusionRepository {

    fun observeAll(): Flow<List<Exclusion>>

    /** Only adds a rule the drive has none of, so a rule the user removed stays removed. */
    suspend fun ensureDefaults(chatId: Long)

    suspend fun getEnabled(): List<Exclusion>

    suspend fun add(type: ExclusionType, value: String): Exclusion

    suspend fun setEnabled(id: String, enabled: Boolean)

    suspend fun remove(id: String)
}
