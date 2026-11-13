package com.novo.drive.data.repository

import com.novo.drive.core.crypto.StreamCrypto
import com.novo.drive.core.files.NoteStore
import com.novo.drive.data.local.entity.FileEntity
import com.novo.drive.data.local.entity.FolderEntity
import com.novo.drive.domain.model.BackupState
import com.novo.drive.domain.model.FileCategory
import com.novo.drive.testing.unused
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The decoy vault marks files with `secondVaultFileIds`, but the visible list is driven by
 * `isHidden`. Transferring only the id set left vault files on Home, Gallery and Search, so these
 * tests pin the real hiding behaviour.
 */
class FolderTreeHiddenTest {

    private val harness = SyncHarness()
    private val database = harness.database
    private val folders = database.folderDao()
    private val files = database.fileDao()

    @After
    fun tearDown() = harness.close()

    @Test
    fun `hiding a folder tree hides every folder and file beneath it`() = runBlocking {
        seedTree()
        val repository = repository()

        repository.setFolderTreeHidden("root", true)

        assertTrue(folders.byId("root")!!.isHidden)
        assertTrue(folders.byId("child")!!.isHidden)
        assertTrue(files.byId("rootFile")!!.isHidden)
        assertTrue(files.byId("childFile")!!.isHidden)
    }

    @Test
    fun `hidden vault files drop out of the recent list`() = runBlocking {
        seedTree()
        val repository = repository()

        repository.setFolderTreeHidden("root", true)

        assertTrue(repository.observeRecent(50).first().isEmpty())
    }

    @Test
    fun `unhiding a folder tree restores every folder and file beneath it`() = runBlocking {
        seedTree()
        val repository = repository()
        repository.setFolderTreeHidden("root", true)

        repository.setFolderTreeHidden("root", false)

        assertFalse(folders.byId("root")!!.isHidden)
        assertFalse(folders.byId("child")!!.isHidden)
        assertFalse(files.byId("rootFile")!!.isHidden)
        assertFalse(files.byId("childFile")!!.isHidden)
        assertEquals(
            setOf("childFile", "rootFile"),
            repository.observeRecent(50).first().map { it.id }.toSet()
        )
    }

    private suspend fun seedTree() {
        folders.upsert(folder("root", parentId = null, name = "Vault"))
        folders.upsert(folder("child", parentId = "root", name = "Inner"))
        files.upsert(file("rootFile", folderId = "root"))
        files.upsert(file("childFile", folderId = "child"))
    }

    private fun repository() = FileRepositoryImpl(
        fileDao = files,
        folderDao = folders,
        publishScheduler = harness.scheduler,
        localCopyDeleter = unused(),
        telegramClient = unused(),
        manifestCodec = harness.codec,
        folderPathResolver = harness.folderPaths,
        activeChannel = harness.activeChannel,
        noteStore = NoteStore(harness.storagePaths),
        streamCrypto = StreamCrypto(),
        wrappedKeyRepository = unused(),
        settingsRepository = harness.settings,
        fileImporter = unused()
    )

    private fun folder(id: String, parentId: String?, name: String) = FolderEntity(
        id = id,
        chatId = TEST_CHAT,
        parentId = parentId,
        name = name,
        createdAt = 1_000,
        modifiedAt = 1_000,
        changedAt = 1_000
    )

    private fun file(id: String, folderId: String?) = FileEntity(
        id = id,
        folderId = folderId,
        name = "$id.bin",
        sizeBytes = 10,
        mimeType = "application/octet-stream",
        category = FileCategory.OTHER,
        localPath = null,
        contentHash = null,
        chatId = TEST_CHAT,
        messageId = 40,
        remoteFileId = "remote-40",
        remoteUniqueId = "unique-40",
        backupState = BackupState.BACKED_UP,
        createdAt = 1_000,
        modifiedAt = 1_000,
        addedAt = 1_000
    )
}
