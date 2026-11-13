package com.novo.drive.core.family

import com.novo.drive.core.common.AppError
import com.novo.drive.core.common.AppResult
import com.novo.drive.core.security.NativeSecurityBridge
import com.novo.drive.core.telegram.TelegramCredentials
import com.novo.drive.data.local.dao.FamilyDao
import com.novo.drive.data.local.entity.FamilyMemberEntity
import com.novo.drive.domain.model.AccountRole
import com.novo.drive.domain.model.FamilyMemberSlot
import com.novo.drive.domain.repository.ChannelRepository
import com.novo.drive.domain.repository.SettingsRepository
import com.novo.drive.domain.repository.TelegramAuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@Serializable
data class NovoFamilyGrantPayload(
    val version: Int = 5,
    val slotIndex: Int,
    val memberName: String,
    val apiId: Int,
    val apiHash: String,
    val channelChatId: Long,
    val quotaBytes: Long,
    val maxFileBytes: Long,
    val expiresAtMs: Long,
    val subKeyBase64: String,
    val grantSignature: String,
    val issuedAtMs: Long
)

data class GeneratedFamilyInvite(
    val slotIndex: Int,
    val memberName: String,
    val inviteToken: String,
    val pairingPin: String,
    val expiresInSeconds: Int = 300
)

