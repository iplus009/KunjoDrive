package com.novo.drive.presentation.components

import java.io.File
import javax.imageio.ImageIO

actual fun decodeQrPixels(path: String): Triple<Int, Int, IntArray>? = runCatching {
    val img = ImageIO.read(File(path)) ?: return@runCatching null
    val w = img.width
    val h = img.height
    val pixels = img.getRGB(0, 0, w, h, null, 0, w)
    Triple(w, h, pixels)
}.getOrNull()

