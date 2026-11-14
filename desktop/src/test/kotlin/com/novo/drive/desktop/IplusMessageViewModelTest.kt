package com.novo.drive.desktop

import com.novo.drive.core.common.AppResult
import com.novo.drive.core.files.FileImporter
import com.novo.drive.core.files.ImportSource
import com.novo.drive.core.files.ImportedFile
import com.novo.drive.core.telegram.IncomingTelegramMessage
import com.novo.drive.core.telegram.RemoteDocument
import com.novo.drive.core.telegram.TelegramClient
import com.novo.drive.core.telegram.TelegramDownloadEvent
import com.novo.drive.core.telegram.TelegramUploadEvent
import com.novo.drive.data.local.dao.FamilyDao
import com.novo.drive.data.local.dao.IplusMessageDao
import com.novo.drive.data.local.entity.FamilyMemberEntity
import com.novo.drive.data.local.entity.IplusMessageContactEntity
import com.novo.drive.data.local.entity.IplusMessageEntity
import com.novo.drive.data.local.entity.IplusMessageGroupEntity
import com.novo.drive.domain.model.AccountRole
import com.novo.drive.domain.model.BackupState
import com.novo.drive.domain.model.DriveFile
import com.novo.drive.domain.model.FileCategory
import com.novo.drive.domain.model.UserPreferences
import com.novo.drive.domain.repository.FileRepository
import com.novo.drive.domain.repository.SettingsRepository
import com.novo.drive.presentation.iplusmessage.IplusMessageViewModel
import java.io.File
import java.lang.reflect.Proxy
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IplusMessageViewModelTest {

    private val tempFile = File.createTempFile("dewan-attach", ".txt").apply {
        writeText("hello attachment")
    }

    @After
    fun tearDown() {
        tempFile.delete()
    }

    private inline fun <reified T : Any> unused(): T = Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf(T::class.java)
    ) { _, method, _ -> throw UnsupportedOperationException(method.name) } as T

    private class FakeMessageDao : IplusMessageDao {
        val contacts = MutableStateFlow<List<IplusMessageContactEntity>>(emptyList())
        val messages = MutableStateFlow<List<IplusMessageEntity>>(emptyList())
        val groups = MutableStateFlow<List<IplusMessageGroupEntity>>(emptyList())

        override fun observeContacts(): Flow<List<IplusMessageContactEntity>> =
            contacts.map { list -> list.filter { !it.isBlocked } }

        override suspend fun getContact(userId: Long): IplusMessageContactEntity? =
            contacts.value.firstOrNull { it.userId == userId }

        override suspend fun upsertContact(contact: IplusMessageContactEntity) {
            contacts.update { list -> list.filterNot { it.userId == contact.userId } + contact }
        }

        override suspend fun setBlocked(userId: Long, blocked: Boolean) {
            contacts.update { list -> list.map { if (it.userId == userId) it.copy(isBlocked = blocked) else it } }
        }

        override fun observeBlockedContacts(): Flow<List<IplusMessageContactEntity>> =
            contacts.map { list -> list.filter { it.isBlocked } }

        override fun observeMessages(chatId: Long): Flow<List<IplusMessageEntity>> =
            messages.map { list -> list.filter { it.chatId == chatId } }

        override suspend fun insertMessage(message: IplusMessageEntity) {
            messages.update { it + message }
        }

        override suspend fun updateMessageStatus(messageId: Long, status: String) {
            messages.update { list -> list.map { if (it.messageId == messageId) it.copy(status = status) else it } }
        }

        override suspend fun markMessagesAsRead(chatId: Long) {}
        override suspend fun markViewOnceViewed(messageId: Long) {
            messages.update { list ->
                list.map { if (it.messageId == messageId) it.copy(isViewed = true) else it }
            }
        }
        override suspend fun deleteMessage(messageId: Long) {
            messages.update { list -> list.filterNot { it.messageId == messageId } }
        }

        override suspend fun deleteMessagesForChat(chatId: Long) {
            messages.update { list -> list.filterNot { it.chatId == chatId } }
        }

        override fun observeGroups(): Flow<List<IplusMessageGroupEntity>> = groups.asStateFlow()
        override suspend fun getGroup(groupId: Long): IplusMessageGroupEntity? =
            groups.value.firstOrNull { it.groupId == groupId }

        override suspend fun upsertGroup(group: IplusMessageGroupEntity) {
            groups.update { list -> list.filterNot { it.groupId == group.groupId } + group }
        }

        override suspend fun updateGroupReadOnly(groupId: Long, isReadOnly: Boolean) {
            groups.update { list -> list.map { if (it.groupId == groupId) it.copy(isReadOnly = isReadOnly) else it } }
        }
        override suspend fun updateGroupAdmins(groupId: Long, adminIds: String) {
            groups.update { list -> list.map { if (it.groupId == groupId) it.copy(adminIds = adminIds) else it } }
        }
        override suspend fun updateGroupAllowedSenders(groupId: Long, allowedSenderIds: String) {
            groups.update { list -> list.map { if (it.groupId == groupId) it.copy(allowedSenderIds = allowedSenderIds) else it } }
        }
        override suspend fun updateGroupUsername(groupId: Long, username: String?) {}
        override suspend fun updateGroupTitle(groupId: Long, title: String) {}
        override suspend fun deleteGroup(groupId: Long) {
            groups.update { list -> list.filterNot { it.groupId == groupId } }
        }
    }

    private fun fakeFamilyDao(slots: List<FamilyMemberEntity> = emptyList()): FamilyDao =
        object : FamilyDao by unused() {
            override suspend fun getAll(): List<FamilyMemberEntity> = slots
        }

    private fun fakeSettings(prefs: UserPreferences = UserPreferences()): SettingsRepository =
        object : SettingsRepository by unused() {
            override val preferences: Flow<UserPreferences> = flowOf(prefs)
        }

    private fun fakeFiles(): FileRepository =
        object : FileRepository by unused() {
            override fun observeRecent(limit: Int): Flow<List<DriveFile>> = flowOf(emptyList())
            override suspend fun getFile(id: String): DriveFile? = null
        }

    private fun fakeImporter(): FileImporter =
        object : FileImporter {
            override fun import(reference: String): ImportedFile? {
                val f = File(reference)
                return if (f.isFile) ImportedFile(f.absolutePath, f.name, f.length()) else null
            }
            override fun expand(reference: String): List<ImportSource> = emptyList()
            override fun discard(imported: ImportedFile) {}
            override fun isStaged(path: String): Boolean = false
            override fun sweepOrphans(referencedPaths: Set<String>) {}
        }

    private suspend fun waitUntil(predicate: () -> Boolean) {
        withTimeout(10_000L) {
            while (!predicate()) delay(25L)
        }
    }

    @Test
    fun `10-digit Telegram user ID resolves private chat and sends text`() = runBlocking {
        val dao = FakeMessageDao()
        val createdChats = mutableListOf<Long>()
        val sentTexts = mutableListOf<Pair<Long, String>>()
        val tg = object : TelegramClient by unused() {
            override suspend fun createPrivateChat(userId: Long): Long {
                createdChats += userId
                return userId
            }
            override suspend fun sendTextMessage(chatId: Long, text: String): Long? {
                sentTexts += chatId to text
                return 9001L
            }
        }
        val vm = IplusMessageViewModel(dao, fakeFamilyDao(), fakeSettings(), fakeFiles(), tg, fakeImporter())

        val tenDigitUserId = 5_123_456_789L
        vm.sendMessage(tenDigitUserId, "Salam", null, false, "Friend")
        waitUntil { dao.messages.value.singleOrNull()?.status == "SENT" }

        assertEquals(listOf(tenDigitUserId), createdChats)
        assertEquals(listOf(tenDigitUserId to "Salam"), sentTexts)
    }

    @Test
    fun `createPrivateChat failure marks FAILED and retry worker succeeds on recovery`() = runBlocking {
        val dao = FakeMessageDao()
        var attempts = 0
        val tg = object : TelegramClient by unused() {
            override suspend fun createPrivateChat(userId: Long): Long {
                attempts++
                if (attempts == 1) error("network error")
                return userId
            }
            override suspend fun sendTextMessage(chatId: Long, text: String): Long? = 9002L
        }
        val vm = IplusMessageViewModel(dao, fakeFamilyDao(), fakeSettings(), fakeFiles(), tg, fakeImporter())

        vm.sendMessage(5_999_888_777L, "Retry me", null, false, "Friend")
        waitUntil { dao.messages.value.singleOrNull()?.status == "SENT" }

        assertTrue("Expected retry after failure", attempts >= 2)
    }

    @Test
    fun `synthetic local contacts do not call createPrivateChat`() = runBlocking {
        val dao = FakeMessageDao()
        var tdlibCalls = 0
        val tg = object : TelegramClient by unused() {
            override suspend fun createPrivateChat(userId: Long): Long {
                tdlibCalls++
                return userId
            }
            override suspend fun sendTextMessage(chatId: Long, text: String): Long? {
                tdlibCalls++
                return 1L
            }
        }
        val vm = IplusMessageViewModel(dao, fakeFamilyDao(), fakeSettings(), fakeFiles(), tg, fakeImporter())

        vm.sendMessage(1L, "Local", null, false, "Owner")
        vm.sendMessage(7777777L, "Local creator", null, false, "Ismail")
        waitUntil { dao.messages.value.size == 2 && dao.messages.value.all { it.status == "SENT" } }

        assertEquals(0, tdlibCalls)
    }

    @Test
    fun `local file attachment uploads to Telegram and missing file marks FAILED`() = runBlocking {
        val dao = FakeMessageDao()
        val uploadedNames = mutableListOf<String>()
        val tg = object : TelegramClient by unused() {
            override suspend fun createPrivateChat(userId: Long): Long = userId
            override fun uploadDocument(
                chatId: Long,
                localPath: String,
                fileName: String,
                mimeType: String,
                caption: String,
                thumbnailPath: String?
            ): Flow<TelegramUploadEvent> = flow {
                uploadedNames += fileName
                emit(
                    TelegramUploadEvent.Completed(
                        RemoteDocument(
                            chatId = chatId,
                            messageId = 101L,
                            remoteFileId = "rf-1",
                            uniqueFileId = "uf-1",
                            fileName = fileName,
                            mimeType = mimeType,
                            sizeBytes = 16L,
                            caption = caption,
                            dateSeconds = 1
                        )
                    )
                )
            }
        }
        val vm = IplusMessageViewModel(dao, fakeFamilyDao(), fakeSettings(), fakeFiles(), tg, fakeImporter())

        vm.sendMessage(222_333_444L, "", "file://${tempFile.absolutePath}", false, "User")
        waitUntil { dao.messages.value.singleOrNull()?.status == "SENT" }
        assertEquals(listOf(tempFile.name), uploadedNames)

        vm.sendMessage(222_333_444L, "", "file:///does/not/exist/missing.bin", false, "User")
        waitUntil { dao.messages.value.size == 2 && dao.messages.value.last().status == "FAILED" }
    }

    @Test
    fun `deleteGroup removes both group and its messages`() = runBlocking {
        val dao = FakeMessageDao()
        dao.upsertGroup(IplusMessageGroupEntity(groupId = 555L, title = "Test", ownerId = 0L))
        dao.insertMessage(IplusMessageEntity(1L, 555L, 0L, "hi", 1L, true))
        val vm = IplusMessageViewModel(dao, fakeFamilyDao(), fakeSettings(), fakeFiles(), unused(), fakeImporter())

        vm.deleteGroup(555L)
        waitUntil { dao.groups.value.isEmpty() && dao.messages.value.isEmpty() }
    }

    @Test
    fun `incoming Telegram DM persists message and sender contact into Room`() = runBlocking {
        val dao = FakeMessageDao()
        val incomingFlow = MutableSharedFlow<IncomingTelegramMessage>(extraBufferCapacity = 4)
        val tg = object : TelegramClient by unused() {
            override fun incomingMessages(): Flow<IncomingTelegramMessage> = incomingFlow
        }
        IplusMessageViewModel(dao, fakeFamilyDao(), fakeSettings(), fakeFiles(), tg, fakeImporter())
        waitUntil { incomingFlow.subscriptionCount.value > 0 }

        incomingFlow.emit(
            IncomingTelegramMessage(
                messageId = 8001L,
                chatId = 900123L,
                senderId = 900123L,
                senderName = "Ismail",
                senderUsername = "ismail_kunjo",
                isGroup = false,
                text = "Hello from Telegram!",
                timestampMs = 123456L
            )
        )

        waitUntil { dao.messages.value.size == 1 && dao.contacts.value.size == 1 }
        val saved = dao.messages.value.single()
        assertEquals("Hello from Telegram!", saved.text)
        assertEquals("DELIVERED", saved.status)
        assertFalse(saved.isOutgoing)
        assertEquals("ismail_kunjo", dao.contacts.value.single().username)
    }

    @Test
    fun `incoming message from blocked contact is ignored`() = runBlocking {
        val dao = FakeMessageDao()
        dao.upsertContact(
            IplusMessageContactEntity(
                userId = 444555L,
                username = "spammer",
                displayName = "Spammer",
                isBlocked = true
            )
        )
        val incomingFlow = MutableSharedFlow<IncomingTelegramMessage>(extraBufferCapacity = 4)
        val tg = object : TelegramClient by unused() {
            override fun incomingMessages(): Flow<IncomingTelegramMessage> = incomingFlow
        }
        val vm = IplusMessageViewModel(dao, fakeFamilyDao(), fakeSettings(), fakeFiles(), tg, fakeImporter())
        waitUntil { incomingFlow.subscriptionCount.value > 0 }

        incomingFlow.emit(
            IncomingTelegramMessage(
                messageId = 8002L,
                chatId = 444555L,
                senderId = 444555L,
                isGroup = false,
                text = "Blocked spam",
                timestampMs = 999L
            )
        )
        delay(200)
        assertEquals(0, dao.messages.value.size)

        vm.markViewOnceViewed(0L) // no-op sanity
    }

    @Test
    fun `single-character search for family member does not match owner`() = runBlocking {
        val dao = FakeMessageDao()
        val vm = IplusMessageViewModel(
            dao,
            fakeFamilyDao(),
            fakeSettings(UserPreferences(accountRole = AccountRole.FAMILY_MEMBER)),
            fakeFiles(),
            unused(),
            fakeImporter()
        )
        vm.searchUsers("o")
        delay(450)
        assertTrue("1-char query must not leak owner match", vm.searchResults.value.isEmpty())
    }

    @Test
    fun `tryRemoveMember blocks admin from removing another admin and allows owner`() = runBlocking {
        val dao = FakeMessageDao()
        dao.upsertGroup(
            IplusMessageGroupEntity(
                groupId = 700L,
                title = "Core",
                ownerId = 10L,
                adminIds = "10,20,30",
                memberCount = 3
            )
        )
        val vm = IplusMessageViewModel(dao, fakeFamilyDao(), fakeSettings(), fakeFiles(), unused(), fakeImporter())

        // Admin (20) tries to remove Admin (30) -> rejected!
        assertFalse(vm.tryRemoveMember(groupId = 700L, requesterId = 20L, targetUserId = 30L))
        // Admin (20) tries to remove Owner (10) -> rejected!
        assertFalse(vm.tryRemoveMember(groupId = 700L, requesterId = 20L, targetUserId = 10L))
        // Owner (10) removes Admin (30) -> allowed!
        assertTrue(vm.tryRemoveMember(groupId = 700L, requesterId = 10L, targetUserId = 30L))
        assertFalse(dao.getGroup(700L)!!.adminIds.contains("30"))
    }

    @Test
    fun `markViewOnceViewed marks message isViewed true`() = runBlocking {
        val dao = FakeMessageDao()
        dao.insertMessage(
            IplusMessageEntity(
                messageId = 919L,
                chatId = 55L,
                senderId = 55L,
                text = "Secret secret",
                timestamp = 10L,
                isOutgoing = false,
                isViewOnce = true,
                isViewed = false
            )
        )
        val vm = IplusMessageViewModel(dao, fakeFamilyDao(), fakeSettings(), fakeFiles(), unused(), fakeImporter())
        vm.markViewOnceViewed(919L)
        waitUntil { dao.messages.value.single().isViewed }
    }

    @Test
    fun `saveToMyDrive downloads tgremote attachment and imports into Drive`() = runBlocking {
        val dao = FakeMessageDao()
        var importedPath: String? = null
        var importedName: String? = null
        val tg = object : TelegramClient by unused() {
            override fun downloadDocument(remoteFileId: String): Flow<TelegramDownloadEvent> = flow {
                emit(TelegramDownloadEvent.Completed(tempFile.absolutePath, tempFile.length()))
            }
        }
        val files = object : FileRepository by unused() {
            override fun observeRecent(limit: Int): Flow<List<DriveFile>> = flowOf(emptyList())
            override suspend fun importLocalFile(
                localPath: String,
                folderId: String?,
                displayName: String?
            ): AppResult<DriveFile> {
                importedPath = localPath
                importedName = displayName
                return AppResult.Success(
                    DriveFile(
                        id = "saved-1",
                        folderId = null,
                        name = displayName ?: "file",
                        sizeBytes = tempFile.length(),
                        mimeType = "text/plain",
                        category = FileCategory.DOCUMENT,
                        localPath = localPath,
                        contentHash = null,
                        chatId = null,
                        messageId = null,
                        remoteFileId = null,
                        remoteUniqueId = null,
                        backupState = BackupState.NONE,
                        isHidden = false,
                        isArchived = false,
                        isFavorite = false,
                        isEncrypted = false,
                        width = null,
                        height = null,
                        durationMs = null,
                        trashedAt = null,
                        createdAt = 1L,
                        modifiedAt = 1L,
                        addedAt = 1L
                    )
                )
            }
        }
        val vm = IplusMessageViewModel(dao, fakeFamilyDao(), fakeSettings(), files, tg, fakeImporter())

        var savedId: String? = null
        vm.saveToMyDrive("tgremote://remote-abc|notes.txt|text/plain") { savedId = it.id }
        waitUntil { savedId == "saved-1" }
        assertEquals(tempFile.absolutePath, importedPath)
        assertEquals("notes.txt", importedName)
    }
}

