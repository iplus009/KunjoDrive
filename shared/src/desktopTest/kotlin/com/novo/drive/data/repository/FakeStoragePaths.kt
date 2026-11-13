package com.novo.drive.data.repository

import com.novo.drive.core.files.AppStoragePaths
import com.novo.drive.testing.unused
import java.io.File

internal class FakeStoragePaths : AppStoragePaths by unused() {
    private val root = File.createTempFile("novodrive-paths", "").also {
        it.delete()
        it.mkdirs()
    }
    override val cacheDir: File = File(root, "cache").also { it.mkdirs() }
}
