package com.novo.drive.core.security

import com.novo.drive.core.bridge.KunjoBridgeImpl
import com.novo.drive.core.bridge.SecurityBridge
import com.novo.drive.core.common.bi
import com.novo.drive.core.network.KunjoUserDto
import com.novo.drive.core.network.SupabaseRegisterOutcome
import com.novo.drive.core.network.SupabaseRestClient
import com.novo.drive.data.repository.KunjoAuthRepository
import com.novo.drive.data.repository.KunjoAuthResult
import com.novo.drive.domain.model.AppLanguage
import com.novo.drive.domain.model.UserPreferences
import com.novo.drive.domain.repository.SettingsRepository
import com.novo.drive.testing.unused
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class KunjoAuthAndBridgeTest {

    private val bengaliRegex = Regex("[\\u0980-\\u09FF]")

    private fun fakeSettings(): Pair<SettingsRepository, MutableStateFlow<UserPreferences>> {
        val state = MutableStateFlow(UserPreferences())
        val repo = object : SettingsRepository by unused() {
            override val preferences: Flow<UserPreferences> = state
            override suspend fun update(transform: (UserPreferences) -> UserPreferences) {
                state.value = transform(state.value)
            }
        }
        return repo to state
    }

    @Test
    fun `default KunjoBridgeImpl security bridge fails closed on vault key check`() {
        val bridge = KunjoBridgeImpl()
        assertFalse(bridge.security.verifyVaultKey("1234"))
        assertFalse(bridge.security.verifyVaultKey("any-pin"))
    }

    @Test
    fun `KunjoBridgeImpl delegates to injected SecurityBridge`() {
        val customSecurity = object : SecurityBridge {
            override val isVaultLocked: StateFlow<Boolean> = MutableStateFlow(true)
            override val isGhostVaultActive: StateFlow<Boolean> = MutableStateFlow(false)
            override fun verifyVaultKey(key: String): Boolean = key == "998877"
            override fun isAppTampered(): Boolean = false
        }
        val bridge = KunjoBridgeImpl(customSecurityBridge = customSecurity)
        assertTrue(bridge.security.verifyVaultKey("998877"))
        assertFalse(bridge.security.verifyVaultKey("000000"))
    }

    @Test
    fun `resetPasswordVerified rejects null or mismatched Telegram user ID`() = runBlocking {
        val (settings, _) = fakeSettings()
        val repo = KunjoAuthRepository(settings)
        val validSalt = Base64.getEncoder().encodeToString("1234567890abcdef".toByteArray())
        val storedDto = KunjoUserDto(
            username = "alice",
            auth_hash = "old-hash",
            encrypted_session_blob = "old-blob",
            encryption_salt = validSalt,
            iv_vector = "iv",
            account_type = "MAIN",
            telegram_user_id = 777888999L
        )
        var updatedCalled = false

        val noId = repo.resetPasswordVerified(
            username = "alice",
            newPassword = "newPassword123",
            freshSessionString = "session-v2",
            verifiedTelegramUserId = null,
            fetchUser = { storedDto },
            updateCredentials = { _, _, _, _, _, _ -> updatedCalled = true; true }
        )
        assertTrue(noId is KunjoAuthResult.Error)
        assertFalse(updatedCalled)

        val wrongId = repo.resetPasswordVerified(
            username = "alice",
            newPassword = "newPassword123",
            freshSessionString = "session-v2",
            verifiedTelegramUserId = 111222333L,
            fetchUser = { storedDto },
            updateCredentials = { _, _, _, _, _, _ -> updatedCalled = true; true }
        )
        assertTrue(wrongId is KunjoAuthResult.Error)
        assertFalse(updatedCalled)
    }

    @Test
    fun `resetPasswordVerified succeeds for verified owner and preserves SUB role`() = runBlocking {
        val (settings, prefsState) = fakeSettings()
        val repo = KunjoAuthRepository(settings)
        val validSalt = Base64.getEncoder().encodeToString("1234567890abcdef".toByteArray())
        val storedDto = KunjoUserDto(
            username = "bob_sub",
            auth_hash = "old-hash",
            encrypted_session_blob = "old-blob",
            encryption_salt = validSalt,
            iv_vector = "iv",
            account_type = "SUB",
            telegram_user_id = 555666777L
        )
        var updatedUser = ""

        val ok = repo.resetPasswordVerified(
            username = "bob_sub",
            newPassword = "strongPassword1!",
            freshSessionString = "session-bob",
            verifiedTelegramUserId = 555666777L,
            fetchUser = { storedDto },
            updateCredentials = { u, _, _, _, _, _ -> updatedUser = u; true }
        )
        assertTrue(ok is KunjoAuthResult.Success)
        val success = ok as KunjoAuthResult.Success
        assertEquals("bob_sub", updatedUser)
        assertEquals("SUB", success.accountType)
        assertEquals("bob_sub", prefsState.value.kunjoUsername)
        assertEquals("SUB", prefsState.value.kunjoAccountType)
    }

    @Test
    fun `SupabaseRestClient encodes decodes and parses Telegram link marker in ip_address`() {
        val marker = com.novo.drive.core.network.SupabaseRestClient.encodeTelegramLinkMarker(777888999L, "@Alice_Tg")
        assertEquals("tg:777888999:Alice_Tg", marker)

        val (decodedId, decodedUser) = com.novo.drive.core.network.SupabaseRestClient.decodeTelegramLinkMarker(marker)
        assertEquals(777888999L, decodedId)
        assertEquals("Alice_Tg", decodedUser)

        val (nonMarkerId, nonMarkerUser) = com.novo.drive.core.network.SupabaseRestClient.decodeTelegramLinkMarker("192.168.1.10")
        assertEquals(0L, nonMarkerId)
        assertEquals(null, nonMarkerUser)

        val jsonPayload = """
            [
              {"id":"u-1","username":"alice_k","ip_address":"tg:777888999:Alice_Tg"},
              {"id":"u-2","dewangram_username":"bob_k","telegram_user_id":11223344,"telegram_username":"bob_tg"},
              {"id":"u-3","username":"unlinked","ip_address":"10.0.0.5"}
            ]
        """.trimIndent()
        val parsed = com.novo.drive.core.network.SupabaseRestClient.parseDewangramSearchJson(jsonPayload)
        assertEquals(3, parsed.size)
        assertEquals("alice_k", parsed[0].dewangramUsername)
        assertEquals(777888999L, parsed[0].telegramUserId)
        assertEquals("Alice_Tg", parsed[0].telegramUsername)

        assertEquals("bob_k", parsed[1].dewangramUsername)
        assertEquals(11223344L, parsed[1].telegramUserId)
        assertEquals("bob_tg", parsed[1].telegramUsername)

        assertEquals("unlinked", parsed[2].dewangramUsername)
        assertEquals(0L, parsed[2].telegramUserId)
    }

    @Test
    fun `default language is English and bi helper respects explicit language`() {
        assertEquals(AppLanguage.ENGLISH, UserPreferences().language)
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode(null))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("invalid-code"))
        assertEquals(AppLanguage.BENGALI, AppLanguage.fromCode("bn"))

        assertEquals("English", bi("English", "বাংলা", AppLanguage.ENGLISH))
        assertEquals("বাংলা", bi("English", "বাংলা", AppLanguage.BENGALI))
    }

    @Test
    fun `SupabaseRestClient parseRegisterResponse classifies status codes and bounds error body`() {
        assertEquals(SupabaseRegisterOutcome.Success, SupabaseRestClient.parseRegisterResponse(201, ""))
        assertEquals(SupabaseRegisterOutcome.Success, SupabaseRestClient.parseRegisterResponse(204, ""))
        assertEquals(SupabaseRegisterOutcome.Conflict, SupabaseRestClient.parseRegisterResponse(409, "duplicate key"))

        val longError = "E".repeat(250)
        val failure = SupabaseRestClient.parseRegisterResponse(401, longError)
        assertTrue(failure is SupabaseRegisterOutcome.HttpFailure)
        failure as SupabaseRegisterOutcome.HttpFailure
        assertEquals(401, failure.code)
        assertEquals(160, failure.detail.length)
    }

    @Test
    fun `KunjoAuthRepository returns English validation and Supabase errors by default and Bengali when selected`() = runBlocking {
        val (settings, _) = fakeSettings()
        val repo = KunjoAuthRepository(settings)

        val shortUser = repo.registerUsernameOnly("ab")
        assertTrue(shortUser is KunjoAuthResult.Error)
        val shortMsg = (shortUser as KunjoAuthResult.Error).message
        assertFalse("Expected English error but got: $shortMsg", bengaliRegex.containsMatchIn(shortMsg))
        assertTrue(shortMsg.contains("at least 3", ignoreCase = true))

        val badChars = repo.registerUsernameOnly("Bad-Name!")
        assertTrue(badChars is KunjoAuthResult.Error)
        val badCharsMsg = (badChars as KunjoAuthResult.Error).message
        assertFalse("Expected English error but got: $badCharsMsg", bengaliRegex.containsMatchIn(badCharsMsg))
        assertTrue(badCharsMsg.contains("lowercase", ignoreCase = true))

        val shortPass = repo.registerAccount("alice_test", "123")
        assertTrue(shortPass is KunjoAuthResult.Error)
        val shortPassMsg = (shortPass as KunjoAuthResult.Error).message
        assertFalse("Expected English error but got: $shortPassMsg", bengaliRegex.containsMatchIn(shortPassMsg))
        assertTrue(shortPassMsg.contains("6 characters", ignoreCase = true))

        listOf(
            SupabaseRegisterOutcome.MissingConfiguration,
            SupabaseRegisterOutcome.Conflict,
            SupabaseRegisterOutcome.HttpFailure(403, "RLS denied"),
            SupabaseRegisterOutcome.NetworkFailure("timeout")
        ).forEach { outcome ->
            val enMsg = repo.formatRegisterFailure(outcome, "alice_test", AppLanguage.ENGLISH)
            assertFalse("Expected pure English for $outcome: $enMsg", bengaliRegex.containsMatchIn(enMsg))
            val bnMsg = repo.formatRegisterFailure(outcome, "alice_test", AppLanguage.BENGALI)
            assertTrue("Expected Bengali for $outcome: $bnMsg", bengaliRegex.containsMatchIn(bnMsg))
        }

        settings.update { it.copy(language = AppLanguage.BENGALI) }
        val bnValidation = repo.registerUsernameOnly("ab")
        assertTrue(bnValidation is KunjoAuthResult.Error)
        assertTrue(bengaliRegex.containsMatchIn((bnValidation as KunjoAuthResult.Error).message))
    }
}
