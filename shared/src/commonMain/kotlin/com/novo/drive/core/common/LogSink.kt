package com.novo.drive.core.common

fun interface LogSink {
    fun write(level: Char, tag: String, message: String, throwable: Throwable?)
}
