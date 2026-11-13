package com.novo.drive.core.update

data class AppRelease(
    val version: String,
    val notes: String,
    val pageUrl: String,
    val apkUrl: String? = null
)
