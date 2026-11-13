package com.novo.drive.presentation

import com.novo.drive.domain.model.AppTheme
import com.novo.drive.domain.model.AppLanguage

data class AppUiState(
    val loading: Boolean = true,
    val onboardingComplete: Boolean = false,
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val dynamicColor: Boolean = false,
    val compactLayout: Boolean = false,
    val locked: Boolean = false
)
