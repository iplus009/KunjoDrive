package com.novo.drive.presentation.platform

import androidx.compose.runtime.staticCompositionLocalOf
import java.io.File

fun interface FileOpener {
    fun openFile(file: File)
    fun open(file: File) = openFile(file)
}

val LocalFileOpener = staticCompositionLocalOf<FileOpener> {
    FileOpener { /* fallback */ }
}

