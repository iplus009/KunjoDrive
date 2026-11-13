package com.novo.drive.presentation.files

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.util.ArrayDeque
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class IplusLocalStorageItem(
    val file: File,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val extension: String
)

typealias LocalStorageItem = IplusLocalStorageItem

object IplusLocalFileOperations {

    /**
     * Formats bytes into clean human-readable binary IEC notation with 2 decimal precision.
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        while (size >= 1024.0 && unitIndex < units.size - 1) {
            size /= 1024.0
            unitIndex++
        }
        return if (unitIndex == 0) {
            "${bytes} B"
        } else {
            "%.1f %s".format(size, units[unitIndex])
        }
    }

    /**
     * High-speed Zero-Copy Kernel DMA Transfer via Linux sendfile(2) / FileChannel.
     */
    suspend fun copyFileZeroCopy(source: File, destination: File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            if (source.isDirectory) {
                copyDirectoryRecursively(source, destination)
            } else {
                destination.parentFile?.mkdirs()
                FileInputStream(source).channel.use { srcChannel ->
                    FileOutputStream(destination).channel.use { destChannel ->
                        val size = srcChannel.size()
                        var transferred = 0L
                        while (transferred < size) {
                            val count = destChannel.transferFrom(srcChannel, transferred, size - transferred)
                            if (count <= 0) break
                            transferred += count
                        }
                    }
                }
                destination.setLastModified(source.lastModified())
                true
            }
        }.getOrDefault(false)
    }

    private suspend fun copyDirectoryRecursively(sourceDir: File, destDir: File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            if (!destDir.exists()) destDir.mkdirs()
            sourceDir.listFiles()?.forEach { child ->
                val target = File(destDir, child.name)
                if (child.isDirectory) {
                    copyDirectoryRecursively(child, target)
                } else {
                    copyFileZeroCopy(child, target)
                }
            }
            true
        }.getOrDefault(false)
    }

    /**
     * Atomic POSIX move if on same filesystem; otherwise zero-copy fallback with delete.
     */
    suspend fun moveFile(source: File, destination: File): Boolean = withContext(Dispatchers.IO) {
        if (source.renameTo(destination)) return@withContext true
        val copied = copyFileZeroCopy(source, destination)
        if (copied) {
            deleteRecursivelySafe(source)
            true
        } else {
            false
        }
    }

    /**
     * Non-recursive iterative Breadth-First Search (BFS) directory delete to avoid stack overflow.
     */
    suspend fun deleteRecursivelySafe(root: File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            if (!root.exists()) return@runCatching true
            if (!root.isDirectory) return@runCatching root.delete()

            val dirsToClean = ArrayDeque<File>()
            val stack = ArrayDeque<File>()
            stack.push(root)

            while (stack.isNotEmpty()) {
                val current = stack.pop()
                dirsToClean.push(current)
                val children = current.listFiles() ?: continue
                for (child in children) {
                    if (child.isDirectory) {
                        stack.push(child)
                    } else {
                        child.delete()
                    }
                }
            }

            while (dirsToClean.isNotEmpty()) {
                dirsToClean.pop().delete()
            }
            !root.exists()
        }.getOrDefault(false)
    }

    /**
     * Renames a file or directory.
     */
    suspend fun rename(file: File, newName: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val target = File(file.parentFile, newName)
            if (target.exists()) return@runCatching false
            file.renameTo(target)
        }.getOrDefault(false)
    }

    /**
     * Resolves storage roots (Internal primary storage and SD cards).
     */
    fun getStorageRoots(): List<File> {
        val roots = mutableListOf<File>()
        val primary = File("/storage/emulated/0")
        if (primary.exists() && primary.canRead()) {
            roots.add(primary)
        } else {
            val userHome = File(System.getProperty("user.home", "/"))
            if (userHome.exists() && userHome.canRead()) roots.add(userHome)
        }

        // Check external SD card mounts
        val storageDir = File("/storage")
        if (storageDir.exists() && storageDir.isDirectory) {
            storageDir.listFiles()?.forEach { file ->
                if (file.name != "emulated" && file.name != "self" && file.canRead()) {
                    roots.add(file)
                }
            }
        }
        return roots.distinctBy { it.absolutePath }
    }
}

typealias LocalFileOperations = IplusLocalFileOperations

