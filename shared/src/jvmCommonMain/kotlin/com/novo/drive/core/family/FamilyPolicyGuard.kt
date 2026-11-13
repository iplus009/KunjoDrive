package com.novo.drive.core.family

import com.novo.drive.core.security.NativeSecurityBridge
import com.novo.drive.domain.model.AccountRole
import com.novo.drive.domain.model.UserPreferences

sealed interface FamilyUploadCheck {
    data object Allowed : FamilyUploadCheck
    data class Denied(val reason: String) : FamilyUploadCheck
}

object FamilyPolicyGuard {

    val MAX_MEMBERS: Int
        get() = NativeSecurityBridge.maxFamilyMembers()

    val MAX_FILE_BYTES: Long
        get() = NativeSecurityBridge.maxFamilyFileBytes()

    fun canonicalGrantPayload(
        slotIndex: Int,
        memberName: String,
        channelChatId: Long,
        quotaBytes: Long,
        maxFileBytes: Long,
        expiresAtMs: Long,
        subKeyBase64: String
    ): String =
        "v5|$slotIndex|$memberName|$channelChatId|$quotaBytes|$maxFileBytes|$expiresAtMs|$subKeyBase64"

    fun verifyActiveFamilyGrant(prefs: UserPreferences): Boolean {
        if (prefs.accountRole != AccountRole.FAMILY_MEMBER) return true
        val chatId = prefs.storageChatId ?: return false
        if (prefs.familySlotIndex !in 1..MAX_MEMBERS) return false
        if (prefs.familyGrantSignature.isBlank() || prefs.familySubKeyBase64.isBlank()) return false
        val canonical = canonicalGrantPayload(
            slotIndex = prefs.familySlotIndex,
            memberName = prefs.familyMemberName,
            channelChatId = chatId,
            quotaBytes = prefs.familyQuotaBytes,
            maxFileBytes = MAX_FILE_BYTES,
            expiresAtMs = prefs.familyExpiresAtMs,
            subKeyBase64 = prefs.familySubKeyBase64
        )
        return NativeSecurityBridge.verifyFamilyGrant(canonical, prefs.familyGrantSignature)
    }

    fun validateUpload(
        prefs: UserPreferences,
        uploadSizeBytes: Long,
        currentUsedBytes: Long,
        nowMs: Long = System.currentTimeMillis()
    ): FamilyUploadCheck {
        if (prefs.accountRole != AccountRole.FAMILY_MEMBER) {
            return FamilyUploadCheck.Allowed
        }
        if (!NativeSecurityBridge.isRuntimeIntegrityValid()) {
            return FamilyUploadCheck.Denied("App signature integrity verification failed.")
        }
        if (!verifyActiveFamilyGrant(prefs)) {
            return FamilyUploadCheck.Denied("Invalid or revoked Family Cloud access token.")
        }
        if (prefs.familyExpiresAtMs > 0L && nowMs > prefs.familyExpiresAtMs) {
            return FamilyUploadCheck.Denied("Your Family Cloud share window has expired.")
        }
        if (uploadSizeBytes > MAX_FILE_BYTES) {
            return FamilyUploadCheck.Denied(
                "Family Cloud members can upload up to 2 GB per file."
            )
        }
        if (prefs.familyQuotaBytes > 0L &&
            (currentUsedBytes >= prefs.familyQuotaBytes ||
                uploadSizeBytes > prefs.familyQuotaBytes - currentUsedBytes)
        ) {
            return FamilyUploadCheck.Denied(
                "Family Cloud storage quota reached. Free up space or ask the Owner to raise your quota."
            )
        }
        return FamilyUploadCheck.Allowed
    }
}
