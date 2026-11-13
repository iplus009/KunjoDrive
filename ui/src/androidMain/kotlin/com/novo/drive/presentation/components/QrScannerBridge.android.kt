package com.novo.drive.presentation.components

import android.graphics.BitmapFactory

actual fun decodeQrPixels(path: String): Triple<Int, Int, IntArray>? = runCatching {
    val bmp = BitmapFactory.decodeFile(path) ?: return@runCatching null
    val w = bmp.width
    val h = bmp.height
    val pixels = IntArray(w * h)
    bmp.getPixels(pixels, 0, w, 0, 0, w, h)
    Triple(w, h, pixels)
}.getOrNull()

