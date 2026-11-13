package com.novo.drive.presentation.settings

import com.novo.drive.core.telegram.TelegramConnectionState
import com.novo.drive.core.telegram.TelegramUser
import com.novo.drive.domain.model.UserPreferences
import com.novo.drive.domain.repository.CacheStats

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val user: TelegramUser? = null,
    val connection: TelegramConnectionState = TelegramConnectionState.CONNECTING,
    val cacheStats: CacheStats = CacheStats(0, 0, 0, 0, 0),
    val syncing: Boolean = false,
    val loading: Boolean = true
)
