package com.novo.drive.core.security

import com.novo.drive.core.family.FamilyPolicyGuard
import com.novo.drive.core.family.FamilyUploadCheck
import com.novo.drive.domain.model.AccountRole
import com.novo.drive.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KunjoV5LogicAndMathTest {

    @Test
    fun `family subkey and grant signature enforce 3-slot and tamper resistance`() {
        assertEquals(0, NativeSecurityBridge.deriveFamilySubKey("seed", 0, -100123L).size)
        assertEquals(0, NativeSecurityBridge.deriveFamilySubKey("seed", 4, -100123L).size)

        val slot1Key = NativeSecurityBridge.deriveFamilySubKey("seed", 1, -100123L)
        val slot2Key = NativeSecurityBridge.deriveFamilySubKey("seed", 2, -100123L)
        assertEquals(32, slot1Key.size)
        assertEquals(32, slot2Key.size)
        assertFalse(slot1Key.contentEquals(slot2Key))

        val payload = FamilyPolicyGuard.canonicalGrantPayload(
            slotIndex = 1,
            memberName = "MemberOne",
            channelChatId = -100123456789L,
            quotaBytes = 10L * 1024L * 1024L * 1024L,
            maxFileBytes = FamilyPolicyGuard.MAX_FILE_BYTES,
            expiresAtMs = 0L,
            subKeyBase64 = "subKeyBase64Value"
        )
        val sig = NativeSecurityBridge.signFamilyGrant(payload)
        assertTrue(NativeSecurityBridge.verifyFamilyGrant(payload, sig))

        val tamperedPayload = FamilyPolicyGuard.canonicalGrantPayload(
            slotIndex = 2,
            memberName = "MemberOne",
            channelChatId = -100123456789L,
            quotaBytes = 10L * 1024L * 1024L * 1024L,
            maxFileBytes = FamilyPolicyGuard.MAX_FILE_BYTES,
            expiresAtMs = 0L,
            subKeyBase64 = "subKeyBase64Value"
        )
        assertFalse(NativeSecurityBridge.verifyFamilyGrant(tamperedPayload, sig))
    }

    @Test
    fun `family policy guard enforces 2GB per-file cap and total quota math`() {
        val maxFile = FamilyPolicyGuard.MAX_FILE_BYTES
        val quota10Gb = 10L * 1024L * 1024L * 1024L
        val payload = FamilyPolicyGuard.canonicalGrantPayload(
            slotIndex = 1,
            memberName = "member-1",
            channelChatId = -100999L,
            quotaBytes = quota10Gb,
            maxFileBytes = maxFile,
            expiresAtMs = 0L,
            subKeyBase64 = "subKey123"
        )
        val validSig = NativeSecurityBridge.signFamilyGrant(payload)

        val familyPrefs = UserPreferences(
            accountRole = AccountRole.FAMILY_MEMBER,
            storageChatId = -100999L,
            familySlotIndex = 1,
            familyMemberName = "member-1",
            familyQuotaBytes = quota10Gb,
            familyExpiresAtMs = 0L,
            familyGrantSignature = validSig,
            familySubKeyBase64 = "subKey123"
        )

        // 1 GB file within 10 GB quota -> Allowed
        val allowed = FamilyPolicyGuard.validateUpload(
            prefs = familyPrefs,
            uploadSizeBytes = 1024L * 1024L * 1024L,
            currentUsedBytes = 2L * 1024L * 1024L * 1024L
        )
        assertTrue(allowed is FamilyUploadCheck.Allowed)

        // Near Long.MAX_VALUE, addition would overflow negative and incorrectly allow upload.
        val overflowAttempt = FamilyPolicyGuard.validateUpload(
            prefs = familyPrefs,
            uploadSizeBytes = Long.MAX_VALUE,
            currentUsedBytes = Long.MAX_VALUE - 1L
        )
        assertTrue(overflowAttempt is FamilyUploadCheck.Denied)

        // Over max file cap -> Denied
        val deniedOverMax = FamilyPolicyGuard.validateUpload(
            prefs = familyPrefs,
            uploadSizeBytes = maxFile + 1024L,
            currentUsedBytes = 0L
        )
        assertTrue(deniedOverMax is FamilyUploadCheck.Denied)

        // Exceeds 10 GB total quota -> Denied
        val deniedQuota = FamilyPolicyGuard.validateUpload(
            prefs = familyPrefs,
            uploadSizeBytes = 1024L * 1024L * 1024L,
            currentUsedBytes = quota10Gb - 100L
        )
        assertTrue(deniedQuota is FamilyUploadCheck.Denied)
    }

    @Test
    fun `fail-closed integrity delegate properly blocks uploads when tampered`() {
        val originalDelegate = NativeSecurityBridge.defaultFallback
        try {
            NativeSecurityBridge.installDelegate(
                object : NativeSecurityDelegate by originalDelegate {
                    override fun isRuntimeIntegrityValid(): Boolean = false
                }
            )
            val prefs = UserPreferences(accountRole = AccountRole.FAMILY_MEMBER)
            val check = FamilyPolicyGuard.validateUpload(prefs, 1024L, 0L)
            assertTrue(check is FamilyUploadCheck.Denied)
            assertEquals("App signature integrity verification failed.", (check as FamilyUploadCheck.Denied).reason)
        } finally {
            NativeSecurityBridge.installDelegate(originalDelegate)
        }
    }
}
