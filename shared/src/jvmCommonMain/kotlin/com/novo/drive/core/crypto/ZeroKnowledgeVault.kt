package com.novo.drive.core.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Zero-Knowledge cryptographic engine for Kunjo Drive v5.0.0.
 *
 * Implements dual-key separation via PBKDF2:
 * 1. Auth Hash: Sent to Supabase to verify user authentication without revealing password.
 * 2. Encryption Key: Stored ONLY in local device RAM to encrypt/decrypt Telegram sessions & keys.
 */
object ZeroKnowledgeVault {

    private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val KDF_ITERATIONS = 100_000
    private const val SALT_SIZE_BYTES = 32
    private const val DERIVED_KEY_BITS = 512 // 64 bytes: 32 for auth, 32 for AES-GCM
    private const val GCM_IV_SIZE_BYTES = 12
    private const val GCM_TAG_BITS = 128

    private val secureRandom = SecureRandom()

    data class DerivedKeys(
        val authHash: String,
        val encryptionKey: ByteArray,
        val saltBase64: String
    ) {
        fun wipe() {
            encryptionKey.fill(0)
        }
    }

    data class EncryptedPayload(
        val ciphertextBase64: String,
        val ivBase64: String
    )

    fun newSalt(): ByteArray = ByteArray(SALT_SIZE_BYTES).also(secureRandom::nextBytes)

    fun deriveKeys(password: CharArray, existingSaltBase64: String? = null): DerivedKeys {
        val salt = if (existingSaltBase64.isNullOrBlank()) {
            newSalt()
        } else {
            val decoded = runCatching {
                Base64.getDecoder().decode(existingSaltBase64.trim())
            }.getOrElse { cause ->
                throw IllegalArgumentException("Malformed Base64 encryption salt", cause)
            }
            require(decoded.isNotEmpty()) { "Decoded encryption salt must not be empty" }
            decoded
        }

        val spec = PBEKeySpec(password, salt, KDF_ITERATIONS, DERIVED_KEY_BITS)
        val rawDerived = try {
            SecretKeyFactory.getInstance(KDF_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }

        // Split 64 bytes into Key 1 (Auth) and Key 2 (Encryption)
        val authKeyBytes = rawDerived.copyOfRange(0, 32)
        val encryptionKeyBytes = rawDerived.copyOfRange(32, 64)
        rawDerived.fill(0)

        // Key 1: Hash with SHA-256 for public cloud storage
        val md = MessageDigest.getInstance("SHA-256")
        val authHashBytes = md.digest(authKeyBytes)
        authKeyBytes.fill(0)
        val authHash = authHashBytes.joinToString("") { "%02x".format(it) }

        return DerivedKeys(
            authHash = authHash,
            encryptionKey = encryptionKeyBytes,
            saltBase64 = Base64.getEncoder().encodeToString(salt)
        )
    }

    fun encrypt(plaintext: String, key: ByteArray): EncryptedPayload {
        val iv = ByteArray(GCM_IV_SIZE_BYTES).also(secureRandom::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_BITS, iv)

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        return EncryptedPayload(
            ciphertextBase64 = Base64.getEncoder().encodeToString(ciphertext),
            ivBase64 = Base64.getEncoder().encodeToString(iv)
        )
    }

    fun decrypt(ciphertextBase64: String, ivBase64: String, key: ByteArray): String {
        val ciphertext = Base64.getDecoder().decode(ciphertextBase64)
        val iv = Base64.getDecoder().decode(ivBase64)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_BITS, iv)

        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        val plaintextBytes = cipher.doFinal(ciphertext)
        return String(plaintextBytes, Charsets.UTF_8)
    }

    private val RECOVERY_WORDS = listOf(
        "anchor", "bridge", "cloud", "desert", "echo", "flame", "galaxy", "harbor",
        "island", "jungle", "kunjo", "lotus", "meadow", "nebula", "ocean", "pioneer",
        "quartz", "river", "sunburst", "timber", "ultra", "velvet", "whisper", "zenith"
    )

    fun generateRecoveryPhrase(): List<String> {
        val words = mutableListOf<String>()
        val pool = RECOVERY_WORDS.shuffled(secureRandom)
        for (i in 0 until 12) {
            words.add(pool[i % pool.size])
        }
        return words
    }
}

