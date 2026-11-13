package com.novo.drive.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey val slotIndex: Int,
    val memberName: String,
    val channelChatId: Long,
    val quotaBytes: Long,
    val usedBytes: Long,
    val subKeyBase64: String,
    val grantSignature: String,
    val expiresAtMs: Long,
    val isRevoked: Boolean = false,
    val createdAt: Long,
    val lastHeartbeatAt: Long
)

