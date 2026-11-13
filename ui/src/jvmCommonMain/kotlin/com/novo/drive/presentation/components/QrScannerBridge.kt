package com.novo.drive.presentation.components

import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer

expect fun decodeQrPixels(path: String): Triple<Int, Int, IntArray>?

object QrScannerBridge {
    fun decodeFromImageFile(path: String): String? = runCatching {
        val (w, h, pixels) = decodeQrPixels(path) ?: return@runCatching null
        val source = RGBLuminanceSource(w, h, pixels)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        MultiFormatReader().decode(bitmap)?.text?.trim()
    }.getOrNull()
}

