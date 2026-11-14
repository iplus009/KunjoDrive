package com.novo.drive.core.telegram

data class IncomingTelegramMessage(
    val messageId: Long,
    val chatId: Long,
    val senderId: Long,
    val senderName: String? = null,
    val senderUsername: String? = null,
    val chatTitle: String? = null,
    val isGroup: Boolean = false,
    val text: String? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val fileSizeBytes: Long = 0L,
    val remoteFileId: String? = null,
    val timestampMs: Long
)

data class TelegramGroupInfo(
    val chatId: Long,
    val supergroupId: Long = 0L,
    val title: String,
    val username: String? = null,
    val inviteLink: String? = null,
    val memberCount: Int = 1,
    val ownerUserId: Long = 0L
)

data class TelegramGroupMember(
    val userId: Long,
    val displayName: String,
    val username: String?,
    val role: String // "OWNER", "ADMIN", "MEMBER"
)
