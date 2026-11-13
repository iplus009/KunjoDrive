package com.novo.drive.presentation.components

internal object KeyboardHandoff {

    var armed = false

    fun take(): Boolean = armed.also { armed = false }
}
