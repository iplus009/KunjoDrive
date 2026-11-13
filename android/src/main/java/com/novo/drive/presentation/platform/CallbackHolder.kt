package com.novo.drive.presentation.platform

internal class CallbackHolder<T> {

    private var pending: ((T) -> Unit)? = null

    fun arm(callback: (T) -> Unit) {
        pending = callback
    }

    fun fire(value: T) {
        pending?.invoke(value)
        pending = null
    }
}
