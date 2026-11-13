package com.novo.drive.presentation.platform

sealed interface PickResult {

    data object Canceled : PickResult

    data object Unreadable : PickResult

    data class Picked(val path: String) : PickResult
}
