package com.novo.drive.core.security

import com.novo.drive.core.crypto.ZeroKnowledgeVault
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.AEADBadTagException

class ZeroKnowledgeVaultTest {

    @Test
    fun testDualKeyDerivationAndEncryptionRoundTrip() {
        val password = "MyUltraSecretPassword123!".toCharArray()
        val keys1 = ZeroKnowledgeVault.deriveKeys(password)

        // Deriving with the same salt should yield identical authHash and key
        val keys2 = ZeroKnowledgeVault.deriveKeys(password, keys1.saltBase64)
        assertEquals(keys1.authHash, keys2.authHash)
        assertTrue(keys1.encryptionKey.contentEquals(keys2.encryptionKey))

        // Different salt yields different authHash
        val keys3 = ZeroKnowledgeVault.deriveKeys(password)
        assertNotEquals(keys1.authHash, keys3.authHash)

        // Encrypt & Decrypt test
        val secretSession = "tdlib_session_string_abc_xyz_1234567890"
        val payload = ZeroKnowledgeVault.encrypt(secretSession, keys1.encryptionKey)
        val decrypted = ZeroKnowledgeVault.decrypt(payload.ciphertextBase64, payload.ivBase64, keys1.encryptionKey)

        assertEquals(secretSession, decrypted)
    }

    @Test(expected = AEADBadTagException::class)
    fun testDecryptFailsWithWrongKey() {
        val keysA = ZeroKnowledgeVault.deriveKeys("PasswordA".toCharArray())
        val keysB = ZeroKnowledgeVault.deriveKeys("PasswordB".toCharArray())

        val secretSession = "my_classified_token"
        val payload = ZeroKnowledgeVault.encrypt(secretSession, keysA.encryptionKey)

        // Attempting to decrypt with keysB must fail with AEADBadTagException
        ZeroKnowledgeVault.decrypt(payload.ciphertextBase64, payload.ivBase64, keysB.encryptionKey)
    }

    @Test
    fun testRecoveryPhraseGeneration() {
        val phrase = ZeroKnowledgeVault.generateRecoveryPhrase()
        assertEquals(12, phrase.size)
        phrase.forEach { word ->
            assertTrue(word.isNotBlank())
        }
    }
}

