package com.novo.drive.presentation.common

import com.novo.drive.core.common.AppError

sealed interface UiState<out T> {

    data object Loading : UiState<Nothing>

    data class Content<T>(val value: T) : UiState<T>

    data object Empty : UiState<Nothing>

    data class Error(val error: AppError) : UiState<Nothing>
}
