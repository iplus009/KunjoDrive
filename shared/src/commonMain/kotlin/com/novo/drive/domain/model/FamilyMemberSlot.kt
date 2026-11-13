package com.novo.drive.domain.model

data class FamilyMemberSlot(
    val slotIndex: Int,
    val memberName: String,
    val channelChatId: Long,
    val quotaBytes: Long,
    val usedBytes: Long,
    val expiresAtMs: Long,
    val isRevoked: Boolean,
    val createdAt: Long,
    val lastHeartbeatAt: Long
) {
    val isExpired: Boolean
        get() = expiresAtMs > 0L && System.currentTimeMillis() > expiresAtMs
}

