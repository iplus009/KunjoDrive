package com.novo.drive.domain.usecase

import com.novo.drive.domain.model.DriveFile
import com.novo.drive.domain.repository.FileRepository
import com.novo.drive.testing.unused

internal class FakeFileRepository(
    private val all: List<DriveFile>,
    private val tree: Map<String, List<String>>
) : FileRepository by unused() {
    val offlineFiles = mutableListOf<String>()
    val onlineOnlyFiles = mutableListOf<String>()
    val offlineFolders = mutableListOf<String>()

    override suspend fun setFilesAvailableOffline(ids: List<String>, available: Boolean) {
        (if (available) offlineFiles else onlineOnlyFiles) += ids
    }

    override suspend fun setFolderAvailableOffline(id: String, available: Boolean) {
        if (available) offlineFolders += id
    }

    override suspend fun fileIdsInTree(folderId: String) = tree[folderId].orEmpty()

    override suspend fun filesByIds(ids: List<String>) = all.filter { it.id in ids }
}
