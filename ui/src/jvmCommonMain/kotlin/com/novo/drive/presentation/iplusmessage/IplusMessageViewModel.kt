package com.novo.drive.presentation.iplusmessage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novo.drive.core.files.FileImporter
import com.novo.drive.core.network.SupabaseRestClient
import com.novo.drive.core.telegram.TelegramClient
import com.novo.drive.core.telegram.TelegramUploadEvent
import com.novo.drive.data.local.dao.FamilyDao
import com.novo.drive.data.local.dao.IplusMessageDao
import com.novo.drive.data.local.entity.IplusMessageContactEntity
import com.novo.drive.data.local.entity.IplusMessageEntity
import com.novo.drive.data.local.entity.IplusMessageGroupEntity
import com.novo.drive.domain.model.AccountRole
import com.novo.drive.domain.model.DriveFile
import com.novo.drive.domain.model.UserPreferences
import com.novo.drive.domain.repository.FileRepository
import com.novo.drive.domain.repository.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class DewanUserSearchResult(
    val userId: Long,
    val username: String,
    val displayName: String,
    val roleBadge: String,
    val avatarEmoji: String = "👤",
    val isFamilyMember: Boolean = false,
    val isOwner: Boolean = false,
    val isVerifiedAdmin: Boolean = false,
    /** Real Telegram user ID resolved from DewanGram mapping. Zero if unresolved. */
    val telegramUserId: Long = 0L
)

/**
 * Represents a failed message that will be retried automatically.
 * Used by the offline retry queue.
 */
private data class RetryableMessage(
    val messageId: Long,
    val chatId: Long,
    val text: String,
    val driveLink: String?,
    val contactName: String,
    val textSent: Boolean = false,
    val docSent: Boolean = false,
    val retryCount: Int = 0
)

private data class DeliveryOutcome(
    val textSent: Boolean,
    val docSent: Boolean
) {
    val allSucceeded: Boolean get() = textSent && docSent
}

