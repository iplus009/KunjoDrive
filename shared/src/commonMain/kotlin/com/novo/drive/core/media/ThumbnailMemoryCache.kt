package com.novo.drive.core.media

interface ThumbnailMemoryCache {

    fun remove(fileId: String)

    fun clear()
}
