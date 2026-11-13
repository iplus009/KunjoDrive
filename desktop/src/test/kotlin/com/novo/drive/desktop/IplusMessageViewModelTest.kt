package com.novo.drive.desktop

import com.novo.drive.core.files.FileImporter
import com.novo.drive.core.files.ImportSource
import com.novo.drive.core.files.ImportedFile
import com.novo.drive.core.telegram.RemoteDocument
import com.novo.drive.core.telegram.TelegramClient
import com.novo.drive.core.telegram.TelegramUploadEvent
import com.novo.drive.data.local.dao.FamilyDao
import com.novo.drive.data.local.dao.IplusMessageDao
import com.novo.drive.data.local.entity.FamilyMemberEntity
import com.novo.drive.data.local.entity.IplusMessageContactEntity
import com.novo.drive.data.local.entity.IplusMessageEntity
import com.novo.drive.data.local.entity.IplusMessageGroupEntity
import com.novo.drive.domain.model.DriveFile
import com.novo.drive.domain.model.UserPreferences
import com.novo.drive.domain.repository.FileRepository
import com.novo.drive.domain.repository.SettingsRepository
import com.novo.drive.presentation.iplusmessage.IplusMessageViewModel
import java.io.File
import java.lang.reflect.Proxy
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
        override suspend fun markViewOnceViewed(messageId: Long) {}
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

        override suspend fun updateGroupReadOnly(groupId: Long, isReadOnly: Boolean) {}
        override suspend fun updateGroupAdmins(groupId: Long, adminIds: String) {}
        override suspend fun updateGroupAllowedSenders(groupId: Long, allowedSenderIds: String) {}
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

    private fun fakeSettings(): SettingsRepository =
        object : SettingsRepository by unused() {
            override val preferences: Flow<UserPreferences> = flowOf(UserPreferences())
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
}

