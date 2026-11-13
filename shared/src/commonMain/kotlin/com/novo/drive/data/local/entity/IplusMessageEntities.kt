package com.novo.drive.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "iplus_message_contacts")
data class IplusMessageContactEntity(
    @PrimaryKey val userId: Long,
    val username: String,
    val displayName: String,
    val avatarType: String = "EMOJI",
    val avatarValue: String = "👤",
    val isBlocked: Boolean = false,
    val lastSeenTimestamp: Long = 0L
)

@Entity(tableName = "iplus_message_messages")
data class IplusMessageEntity(
    @PrimaryKey val messageId: Long,
    val chatId: Long,
    val senderId: Long,
    val text: String?,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val status: String = "SENT",
    val replyToMessageId: Long? = null,
    val driveLink: String? = null,
    val isViewOnce: Boolean = false,
    val isViewed: Boolean = false
)

@Entity(tableName = "iplus_message_groups")
data class IplusMessageGroupEntity(
    @PrimaryKey val groupId: Long,
    val title: String,
    val ownerId: Long,
    val memberCount: Int = 1,
    val inviteLink: String? = null,
    val avatarUrl: String? = null,
    val isReadOnly: Boolean = false,
    val adminIds: String = "",
    val allowedSenderIds: String = "",
    val groupUsername: String? = null
)

