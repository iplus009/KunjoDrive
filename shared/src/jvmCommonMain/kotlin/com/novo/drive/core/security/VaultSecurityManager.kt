package com.novo.drive.core.security

import com.novo.drive.core.crypto.PassphraseKdf
import com.novo.drive.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import java.util.Base64
import kotlin.math.max

sealed interface PinVerificationResult {
    data object Success : PinVerificationResult
    data object DecoySuccess : PinVerificationResult
    data class Incorrect(val remainingAttempts: Int) : PinVerificationResult
    data class LockedOut(val remainingSeconds: Int) : PinVerificationResult
}

class VaultSecurityManager(
    private val settingsRepository: SettingsRepository,
    private val passphraseKdf: PassphraseKdf = PassphraseKdf()
) {

    suspend fun isPinConfigured(): Boolean {
        val prefs = settingsRepository.preferences.first()
        return prefs.vaultPinHash.isNotBlank() && prefs.vaultPinSalt.isNotBlank()
    }

    suspend fun isDecoyPinConfigured(): Boolean {
        val prefs = settingsRepository.preferences.first()
        return prefs.decoyVaultPinHash.isNotBlank() &&
            (prefs.decoyVaultPinSalt.isNotBlank() || prefs.vaultPinSalt.isNotBlank())
    }

    suspend fun isLockedOut(): Boolean {
        val prefs = settingsRepository.preferences.first()
        val now = System.currentTimeMillis()
        return prefs.vaultLockoutUntilMs > now
    }

    suspend fun remainingLockoutSeconds(): Int {
        val prefs = settingsRepository.preferences.first()
        val now = System.currentTimeMillis()
        val diff = prefs.vaultLockoutUntilMs - now
        return if (diff > 0) (diff / 1000).toInt() + 1 else 0
    }

    suspend fun setupPin(pin: String): Boolean {
        if (pin.length != 4 || !pin.all { it.isDigit() }) return false
        val salt = passphraseKdf.newSalt()
        val hash = passphraseKdf.deriveKey(pin.toCharArray(), salt)
        val saltBase64 = Base64.getEncoder().encodeToString(salt)
        val hashBase64 = Base64.getEncoder().encodeToString(hash)

        settingsRepository.update {
            it.copy(
                vaultPinSalt = saltBase64,
                vaultPinHash = hashBase64,
                // A decoy PIN with its own salt survives a master-PIN change; a legacy decoy
                // (no dedicated salt) was derived from the old vault salt and is dropped.
                decoyVaultPinHash = if (it.decoyVaultPinSalt.isBlank()) "" else it.decoyVaultPinHash,
                decoyVaultPinSalt = if (it.decoyVaultPinSalt.isBlank()) "" else it.decoyVaultPinSalt,
                vaultFailedAttempts = 0,
                vaultLockoutUntilMs = 0L
            )
        }
        return true
    }

    suspend fun setupDecoyPin(decoyPin: String): Boolean {
        if (decoyPin.length != 4 || !decoyPin.all { it.isDigit() }) return false
        val prefs = settingsRepository.preferences.first()
        if (prefs.vaultPinSalt.isBlank() || prefs.vaultPinHash.isBlank()) return false
        val salt = runCatching { Base64.getDecoder().decode(prefs.vaultPinSalt) }.getOrNull()
            ?: return false
        val masterHash = runCatching { Base64.getDecoder().decode(prefs.vaultPinHash) }.getOrNull()
            ?: return false
        val decoyHash = passphraseKdf.deriveKey(decoyPin.toCharArray(), salt)
        // Decoy PIN must never collide with Master PIN
        if (NativeSecurityBridge.constantTimeEquals(decoyHash, masterHash)) {
            NativeSecurityBridge.wipeByteArray(decoyHash)
            return false
        }
        // A dedicated salt lets the decoy PIN survive master-PIN changes instead of dying with
        // the vault salt it used to share.
        val decoySalt = passphraseKdf.newSalt()
        val decoyHashWithOwnSalt = passphraseKdf.deriveKey(decoyPin.toCharArray(), decoySalt)
        val decoyBase64 = Base64.getEncoder().encodeToString(decoyHashWithOwnSalt)
        val decoySaltBase64 = Base64.getEncoder().encodeToString(decoySalt)
        NativeSecurityBridge.wipeByteArray(decoyHashWithOwnSalt)
        settingsRepository.update {
            it.copy(
                decoyVaultPinHash = decoyBase64,
                decoyVaultPinSalt = decoySaltBase64
            )
        }
        return true
    }

    suspend fun clearDecoyPin() {
        settingsRepository.update { it.copy(decoyVaultPinHash = "", decoyVaultPinSalt = "") }
    }

    suspend fun verifyPin(pin: String): PinVerificationResult {
        if (pin.length != 4) return PinVerificationResult.Incorrect(0)
        val prefs = settingsRepository.preferences.first()
        val now = System.currentTimeMillis()

        if (prefs.vaultLockoutUntilMs > now) {
            val remainingSec = ((prefs.vaultLockoutUntilMs - now) / 1000).toInt() + 1
            return PinVerificationResult.LockedOut(remainingSec)
        }

        if (prefs.vaultPinSalt.isBlank() || prefs.vaultPinHash.isBlank()) {
            return PinVerificationResult.Incorrect(0)
        }

        val salt = runCatching { Base64.getDecoder().decode(prefs.vaultPinSalt) }.getOrNull()
            ?: return PinVerificationResult.Incorrect(0)
        val expectedHash = runCatching { Base64.getDecoder().decode(prefs.vaultPinHash) }.getOrNull()
            ?: return PinVerificationResult.Incorrect(0)

        val computedHash = passphraseKdf.deriveKey(pin.toCharArray(), salt)
        val matchesMaster = NativeSecurityBridge.constantTimeEquals(computedHash, expectedHash)

        if (matchesMaster) {
            NativeSecurityBridge.wipeByteArray(computedHash)
            settingsRepository.update {
                it.copy(vaultFailedAttempts = 0, vaultLockoutUntilMs = 0L)
            }
            return PinVerificationResult.Success
        }

        if (prefs.decoyVaultPinHash.isNotBlank()) {
            val decoySalt = if (prefs.decoyVaultPinSalt.isNotBlank()) {
                runCatching { Base64.getDecoder().decode(prefs.decoyVaultPinSalt) }.getOrNull()
            } else {
                salt // legacy installs derived the decoy hash with the vault salt
            }
            val decoyHash = runCatching { Base64.getDecoder().decode(prefs.decoyVaultPinHash) }.getOrNull()
            val decoyComputed = decoySalt?.let { passphraseKdf.deriveKey(pin.toCharArray(), it) }
            if (decoyHash != null && decoyComputed != null &&
                NativeSecurityBridge.constantTimeEquals(decoyComputed, decoyHash)
            ) {
                NativeSecurityBridge.wipeByteArray(decoyComputed)
                settingsRepository.update {
                    it.copy(vaultFailedAttempts = 0, vaultLockoutUntilMs = 0L)
                }
                return PinVerificationResult.DecoySuccess
            }
            if (decoyComputed != null) NativeSecurityBridge.wipeByteArray(decoyComputed)
        }
        NativeSecurityBridge.wipeByteArray(computedHash)

        val newFailed = prefs.vaultFailedAttempts + 1
        val lockout = if (newFailed >= MAX_ATTEMPTS) now + LOCKOUT_DURATION_MS else 0L
        settingsRepository.update {
            it.copy(
                vaultFailedAttempts = newFailed,
                vaultLockoutUntilMs = lockout
            )
        }
        return if (lockout > 0L) {
            PinVerificationResult.LockedOut((LOCKOUT_DURATION_MS / 1000).toInt())
        } else {
            PinVerificationResult.Incorrect(max(0, MAX_ATTEMPTS - newFailed))
        }
    }

    suspend fun changePin(currentPin: String, newPin: String): Boolean {
        if (verifyPin(currentPin) !is PinVerificationResult.Success) return false
        return setupPin(newPin)
    }

    suspend fun resetLockout() {
        settingsRepository.update {
            it.copy(vaultFailedAttempts = 0, vaultLockoutUntilMs = 0L)
        }
    }

    companion object {
        const val MAX_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 30_000L
    }
}
