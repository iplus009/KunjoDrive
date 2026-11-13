package com.novo.drive.core.security

import com.novo.drive.data.repository.FakeSettings
import com.novo.drive.domain.model.UserPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The vault gate must reject wrong PINs, open the decoy with the decoy PIN only, lock out after
 * 5 failures, and never let the decoy PIN collide with the master PIN.
 */
class VaultSecurityManagerTest {

    private fun manager(prefs: UserPreferences = UserPreferences()) =
        VaultSecurityManager(FakeSettings(prefs))

    @Test
    fun `setup rejects PINs that are not exactly 4 digits`() = runBlocking {
        val m = manager()
        assertFalse(m.setupPin("123"))
        assertFalse(m.setupPin("12345"))
        assertFalse(m.setupPin("12a4"))
        assertFalse(m.setupPin(""))
        assertFalse(m.isPinConfigured())
    }

    @Test
    fun `correct master PIN succeeds and wrong PIN counts down without locking early`() = runBlocking {
        val m = manager()
        assertTrue(m.setupPin("2580"))
        assertTrue(m.isPinConfigured())

        assertEquals(PinVerificationResult.Success, m.verifyPin("2580"))
        assertEquals(PinVerificationResult.Incorrect(4), m.verifyPin("0000"))
        assertEquals(PinVerificationResult.Incorrect(3), m.verifyPin("1111"))
        assertFalse(m.isLockedOut())
    }

    @Test
    fun `decoy PIN opens decoy vault and never the real one`() = runBlocking {
        val m = manager()
        assertTrue(m.setupPin("2580"))
        assertTrue(m.setupDecoyPin("9753"))
        assertTrue(m.isDecoyPinConfigured())

        assertEquals(PinVerificationResult.DecoySuccess, m.verifyPin("9753"))
        assertEquals(PinVerificationResult.Success, m.verifyPin("2580"))
        // decoy PIN must not count as a failure against the master PIN
        assertFalse(m.isLockedOut())
    }

    @Test
    fun `decoy PIN cannot be set equal to the master PIN`() = runBlocking {
        val m = manager()
        assertTrue(m.setupPin("2580"))
        assertFalse(m.setupDecoyPin("2580"))
        assertFalse(m.isDecoyPinConfigured())
    }

    @Test
    fun `five wrong attempts lock the vault for 30 seconds`() = runBlocking {
        val m = manager()
        assertTrue(m.setupPin("2580"))
        repeat(4) { assertEquals(PinVerificationResult.Incorrect(4 - it), m.verifyPin("0000")) }
        assertTrue(m.verifyPin("0000") is PinVerificationResult.LockedOut)
        assertTrue(m.isLockedOut())
        val remaining = m.remainingLockoutSeconds()
        assertTrue("lockout should be ~30s, was $remaining", remaining in 25..31)

        // even the correct PIN is refused during lockout
        assertTrue(m.verifyPin("2580") is PinVerificationResult.LockedOut)

        m.resetLockout()
        assertFalse(m.isLockedOut())
        assertEquals(PinVerificationResult.Success, m.verifyPin("2580"))
    }

    @Test
    fun `setupDecoyPin requires an existing master PIN`() = runBlocking {
        val m = manager()
        assertFalse(m.setupDecoyPin("9753"))
        assertFalse(m.isDecoyPinConfigured())
    }

    @Test
    fun `changing the master PIN keeps the decoy PIN working`() = runBlocking {
        val m = manager()
        assertTrue(m.setupPin("2580"))
        assertTrue(m.setupDecoyPin("9753"))
        assertTrue(m.changePin("2580", "1357"))

        assertEquals(PinVerificationResult.Success, m.verifyPin("1357"))
        // the decoy has its own salt now, so it survives master-PIN changes
        assertTrue(m.isDecoyPinConfigured())
        assertEquals(PinVerificationResult.DecoySuccess, m.verifyPin("9753"))
    }
}