class IplusMessageViewModel(
    private val messageDao: IplusMessageDao,
    private val familyDao: FamilyDao,
    private val settingsRepository: SettingsRepository,
    private val fileRepository: FileRepository,
    private val telegramClient: TelegramClient,
    private val fileImporter: FileImporter
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = settingsRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    val contacts: StateFlow<List<IplusMessageContactEntity>> = messageDao.observeContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groups: StateFlow<List<IplusMessageGroupEntity>> = messageDao.observeGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedContacts: StateFlow<List<IplusMessageContactEntity>> = messageDao.observeBlockedContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentDriveFiles: StateFlow<List<DriveFile>> = fileRepository.observeRecent(50)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pendingShareFile = MutableStateFlow<String?>(null)
    val pendingShareFile: StateFlow<String?> = _pendingShareFile.asStateFlow()

    private val _searchResults = MutableStateFlow<List<DewanUserSearchResult>>(emptyList())
    val searchResults: StateFlow<List<DewanUserSearchResult>> = _searchResults.asStateFlow()

    // ═══════════════════════════════════════════════════════════════
    // Search Debouncing: prevent spam API calls on every keystroke
    // ═══════════════════════════════════════════════════════════════
    private val searchDebounceJob = MutableStateFlow<kotlinx.coroutines.Job?>(null)

    // ═══════════════════════════════════════════════════════════════
    // Rate Limiting: prevent spam sending (max 10 messages per 30 sec)
    // ═══════════════════════════════════════════════════════════════
    private val messageTimestamps = MutableStateFlow<List<Long>>(emptyList())
    private val rateLimitWindowMs = 30_000L
    private val maxMessagesPerWindow = 10

    // ═══════════════════════════════════════════════════════════════
    // Offline Queue: single sequential worker so retries never race
    // ═══════════════════════════════════════════════════════════════
    private val retryQueueMutex = Mutex()
    private val pendingRetries = ArrayDeque<RetryableMessage>()
    private val retryTrigger = Channel<Unit>(Channel.CONFLATED)
    private val maxRetryAttempts = 3
    private val retryBaseDelayMs = 1_500L

    init {
        viewModelScope.launch {
            for (signal in retryTrigger) {
                processRetryQueue()
            }
        }
    }

    fun setPendingShare(filePathOrId: String?) {
        _pendingShareFile.value = filePathOrId
    }

    fun clearPendingShare() {
        _pendingShareFile.value = null
    }

    /**
     * Debounced search - waits 300ms after last keystroke before searching.
     * Prevents spamming Supabase with API requests on every character.
     */
    fun searchUsers(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) {
            searchDebounceJob.value?.cancel()
            _searchResults.value = emptyList()
            return
        }
        // Cancel previous debounce job
        searchDebounceJob.value?.cancel()
        // Start new debounced search
        searchDebounceJob.value = viewModelScope.launch {
            kotlinx.coroutines.delay(300L) // 300ms debounce - wait for user to stop typing
            performSearch(clean)
        }
    }

    /**
     * Actual search execution (called after debounce delay).
     * Separated from searchUsers() to allow direct调用 for testing.
     */
    private suspend fun performSearch(clean: String) {
        val prefs = settingsRepository.preferences.first()
        val results = mutableListOf<DewanUserSearchResult>()
        val cleanUser = clean.removePrefix("@").lowercase()

        // Layer 1: Family Accounts (Cross-Search between Main & Sub-Accounts)

            // Layer 1: Family Accounts (Cross-Search between Main & Sub-Accounts)
            if (prefs.accountRole == AccountRole.OWNER) {
                val slots = familyDao.getAll()
                for (slot in slots) {
                    if (!slot.isRevoked && (
                        slot.memberName.contains(clean, ignoreCase = true) ||
                        "sub_${slot.slotIndex}".contains(cleanUser) ||
                        "slot_${slot.slotIndex}".contains(cleanUser) ||
                        cleanUser.contains("sub") || cleanUser.contains("family") ||
                        cleanUser.contains("member")
                    )) {
                        results.add(
                            DewanUserSearchResult(
                                userId = 1000L + slot.slotIndex,
                                username = "sub_${slot.slotIndex}_${slot.memberName.lowercase().replace(" ", "_")}",
                                displayName = slot.memberName,
                                roleBadge = "👨‍👩‍👧 সাব-অ্যাকাউন্ট (স্লট ${slot.slotIndex})",
                                avatarEmoji = "👤",
                                isFamilyMember = true
                            )
                        )
                    }
                }
            } else if (prefs.accountRole == AccountRole.FAMILY_MEMBER) {
                // Sub-Account searching Main Account / Owner
                if ("owner".contains(cleanUser) || "main".contains(cleanUser) || "admin".contains(cleanUser) ||
                    "cloud".contains(cleanUser) || cleanUser.contains("dewan") || cleanUser.contains("kunjo")
                ) {
                    results.add(
                        DewanUserSearchResult(
                            userId = 1L,
                            username = "cloud_owner",
                            displayName = "ক্লাউড ওনার (Main Account)",
                            roleBadge = "👑 ক্লাউড ওনার (মেইন অ্যাকাউন্ট)",
                            avatarEmoji = "👑",
                            isOwner = true
                        )
                    )
                }
            }

            // Layer 1.5: Ismail Bhai - Super Admin / Creator Identity (Highest Priority)
            val ismailAliases = setOf(
                "ismail", "ismail01", "ismaiq01", "dmnur", "nur", "md ismail", "dmnurq01", "ismail2250"
            )
            val queryLower = clean.lowercase()
            if (ismailAliases.any { queryLower.contains(it) || it.contains(queryLower) } || clean.contains("ইসমাইল", ignoreCase = true)) {
                results.add(
                    0,
                    DewanUserSearchResult(
                        userId = 7777777L,
                        username = "ismail",
                        displayName = "Md Ismail (ইসমাইল ভাই)",
                        roleBadge = "👑 অ্যাডমিন / ক্রিয়েটর (Admin / Creator)",
                        avatarEmoji = "👑",
                        isFamilyMember = false,
                        isOwner = false,
                        isVerifiedAdmin = true
                    )
                )
            }

            // Layer 2: Local Contacts
            val localContacts = messageDao.observeContacts().first()
            for (c in localContacts) {
                if (c.displayName.contains(clean, ignoreCase = true) || c.username.contains(cleanUser, ignoreCase = true)) {
                    if (results.none { it.userId == c.userId }) {
                        results.add(
                            DewanUserSearchResult(
                                userId = c.userId,
                                username = c.username,
                                displayName = c.displayName,
                                roleBadge = "💬 সংরক্ষিত কন্টাক্ট",
                                avatarEmoji = c.avatarValue
                            )
                        )
                    }
                }
            }

            // Layer 3: DewanGram User Search via Supabase
            // This replaces the old fake @username hash generation with real DewanGram mapping.
            // DewanGram username search returns the real Telegram user ID from our Supabase mapping.
            val dewangramResults = SupabaseRestClient.searchDewangramUsers(clean)
            for (dg in dewangramResults) {
                val duplicate = results.any {
                    (dg.telegramUserId != 0L && (it.userId == dg.telegramUserId || it.telegramUserId == dg.telegramUserId)) ||
                        it.username.equals(dg.dewangramUsername, ignoreCase = true)
                }
                if (!duplicate) {
                    results.add(
                        DewanUserSearchResult(
                            userId = dg.telegramUserId,
                            username = dg.dewangramUsername,
                            displayName = dg.dewangramUsername,
                            roleBadge = "📱 দেওয়ানগ্রাম",
                            avatarEmoji = "📱",
                            telegramUserId = dg.telegramUserId
                        )
                    )
                }
            }

            _searchResults.value = results
    }

    fun observeMessages(chatId: Long): Flow<List<IplusMessageEntity>> {
        return messageDao.observeMessages(chatId)
    }

    fun sendMessage(chatId: Long, text: String, driveLink: String?, isViewOnce: Boolean, contactName: String = "User") {
        viewModelScope.launch {
            val prefs = preferences.value

            // STRICT SUB-ACCOUNT RESTRICTION 1: Sub-Account CANNOT send text messages or chat! Only drive links allowed!
            if (prefs.isFamilyMember && text.isNotBlank() && driveLink.isNullOrBlank()) {
                return@launch
            }

            // ═══════════════════════════════════════════════════════════════
            // Rate Limiting: max 10 messages per 30 seconds
            // ═══════════════════════════════════════════════════════════════
            val now = System.currentTimeMillis()
            val recentTimestamps = messageTimestamps.value.filter { now - it < rateLimitWindowMs }
            if (recentTimestamps.size >= maxMessagesPerWindow) {
                messageDao.insertMessage(
                    IplusMessageEntity(
                        messageId = System.currentTimeMillis() * 1000,
                        chatId = chatId,
                        senderId = if (prefs.isFamilyMember) (1000L + prefs.familySlotIndex) else 0L,
                        text = if (prefs.isFamilyMember) null else text.ifBlank { null },
                        timestamp = now,
                        isOutgoing = true,
                        status = "RATE_LIMITED",
                        driveLink = driveLink,
                        isViewOnce = isViewOnce,
                        isViewed = false
                    )
                )
                return@launch
            }
            // Update rate limit timestamp
            messageTimestamps.value = recentTimestamps + now

            val messageId = System.currentTimeMillis() * 1000 + kotlin.random.Random.nextInt(1000)
            // Random in [0, 999] appended to timestamp gives ~1000 unique IDs per millisecond

            val entity = IplusMessageEntity(
                messageId = messageId,
                chatId = chatId,
                senderId = if (prefs.isFamilyMember) (1000L + prefs.familySlotIndex) else 0L,
                text = if (prefs.isFamilyMember) null else text.ifBlank { null }, // Sub-account text stripped
                timestamp = System.currentTimeMillis(),
                isOutgoing = true,
                status = "SENDING",
                driveLink = driveLink,
                isViewOnce = isViewOnce,
                isViewed = false
            )
            messageDao.insertMessage(entity)

            // STRICT SUB-ACCOUNT RESTRICTION 2: Sub-Account CANNOT save contacts!
            if (!prefs.isFamilyMember) {
                val existing = messageDao.getContact(chatId)
                if (existing == null) {
                    messageDao.upsertContact(
                        IplusMessageContactEntity(
                            userId = chatId,
                            username = "user_$chatId",
                            displayName = contactName,
                            lastSeenTimestamp = System.currentTimeMillis()
                        )
                    )
                } else {
                    // Update lastSeenTimestamp and displayName for existing contacts on each message
                    val updated = existing.copy(
                        lastSeenTimestamp = System.currentTimeMillis(),
                        displayName = contactName.takeIf { it != "User" && it != existing.displayName } ?: existing.displayName
                    )
                    messageDao.upsertContact(updated)
                }
            }

            // Sub-accounts: messages are local-only, skip Telegram send
            if (prefs.isFamilyMember) {
                messageDao.updateMessageStatus(messageId, "SENT")
                return@launch
            }

            // Synthetic local contacts (1L, 7777777L, family slots) and local-only Room groups have
            // no Telegram counterpart yet, so keep them local instead of throwing bogus IDs at TDLib.
            if (isLocalOnlyChat(chatId)) {
                messageDao.updateMessageStatus(messageId, "SENT")
                return@launch
            }

            val outcome = deliverToTelegram(
                chatId = chatId,
                text = text,
                driveLink = driveLink,
                textAlreadySent = false,
                docAlreadySent = false
            )

            val finalStatus = if (!outcome.allSucceeded) {
                enqueueRetry(
                    RetryableMessage(
                        messageId = messageId,
                        chatId = chatId,
                        text = text,
                        driveLink = driveLink,
                        contactName = contactName,
                        textSent = outcome.textSent,
                        docSent = outcome.docSent,
                        retryCount = 0
                    )
                )
                "FAILED"
            } else {
                "SENT"
            }
            messageDao.updateMessageStatus(messageId, finalStatus)
        }
    }

    private suspend fun isLocalOnlyChat(chatId: Long): Boolean {
        if (chatId == 1L || chatId == 7777777L) return true
        if (familyDao.getAll().any { 1000L + it.slotIndex == chatId }) return true
        // Real Telegram group/supergroup chat IDs are negative; positive group IDs are local Room groups.
        if (chatId > 0L && messageDao.getGroup(chatId) != null) return true
        return false
    }

    private suspend fun resolveTelegramChatId(chatId: Long): Long {
        if (chatId < 0L) return chatId
        return telegramClient.createPrivateChat(chatId)
    }

    private suspend fun deliverToTelegram(
        chatId: Long,
        text: String,
        driveLink: String?,
        textAlreadySent: Boolean,
        docAlreadySent: Boolean
    ): DeliveryOutcome {
        val resolvedChatId = runCatching { resolveTelegramChatId(chatId) }
            .getOrNull()
            ?.takeIf { it != 0L }
            ?: return DeliveryOutcome(textSent = textAlreadySent, docSent = docAlreadySent)

        val textOk = if (textAlreadySent || text.isBlank()) {
            true
        } else {
            runCatching { telegramClient.sendTextMessage(resolvedChatId, text) }
                .getOrNull() != null
        }

        val docOk = if (docAlreadySent || driveLink.isNullOrBlank()) {
            true
        } else {
            deliverAttachment(resolvedChatId, driveLink, text)
        }

        return DeliveryOutcome(textSent = textOk, docSent = docOk)
    }

    private suspend fun deliverAttachment(
        resolvedChatId: Long,
        driveLink: String,
        captionText: String
    ): Boolean {
        if (driveLink.startsWith("kunjo://file/")) {
            val fileId = driveLink.removePrefix("kunjo://file/")
            val targetFile = fileRepository.getFile(fileId)
                ?: recentDriveFiles.value.firstOrNull { it.id == fileId || driveLink.contains(it.id) }
            val remoteFileId = targetFile?.remoteFileId ?: return false
            return runCatching {
                telegramClient.copyDocument(
                    chatId = resolvedChatId,
                    remoteFileId = remoteFileId,
                    fileName = targetFile.name,
                    mimeType = targetFile.mimeType,
                    caption = captionText.ifBlank { targetFile.name }
                )
            }.isSuccess
        }

        val rawRef = driveLink.removePrefix("file://")
        val imported = fileImporter.import(rawRef) ?: return false
        return try {
            var completed = false
            runCatching {
                telegramClient.uploadDocument(
                    chatId = resolvedChatId,
                    localPath = imported.path,
                    fileName = imported.name,
                    mimeType = guessMimeType(imported.name),
                    caption = captionText.ifBlank { imported.name }
                ).collect { event ->
                    if (event is TelegramUploadEvent.Completed) {
                        completed = true
                    }
                }
            }
            completed
        } finally {
            fileImporter.discard(imported)
        }
    }

    private fun guessMimeType(fileName: String): String =
        when (fileName.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "pdf" -> "application/pdf"
            "apk" -> "application/vnd.android.package-archive"
            "zip" -> "application/zip"
            else -> "application/octet-stream"
        }

    private suspend fun enqueueRetry(item: RetryableMessage) {
        retryQueueMutex.withLock { pendingRetries.addLast(item) }
        retryTrigger.trySend(Unit)
    }

    /**
     * Drains the pending retry queue sequentially on a single worker so retries never overlap.
     */
    private suspend fun processRetryQueue() {
        while (true) {
            val retryable = retryQueueMutex.withLock { pendingRetries.removeFirstOrNull() } ?: return
            if (retryable.retryCount >= maxRetryAttempts) {
                messageDao.updateMessageStatus(retryable.messageId, "FAILED")
                continue
            }

            delay(retryBaseDelayMs * (retryable.retryCount + 1))

            val outcome = deliverToTelegram(
                chatId = retryable.chatId,
                text = retryable.text,
                driveLink = retryable.driveLink,
                textAlreadySent = retryable.textSent,
                docAlreadySent = retryable.docSent
            )

            if (outcome.allSucceeded) {
                messageDao.updateMessageStatus(retryable.messageId, "SENT")
            } else {
                val nextCount = retryable.retryCount + 1
                if (nextCount >= maxRetryAttempts) {
                    messageDao.updateMessageStatus(retryable.messageId, "FAILED")
                } else {
                    enqueueRetry(
                        retryable.copy(
                            textSent = outcome.textSent,
                            docSent = outcome.docSent,
                            retryCount = nextCount
                        )
                    )
                }
            }
        }
    }

    fun createGroup(title: String, username: String? = null, onCreated: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val prefs = preferences.value
            // STRICT SUB-ACCOUNT RESTRICTION 3: Sub-Account CANNOT create groups!
            if (prefs.isFamilyMember) {
                return@launch
            }
            val groupId = System.currentTimeMillis()
            val cleanUsername = username?.removePrefix("@")?.trim()?.takeIf { it.isNotBlank() }
            val group = IplusMessageGroupEntity(
                groupId = groupId,
                title = title,
                ownerId = 0L,
                memberCount = 1,
                inviteLink = "dewan://join/g_$groupId",
                groupUsername = cleanUsername
            )
            messageDao.upsertGroup(group)
            onCreated?.invoke(groupId)
        }
    }

    fun deleteGroup(groupId: Long) {
        viewModelScope.launch {
            messageDao.deleteMessagesForChat(groupId)
            messageDao.deleteGroup(groupId)
        }
    }

    fun updateGroupTitle(groupId: Long, title: String) {
        viewModelScope.launch {
            messageDao.updateGroupTitle(groupId, title)
        }
    }

    fun updateGroupUsername(groupId: Long, username: String?) {
        viewModelScope.launch {
            val clean = username?.removePrefix("@")?.trim()?.takeIf { it.isNotBlank() }
            messageDao.updateGroupUsername(groupId, clean)
        }
    }

    fun setGroupReadOnly(groupId: Long, isReadOnly: Boolean) {
        viewModelScope.launch {
            messageDao.updateGroupReadOnly(groupId, isReadOnly)
        }
    }

    fun setMemberAdminRole(groupId: Long, targetUserId: Long, makeAdmin: Boolean) {
        viewModelScope.launch {
            val group = messageDao.getGroup(groupId) ?: return@launch
            val currentAdmins = group.adminIds.split(",").filter { it.isNotBlank() }.toMutableSet()
            if (makeAdmin) {
                currentAdmins.add(targetUserId.toString())
            } else {
                currentAdmins.remove(targetUserId.toString())
            }
            messageDao.updateGroupAdmins(groupId, currentAdmins.joinToString(","))
        }
    }

    fun setMemberSendPermission(groupId: Long, targetUserId: Long, allowed: Boolean) {
        viewModelScope.launch {
            val group = messageDao.getGroup(groupId) ?: return@launch
            val currentAllowed = group.allowedSenderIds.split(",").filter { it.isNotBlank() }.toMutableSet()
            if (allowed) {
                currentAllowed.add(targetUserId.toString())
            } else {
                currentAllowed.remove(targetUserId.toString())
            }
            messageDao.updateGroupAllowedSenders(groupId, currentAllowed.joinToString(","))
        }
    }

    fun removeMemberWithRoleCheck(groupId: Long, requesterId: Long, targetUserId: Long): Boolean {
        viewModelScope.launch {
            val group = messageDao.getGroup(groupId) ?: return@launch
            val adminList = group.adminIds.split(",").filter { it.isNotBlank() }.toSet()
            val isTargetAdmin = adminList.contains(targetUserId.toString())
            val isRequesterOwner = requesterId == group.ownerId

            if (!isRequesterOwner && isTargetAdmin) {
                // Admin cannot remove other admins!
                return@launch
            }
            setMemberAdminRole(groupId, targetUserId, false)
            setMemberSendPermission(groupId, targetUserId, false)
        }
        return true
    }

    fun blockUser(userId: Long) {
        viewModelScope.launch {
            messageDao.setBlocked(userId, true)
        }
    }

    fun unblockUser(userId: Long) {
        viewModelScope.launch {
            messageDao.setBlocked(userId, false)
        }
    }

    fun markViewOnceViewed(messageId: Long) {
        viewModelScope.launch {
            messageDao.markViewOnceViewed(messageId)
        }
    }
}
