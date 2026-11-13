package com.novo.drive.core.security

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

interface NativeSecurityDelegate {
    fun maxFamilyMembers(): Int
    fun maxFamilyFileBytes(): Long
    fun signFamilyGrant(canonicalPayload: String): String
    fun verifyFamilyGrant(canonicalPayload: String, signatureHex: String): Boolean
    fun deriveFamilySubKey(ownerSeed: String, slotIndex: Int, channelChatId: Long): ByteArray
    fun isRuntimeIntegrityValid(): Boolean
    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean
    fun wipeByteArray(target: ByteArray)
}

/**
 * Bridges native C++ security enforcement (`libnovo_core.so` on Android) with a byte-identical
 * JVM HMAC-SHA256 fallback for Desktop and JVM unit tests.
 */
object NativeSecurityBridge {

    @Volatile
    private var delegate: NativeSecurityDelegate = JvmFallbackSecurityDelegate

    val defaultFallback: NativeSecurityDelegate
        get() = JvmFallbackSecurityDelegate

    fun installDelegate(customDelegate: NativeSecurityDelegate) {
        delegate = customDelegate
    }

    fun maxFamilyMembers(): Int = delegate.maxFamilyMembers()

    fun maxFamilyFileBytes(): Long = delegate.maxFamilyFileBytes()

    fun signFamilyGrant(canonicalPayload: String): String =
        delegate.signFamilyGrant(canonicalPayload)

    fun verifyFamilyGrant(canonicalPayload: String, signatureHex: String): Boolean =
        delegate.verifyFamilyGrant(canonicalPayload, signatureHex)

    fun deriveFamilySubKey(ownerSeed: String, slotIndex: Int, channelChatId: Long): ByteArray =
        delegate.deriveFamilySubKey(ownerSeed, slotIndex, channelChatId)

    fun isRuntimeIntegrityValid(): Boolean = delegate.isRuntimeIntegrityValid()

    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean =
        delegate.constantTimeEquals(a, b)

    fun wipeByteArray(target: ByteArray) {
        delegate.wipeByteArray(target)
    }
}

internal object JvmFallbackSecurityDelegate : NativeSecurityDelegate {

    private val MASKED_SHIELD_KEY = byteArrayOf(
        0xE9.toByte(), 0xC5.toByte(), 0xCB.toByte(), 0xEF.toByte(),
        0xD7.toByte(), 0x94.toByte(), 0x80.toByte(), 0x8A.toByte(),
        0xAA.toByte(), 0xE8.toByte(), 0x1F.toByte(), 0x79.toByte(),
        0x4E.toByte(), 0x6F.toByte(), 0x7F.toByte(), 0x10.toByte(),
        0x02.toByte(), 0x17.toByte(), 0x1E.toByte(), 0x38.toByte(),
        0xCA.toByte(), 0xD3.toByte(), 0xD5.toByte(), 0xE8.toByte(),
        0xA5.toByte(), 0xD8.toByte(), 0x83.toByte(), 0xCD.toByte(),
        0xE5.toByte(), 0xEE.toByte(), 0x00.toByte(), 0x15.toByte()
    )

    private fun unmaskKey(): ByteArray {
        val out = ByteArray(MASKED_SHIELD_KEY.size)
        for (i in MASKED_SHIELD_KEY.indices) {
            val mask = (0xA7 xor ((i * 13) and 0xFF)).toByte()
            out[i] = (MASKED_SHIELD_KEY[i].toInt() xor mask.toInt()).toByte()
        }
        return out
    }

    private fun hmacSha256(message: ByteArray): ByteArray {
        val key = unmaskKey()
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(key, "HmacSHA256"))
            mac.doFinal(message)
        } finally {
            key.fill(0)
        }
    }

    override fun maxFamilyMembers(): Int = 3

    override fun maxFamilyFileBytes(): Long = 4000L * 512L * 1024L

    override fun signFamilyGrant(canonicalPayload: String): String {
        val digest = hmacSha256(canonicalPayload.encodeToByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    override fun verifyFamilyGrant(canonicalPayload: String, signatureHex: String): Boolean {
        if (signatureHex.length != 64) return false
        val expected = signFamilyGrant(canonicalPayload).lowercase()
        val provided = signatureHex.lowercase()
        return MessageDigest.isEqual(
            expected.encodeToByteArray(),
            provided.encodeToByteArray()
        )
    }

    override fun deriveFamilySubKey(
        ownerSeed: String,
        slotIndex: Int,
        channelChatId: Long
    ): ByteArray {
        if (slotIndex !in 1..3) return ByteArray(0)
        val info = "novo-family-subkey|$ownerSeed|slot=$slotIndex|chat=$channelChatId"
        return hmacSha256(info.encodeToByteArray())
    }

    override fun isRuntimeIntegrityValid(): Boolean = true

    override fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) {
            diff = diff or (a[i].toInt() xor b[i].toInt())
        }
        return diff == 0
    }

    override fun wipeByteArray(target: ByteArray) {
        target.fill(0)
    }
}