class FamilyAccessManager(
    private val familyDao: FamilyDao,
    private val settingsRepository: SettingsRepository,
    private val channelRepository: ChannelRepository,
    private val telegramAuthRepository: TelegramAuthRepository
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val secureRandom = SecureRandom()

    fun observeSlots(): Flow<List<FamilyMemberSlot>> =
        familyDao.observeAll().map { entities ->
            entities.map { entity ->
                FamilyMemberSlot(
                    slotIndex = entity.slotIndex,
                    memberName = entity.memberName,
                    channelChatId = entity.channelChatId,
                    quotaBytes = entity.quotaBytes,
                    usedBytes = entity.usedBytes,
                    expiresAtMs = entity.expiresAtMs,
                    isRevoked = entity.isRevoked,
                    createdAt = entity.createdAt,
                    lastHeartbeatAt = entity.lastHeartbeatAt
                )
            }
        }

    suspend fun createSlotInvite(
        slotIndex: Int,
        memberName: String,
        quotaGb: Int,
        expiryDays: Int
    ): AppResult<GeneratedFamilyInvite> {
        val maxSlots = NativeSecurityBridge.maxFamilyMembers()
        if (slotIndex !in 1..maxSlots) {
            return AppResult.Failure(
                AppError.Unknown("Family Cloud strictly allows up to $maxSlots members (Slots 1..$maxSlots).")
            )
        }
        val cleanName = memberName.trim().ifBlank { "Family Member #$slotIndex" }
        val prefs = settingsRepository.preferences.first()
        if (prefs.accountRole != AccountRole.OWNER) {
            return AppResult.Failure(AppError.Unknown("Only the Cloud Owner can invite family members."))
        }
        val credentials = settingsRepository.getTelegramCredentials()
            ?: return AppResult.Failure(AppError.InvalidApiCredentials)

        val existing = familyDao.bySlot(slotIndex)
        val activeOtherSlots = familyDao.getAll().count { !it.isRevoked && it.slotIndex != slotIndex }
        if (activeOtherSlots >= maxSlots) {
            return AppResult.Failure(
                AppError.Unknown("Maximum $maxSlots family member slots are already active.")
            )
        }

        val channelChatId = if (existing != null && !existing.isRevoked && existing.channelChatId != 0L) {
            existing.channelChatId
        } else {
            when (val created = channelRepository.create("Family • $cleanName (#$slotIndex)")) {
                is AppResult.Success -> created.value.chatId
                is AppResult.Failure -> return created
            }
        }

        val now = System.currentTimeMillis()
        val quotaBytes = quotaGb.coerceIn(1, 500).toLong() * 1024L * 1024L * 1024L
        val expiresAtMs = if (expiryDays <= 0) 0L else now + expiryDays.toLong() * 24L * 3600L * 1000L
        val maxFileBytes = NativeSecurityBridge.maxFamilyFileBytes()

        val ownerSeed = "${credentials.apiId}:${credentials.apiHash.take(16)}:$now"
        val subKeyBytes = NativeSecurityBridge.deriveFamilySubKey(ownerSeed, slotIndex, channelChatId)
        if (subKeyBytes.isEmpty()) {
            return AppResult.Failure(AppError.Unknown("Native security engine rejected sub-key derivation."))
        }
        val subKeyBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(subKeyBytes)
        val canonical = FamilyPolicyGuard.canonicalGrantPayload(
            slotIndex = slotIndex,
            memberName = cleanName,
            channelChatId = channelChatId,
            quotaBytes = quotaBytes,
            maxFileBytes = maxFileBytes,
            expiresAtMs = expiresAtMs,
            subKeyBase64 = subKeyBase64
        )
        val signature = NativeSecurityBridge.signFamilyGrant(canonical)
        if (signature.isBlank()) {
            return AppResult.Failure(AppError.Unknown("Native security shield rejected grant signing."))
        }

        val payload = NovoFamilyGrantPayload(
            slotIndex = slotIndex,
            memberName = cleanName,
            apiId = credentials.apiId,
            apiHash = credentials.apiHash,
            channelChatId = channelChatId,
            quotaBytes = quotaBytes,
            maxFileBytes = maxFileBytes,
            expiresAtMs = expiresAtMs,
            subKeyBase64 = subKeyBase64,
            grantSignature = signature,
            issuedAtMs = now
        )

        val pairingPin = "%06d".format(secureRandom.nextInt(1_000_000))
        val encryptedInvite = encryptInvitePayload(json.encodeToString(payload), pairingPin)

        familyDao.upsert(
            FamilyMemberEntity(
                slotIndex = slotIndex,
                memberName = cleanName,
                channelChatId = channelChatId,
                quotaBytes = quotaBytes,
                usedBytes = existing?.usedBytes ?: 0L,
                subKeyBase64 = subKeyBase64,
                grantSignature = signature,
                expiresAtMs = expiresAtMs,
                isRevoked = false,
                createdAt = existing?.createdAt ?: now,
                lastHeartbeatAt = now
            )
        )

        return AppResult.Success(
            GeneratedFamilyInvite(
                slotIndex = slotIndex,
                memberName = cleanName,
                inviteToken = "$INVITE_PREFIX$encryptedInvite",
                pairingPin = pairingPin
            )
        )
    }

    suspend fun acceptFamilyInvite(
        rawToken: String,
        pairingPin: String
    ): AppResult<NovoFamilyGrantPayload> {
        val cleaned = rawToken.trim().substringBefore("|PIN:")
        val body = when {
            cleaned.startsWith(INVITE_PREFIX) -> cleaned.removePrefix(INVITE_PREFIX)
            cleaned.startsWith(LEGACY_INVITE_PREFIX) -> cleaned.removePrefix(LEGACY_INVITE_PREFIX)
            else -> return AppResult.Failure(AppError.Unknown("Invalid Kunjo Family Invite QR or token."))
        }
        val pin = pairingPin.trim()
        if (pin.length != 6 || !pin.all { it.isDigit() }) {
            return AppResult.Failure(AppError.Unknown("Enter the 6-digit pairing PIN from the Owner's screen."))
        }
        val decryptedJson = runCatching { decryptInvitePayload(body, pin) }.getOrNull()
            ?: return AppResult.Failure(AppError.Unknown("Incorrect pairing PIN or corrupted Family Invite."))

        val payload = runCatching { json.decodeFromString<NovoFamilyGrantPayload>(decryptedJson) }.getOrNull()
            ?: return AppResult.Failure(AppError.Unknown("Invalid Family Invite format."))

        if (payload.slotIndex !in 1..NativeSecurityBridge.maxFamilyMembers()) {
            return AppResult.Failure(AppError.Unknown("Family slot exceeds 3-member protocol cap."))
        }
        val canonical = FamilyPolicyGuard.canonicalGrantPayload(
            slotIndex = payload.slotIndex,
            memberName = payload.memberName,
            channelChatId = payload.channelChatId,
            quotaBytes = payload.quotaBytes,
            maxFileBytes = NativeSecurityBridge.maxFamilyFileBytes(),
            expiresAtMs = payload.expiresAtMs,
            subKeyBase64 = payload.subKeyBase64
        )
        if (!NativeSecurityBridge.verifyFamilyGrant(canonical, payload.grantSignature)) {
            return AppResult.Failure(AppError.Unknown("Cryptographic signature verification failed."))
        }
        if (payload.expiresAtMs > 0L && System.currentTimeMillis() > payload.expiresAtMs) {
            return AppResult.Failure(AppError.Unknown("This Family Invite has already expired."))
        }

        // Persist scoped Family policy BEFORE starting TDLib QR handshake
        settingsRepository.update {
            it.copy(
                accountRole = AccountRole.FAMILY_MEMBER,
                storageChatId = payload.channelChatId,
                familySlotIndex = payload.slotIndex,
                familyMemberName = payload.memberName,
                familyQuotaBytes = payload.quotaBytes,
                familyExpiresAtMs = payload.expiresAtMs,
                familyGrantSignature = payload.grantSignature,
                familySubKeyBase64 = payload.subKeyBase64,
                transferConcurrency = 1
            )
        }

        when (val configured = telegramAuthRepository.configure(
            TelegramCredentials(payload.apiId, payload.apiHash)
        )) {
            is AppResult.Failure -> return configured
            is AppResult.Success -> Unit
        }

        return AppResult.Success(payload)
    }

    suspend fun approveMemberDeviceQr(tgLoginLink: String): AppResult<Unit> {
        val link = tgLoginLink.trim()
        if (!link.startsWith("tg://login?token=")) {
            return AppResult.Failure(
                AppError.Unknown("Scan or paste the Step-2 Session QR ('tg://login?token=...') shown on the family member's Novo Drive.")
            )
        }
        return telegramAuthRepository.confirmQrCodeAuthentication(link)
    }

    suspend fun revokeSlot(slotIndex: Int, deleteRemoteChannel: Boolean = false): AppResult<Unit> {
        val member = familyDao.bySlot(slotIndex)
            ?: return AppResult.Failure(AppError.NotFound)
        val now = System.currentTimeMillis()
        familyDao.setRevoked(slotIndex, revoked = true, updatedAt = now)
        if (deleteRemoteChannel && member.channelChatId != 0L) {
            channelRepository.deleteRemotely(member.channelChatId)
            familyDao.deleteSlot(slotIndex)
        }
        return AppResult.Success(Unit)
    }

    private fun encryptInvitePayload(plainJson: String, pin: String): String {
        val salt = ByteArray(12).also(secureRandom::nextBytes)
        val iv = ByteArray(12).also(secureRandom::nextBytes)
        val key = derivePinKey(pin, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val ct = cipher.doFinal(plainJson.encodeToByteArray())
        val packed = ByteArray(salt.size + iv.size + ct.size)
        System.arraycopy(salt, 0, packed, 0, salt.size)
        System.arraycopy(iv, 0, packed, salt.size, iv.size)
        System.arraycopy(ct, 0, packed, salt.size + iv.size, ct.size)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(packed)
    }

    private fun decryptInvitePayload(encoded: String, pin: String): String {
        val packed = Base64.getUrlDecoder().decode(encoded)
        require(packed.size > 24) { "Invite payload too short" }
        val salt = packed.copyOfRange(0, 12)
        val iv = packed.copyOfRange(12, 24)
        val ct = packed.copyOfRange(24, packed.size)
        val key = derivePinKey(pin, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(ct).decodeToString()
    }

    private fun derivePinKey(pin: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 32_768, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(bytes, "AES")
    }

    companion object {
        const val INVITE_PREFIX = "KUNJO-FAM-V2:"
        const val LEGACY_INVITE_PREFIX = "NOVO-FAM-V5:"
    }
}

