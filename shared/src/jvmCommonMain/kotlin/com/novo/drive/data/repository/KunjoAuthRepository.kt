package com.novo.drive.data.repository

import com.novo.drive.core.common.bi
import com.novo.drive.core.crypto.ZeroKnowledgeVault
import com.novo.drive.core.network.AdminAlertBotClient
import com.novo.drive.core.network.GeoAnalyticsClient
import com.novo.drive.core.network.KunjoUserDto
import com.novo.drive.core.network.Secrets
import com.novo.drive.core.network.SupabaseRegisterOutcome
import com.novo.drive.core.network.SupabaseRestClient
import com.novo.drive.core.network.UserCountSummary
import com.novo.drive.domain.model.AppLanguage
import com.novo.drive.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

sealed interface KunjoAuthResult {
    data class Success(
        val username: String,
        val accountType: String,
        val decryptedSession: String?
    ) : KunjoAuthResult

    data class Error(val message: String) : KunjoAuthResult
}

class KunjoAuthRepository(
    private val settingsRepository: SettingsRepository
) {

    private suspend fun currentLang(): AppLanguage =
        settingsRepository.preferences.first().language

    internal fun formatRegisterFailure(
        outcome: SupabaseRegisterOutcome,
        cleanUser: String,
        lang: AppLanguage
    ): String = when (outcome) {
        is SupabaseRegisterOutcome.MissingConfiguration -> bi(
            "Cloud backend keys are missing in this build. Please update to the latest official release.",
            "এই বিল্ডে Supabase ক্লাউড কী যুক্ত নেই। অনুগ্রহ করে সর্বশেষ রিলিজ ইনস্টল করুন।",
            lang
        )
        is SupabaseRegisterOutcome.Conflict -> bi(
            "This username (@$cleanUser) is already taken! Please choose another unique username.",
            "এই ইউজারনেমটি (@$cleanUser) ইতিমধ্যে অন্য কেউ ব্যবহার করছেন! অন্য একটি ইউনিক ইউজারনেম দিন।",
            lang
        )
        is SupabaseRegisterOutcome.HttpFailure -> bi(
            "Could not save username to Supabase (HTTP ${outcome.code}). Check your connection and try again.",
            "Supabase ক্লাউডে ইউজারনেম সংরক্ষণ করা যায়নি (HTTP ${outcome.code})। ইন্টারনেট চেক করে আবার চেষ্টা করুন।",
            lang
        )
        is SupabaseRegisterOutcome.NetworkFailure -> bi(
            "Could not reach Supabase (${outcome.message}). Check your internet connection and try again.",
            "Supabase সার্ভারে সংযোগ করা যায়নি (${outcome.message})। ইন্টারনেট সংযোগ চেক করে আবার চেষ্টা করুন।",
            lang
        )
        is SupabaseRegisterOutcome.Success -> ""
    }

    suspend fun registerUsernameOnly(
        username: String,
        storageChannelId: Long? = null,
        telegramUserId: Long? = null,
        telegramUsername: String? = null
    ): KunjoAuthResult = withContext(Dispatchers.IO) {
        val lang = currentLang()
        val cleanUser = username.trim().lowercase().removePrefix("@")
        if (cleanUser.length < 3) {
            return@withContext KunjoAuthResult.Error(
                bi("Username must be at least 3 characters.", "ইউজারনেম সর্বনিম্ন ৩ অক্ষরের হতে হবে", lang)
            )
        }
        if (!cleanUser.matches(Regex("^[a-z0-9_]{3,20}$"))) {
            return@withContext KunjoAuthResult.Error(
                bi(
                    "Username can only contain lowercase letters (a-z), numbers (0-9), and _.",
                    "ইউজারনেম শুধুমাত্র ছোট হাতের ইংরেজি অক্ষর (a-z), সংখ্যা (0-9) এবং _ হতে হবে",
                    lang
                )
            )
        }

        val existing = SupabaseRestClient.getUserByUsername(cleanUser)
        val currentLocalUser = settingsRepository.preferences.first().kunjoUsername
        if (existing != null && existing.username != currentLocalUser) {
            return@withContext KunjoAuthResult.Error(
                bi(
                    "This username (@$cleanUser) is already taken! Please pick another unique username.",
                    "এই ইউজারনেমটি (@$cleanUser) ইতিমধ্যে অন্য কেউ ব্যবহার করছেন! অন্য একটি ইউনিক ইউজারনেম নির্বাচন করুন।",
                    lang
                )
            )
        }

        if (existing == null) {
            val location = GeoAnalyticsClient.fetchCurrentLocation()
            val dto = KunjoUserDto(
                username = cleanUser,
                auth_hash = "",
                encrypted_session_blob = "",
                encryption_salt = "",
                iv_vector = "",
                primary_channel_id = storageChannelId,
                account_type = "MAIN",
                country = location.country,
                city = location.city,
                isp = location.isp,
                device_model = System.getProperty("os.name") ?: "Android",
                app_version = Secrets.APP_VERSION
            )
            val outcome = SupabaseRestClient.registerUserDetailed(dto)
            if (outcome !is SupabaseRegisterOutcome.Success) {
                return@withContext KunjoAuthResult.Error(formatRegisterFailure(outcome, cleanUser, lang))
            }
        }

        if (telegramUserId != null && telegramUserId != 0L) {
            if (existing == null || existing.telegram_user_id == null || !telegramUsername.isNullOrBlank()) {
                SupabaseRestClient.linkTelegramUserId(cleanUser, telegramUserId, telegramUsername)
            }
            if (existing?.dewangram_username.isNullOrBlank()) {
                SupabaseRestClient.setDewangramUsername(cleanUser, cleanUser)
            }
        }

        val resolvedAccountType = existing?.account_type ?: "MAIN"
        settingsRepository.update { prefs ->
            prefs.copy(
                kunjoUsername = cleanUser,
                kunjoAccountType = resolvedAccountType,
                kunjoCanUpload = resolvedAccountType.equals("MAIN", ignoreCase = true),
                storageChatId = storageChannelId ?: prefs.storageChatId
            )
        }

        KunjoAuthResult.Success(cleanUser, resolvedAccountType, null)
    }

    suspend fun registerAccount(
        username: String,
        password: String,
        accountType: String = "MAIN",
        parentUsername: String? = null,
        sessionString: String? = null,
        storageChannelId: Long? = null,
        telegramUserId: Long? = null,
        telegramUsername: String? = null
    ): KunjoAuthResult = withContext(Dispatchers.IO) {
        val lang = currentLang()
        val cleanUser = username.trim().lowercase().removePrefix("@")
        if (cleanUser.length < 3) {
            return@withContext KunjoAuthResult.Error(
                bi("Username must be at least 3 characters.", "ইউজারনেম সর্বনিম্ন ৩ অক্ষরের হতে হবে", lang)
            )
        }
        if (!cleanUser.matches(Regex("^[a-z0-9_]{3,20}$"))) {
            return@withContext KunjoAuthResult.Error(
                bi(
                    "Username can only contain lowercase letters (a-z), numbers (0-9), and _.",
                    "ইউজারনেম শুধুমাত্র ছোট হাতের ইংরেজি অক্ষর (a-z), সংখ্যা (0-9) এবং _ হতে হবে",
                    lang
                )
            )
        }
        if (password.length < 6) {
            return@withContext KunjoAuthResult.Error(
                bi("Password must be at least 6 characters.", "পাসওয়ার্ড সর্বনিম্ন ৬ অক্ষরের হতে হবে", lang)
            )
        }

        // Check uniqueness on Supabase Cloud
        val existing = SupabaseRestClient.getUserByUsername(cleanUser)
        if (existing != null) {
            val derivedCheck = runCatching {
                ZeroKnowledgeVault.deriveKeys(password.toCharArray(), existing.encryption_salt.ifBlank { null })
            }.getOrElse {
                return@withContext KunjoAuthResult.Error(
                    bi("Account security salt is invalid.", "অ্যাকাউন্টের নিরাপত্তা সল্ট ত্রুটিপূর্ণ।", lang)
                )
            }
            if (derivedCheck.authHash == existing.auth_hash) {
                // Existing account credentials verified - link and log in
                settingsRepository.update { prefs ->
                    prefs.copy(
                        kunjoUsername = cleanUser,
                        kunjoAccountType = existing.account_type,
                        kunjoAuthHash = derivedCheck.authHash,
                        kunjoCanUpload = (existing.account_type.uppercase() == "MAIN"),
                        storageChatId = existing.primary_channel_id ?: storageChannelId ?: prefs.storageChatId
                    )
                }
                derivedCheck.wipe()
                return@withContext KunjoAuthResult.Success(
                    username = cleanUser,
                    accountType = existing.account_type,
                    decryptedSession = sessionString
                )
            } else {
                derivedCheck.wipe()
                return@withContext KunjoAuthResult.Error(
                    bi(
                        "This username (@$cleanUser) is already registered. Enter the correct password if it belongs to you, or choose a different username.",
                        "এই ইউজারনেমটি (@$cleanUser) ইতিমধ্যে তৈরি করা আছে! এটি যদি আপনার অ্যাকাউন্ট হয় তবে সঠিক পাসওয়ার্ড দিন, অথবা অন্য একটি ইউনিক ইউজারনেম দিন।",
                        lang
                    )
                )
            }
        }

        // 1. Derive Keys (Key 1 for Auth, Key 2 for Session Encryption)
        val derived = ZeroKnowledgeVault.deriveKeys(password.toCharArray())

        // 2. Encrypt session if provided
        val encryptedPayload = if (!sessionString.isNullOrBlank()) {
            ZeroKnowledgeVault.encrypt(sessionString, derived.encryptionKey)
        } else {
            ZeroKnowledgeVault.encrypt("UNINITIALIZED", derived.encryptionKey)
        }

        // 3. Detect Geolocation silently
        val location = GeoAnalyticsClient.fetchCurrentLocation()

        val deviceModel = System.getProperty("os.name") ?: "Android"

        // 4. Construct DTO
        val dto = KunjoUserDto(
            username = cleanUser,
            auth_hash = derived.authHash,
            encrypted_session_blob = encryptedPayload.ciphertextBase64,
            encryption_salt = derived.saltBase64,
            iv_vector = encryptedPayload.ivBase64,
            primary_channel_id = storageChannelId,
            account_type = accountType.uppercase(),
            country = location.country,
            city = location.city,
            isp = location.isp,
            device_model = deviceModel,
            app_version = Secrets.APP_VERSION
        )

        // 5. Save to Supabase Cloud
        val outcome = SupabaseRestClient.registerUserDetailed(dto)
        if (outcome !is SupabaseRegisterOutcome.Success) {
            derived.wipe()
            return@withContext KunjoAuthResult.Error(formatRegisterFailure(outcome, cleanUser, lang))
        }

        if (telegramUserId != null && telegramUserId != 0L) {
            SupabaseRestClient.linkTelegramUserId(cleanUser, telegramUserId, telegramUsername)
        }
        SupabaseRestClient.setDewangramUsername(cleanUser, cleanUser)

        // 6. Update local user preferences
        settingsRepository.update { prefs ->
            prefs.copy(
                kunjoUsername = cleanUser,
                kunjoAccountType = accountType.uppercase(),
                kunjoAuthHash = derived.authHash,
                kunjoCanUpload = (accountType.uppercase() == "MAIN"),
                storageChatId = storageChannelId ?: prefs.storageChatId
            )
        }

        // 7. Send Telegram Alert to Ismail Bhai
        try {
            val counts = SupabaseRestClient.fetchUserCounts()
            AdminAlertBotClient.notifyNewSignup(
                username = cleanUser,
                accountType = accountType.uppercase(),
                parentUsername = parentUsername,
                country = location.country,
                city = location.city,
                isp = location.isp,
                deviceModel = deviceModel,
                totalUsers = counts.totalUsers,
                mainUsers = counts.mainUsers,
                subUsers = counts.subUsers
            )
        } catch (_: Throwable) {
            // Silently continue
        }

        derived.wipe()
        KunjoAuthResult.Success(
            username = cleanUser,
            accountType = accountType.uppercase(),
            decryptedSession = sessionString
        )
    }

    suspend fun loginWithPassword(
        username: String,
        password: String
    ): KunjoAuthResult = withContext(Dispatchers.IO) {
        val lang = currentLang()
        val cleanUser = username.trim().lowercase().removePrefix("@")
        if (cleanUser.isBlank() || password.isBlank()) {
            return@withContext KunjoAuthResult.Error(
                bi("Both username and password are required.", "ইউজারনেম এবং পাসওয়ার্ড দুটোই প্রয়োজন", lang)
            )
        }

        // 1. Fetch from Supabase (Pure online-only authentication)
        val remoteUser = SupabaseRestClient.getUserByUsername(cleanUser)
            ?: return@withContext KunjoAuthResult.Error(
                bi(
                    "Account not found. Check your username or internet connection.",
                    "অ্যাকাউন্ট খুঁজে পাওয়া যায়নি! ইউজারনেম ঠিক আছে কিনা বা ইন্টারনেট কানেকশন চেক করুন।",
                    lang
                )
            )

        val authHashToCheck = remoteUser.auth_hash
        val saltToCheck = remoteUser.encryption_salt
        val encryptedBlob = remoteUser.encrypted_session_blob
        val ivVector = remoteUser.iv_vector
        val accountType = remoteUser.account_type
        val channelId = remoteUser.primary_channel_id

        // 2. Derive key locally using remote salt
        val derived = runCatching {
            ZeroKnowledgeVault.deriveKeys(password.toCharArray(), saltToCheck.ifBlank { null })
        }.getOrElse {
            return@withContext KunjoAuthResult.Error(
                bi("Account encryption salt is invalid.", "অ্যাকাউন্টের এনক্রিপশন সল্ট ত্রুটিপূর্ণ।", lang)
            )
        }

        // 3. Verify Auth Hash
        if (derived.authHash != authHashToCheck) {
            derived.wipe()
            return@withContext KunjoAuthResult.Error(
                bi("Incorrect password. Please try again.", "ভুল পাসওয়ার্ড! পুনরায় চেষ্টা করুন।", lang)
            )
        }

        // 4. Decrypt session if available
        var decryptedSession: String? = null
        if (encryptedBlob.isNotBlank() && ivVector.isNotBlank()) {
            try {
                val plain = ZeroKnowledgeVault.decrypt(encryptedBlob, ivVector, derived.encryptionKey)
                if (plain != "UNINITIALIZED") {
                    decryptedSession = plain
                }
            } catch (_: Throwable) {
                // Session corrupt or tampered
            }
        }

        // 5. Sub-account policy enforcement (Stream size limit)
        var streamSizeLimitMb = 0
        var canUpload = true
        if (accountType.equals("SUB", ignoreCase = true)) {
            streamSizeLimitMb = 500 // default 500MB limit for sub-accounts
            canUpload = false
            val userId = remoteUser.id
            if (!userId.isNullOrBlank()) {
                val policy = SupabaseRestClient.getSubAccountPolicy(userId)
                if (policy != null) {
                    streamSizeLimitMb = policy.max_stream_size_mb
                    canUpload = policy.can_upload
                }
            }
        }

        // 6. Save verified state to local settings (keep existing onboardingComplete until TDLib session check)
        settingsRepository.update { prefs ->
            prefs.copy(
                kunjoUsername = cleanUser,
                kunjoAccountType = accountType,
                kunjoAuthHash = derived.authHash,
                kunjoMaxStreamSizeMb = streamSizeLimitMb,
                kunjoCanUpload = canUpload,
                storageChatId = channelId ?: prefs.storageChatId
            )
        }

        derived.wipe()
        KunjoAuthResult.Success(
            username = cleanUser,
            accountType = accountType,
            decryptedSession = decryptedSession
        )
    }

    suspend fun resetPassword(
        username: String,
        newPassword: String,
        freshSessionString: String,
        verifiedTelegramUserId: Long? = null
    ): KunjoAuthResult = resetPasswordVerified(
        username = username,
        newPassword = newPassword,
        freshSessionString = freshSessionString,
        verifiedTelegramUserId = verifiedTelegramUserId,
        fetchUser = SupabaseRestClient::getUserByUsername,
        updateCredentials = { user, hash, blob, iv, channelId, salt ->
            SupabaseRestClient.updateSessionCredentials(user, hash, blob, iv, channelId, salt)
        }
    )

    internal suspend fun resetPasswordVerified(
        username: String,
        newPassword: String,
        freshSessionString: String,
        verifiedTelegramUserId: Long?,
        fetchUser: suspend (String) -> KunjoUserDto?,
        updateCredentials: suspend (String, String, String, String, Long?, String?) -> Boolean
    ): KunjoAuthResult = withContext(Dispatchers.IO) {
        val lang = currentLang()
        val cleanUser = username.trim().lowercase().removePrefix("@")
        if (cleanUser.length < 3) {
            return@withContext KunjoAuthResult.Error(bi("Enter a valid username.", "সঠিক ইউজারনেম দিন", lang))
        }
        if (newPassword.length < 6) {
            return@withContext KunjoAuthResult.Error(
                bi("New password must be at least 6 characters.", "নতুন পাসওয়ার্ড সর্বনিম্ন ৬ অক্ষরের হতে হবে", lang)
            )
        }
        if (verifiedTelegramUserId == null || verifiedTelegramUserId == 0L) {
            return@withContext KunjoAuthResult.Error(
                bi(
                    "Telegram OTP verification is required for security.",
                    "নিরাপত্তার স্বার্থে টেলিগ্রাম ওটিপি ভেরিফিকেশন আবশ্যক।",
                    lang
                )
            )
        }

        val existing = fetchUser(cleanUser)
            ?: return@withContext KunjoAuthResult.Error(bi("Account not found.", "অ্যাকাউন্ট খুঁজে পাওয়া যায়নি।", lang))

        if (existing.telegram_user_id == null || existing.telegram_user_id == 0L) {
            return@withContext KunjoAuthResult.Error(
                bi(
                    "No verified Telegram ID is linked to this account, so password reset is unavailable.",
                    "এই অ্যাকাউন্টে ভেরিফাইড টেলিগ্রাম আইডি যুক্ত নেই, তাই পাসওয়ার্ড রিসেট করা যাবে না।",
                    lang
                )
            )
        }
        if (existing.telegram_user_id != verifiedTelegramUserId) {
            return@withContext KunjoAuthResult.Error(
                bi(
                    "The verified Telegram account does not match @$cleanUser.",
                    "ভেরিফাইড টেলিগ্রাম অ্যাকাউন্টটি এই ইউজারনেমের (@$cleanUser) সাথে মিলছে না!",
                    lang
                )
            )
        }

        val derived = ZeroKnowledgeVault.deriveKeys(newPassword.toCharArray())
        val sessionPayload = freshSessionString.ifBlank { "UNINITIALIZED" }
        val encryptedPayload = ZeroKnowledgeVault.encrypt(sessionPayload, derived.encryptionKey)

        val success = updateCredentials(
            cleanUser,
            derived.authHash,
            encryptedPayload.ciphertextBase64,
            encryptedPayload.ivBase64,
            null,
            derived.saltBase64
        )

        if (!success) {
            derived.wipe()
            return@withContext KunjoAuthResult.Error(
                bi(
                    "Could not update password in the cloud. Check your internet connection or username.",
                    "ক্লাউডে পাসওয়ার্ড আপডেট করা যায়নি। ইন্টারনেট সংযোগ বা ইউজারনেম যাচাই করে আবার চেষ্টা করুন।",
                    lang
                )
            )
        }

        val accountType = existing.account_type.ifBlank { "MAIN" }
        settingsRepository.update { prefs ->
            prefs.copy(
                kunjoUsername = cleanUser,
                kunjoAccountType = accountType,
                kunjoAuthHash = derived.authHash
            )
        }

        derived.wipe()
        KunjoAuthResult.Success(cleanUser, accountType, freshSessionString.ifBlank { null })
    }

    suspend fun updateUsername(
        currentUsername: String,
        newUsername: String
    ): KunjoAuthResult = withContext(Dispatchers.IO) {
        val lang = currentLang()
        val cleanCurrent = currentUsername.trim().lowercase().removePrefix("@")
        val cleanNew = newUsername.trim().lowercase().removePrefix("@")
        val currentAccountType = settingsRepository.preferences.first().kunjoAccountType.ifBlank { "MAIN" }

        if (cleanNew == cleanCurrent) {
            return@withContext KunjoAuthResult.Success(cleanNew, currentAccountType, null)
        }
        if (cleanNew.length < 3) {
            return@withContext KunjoAuthResult.Error(
                bi("Username must be at least 3 characters.", "ইউজারনেম সর্বনিম্ন ৩ অক্ষরের হতে হবে", lang)
            )
        }
        if (!cleanNew.matches(Regex("^[a-z0-9_]{3,20}$"))) {
            return@withContext KunjoAuthResult.Error(
                bi(
                    "Username can only contain lowercase letters (a-z), numbers (0-9), and _.",
                    "ইউজারনেম শুধুমাত্র ছোট হাতের ইংরেজি অক্ষর (a-z), সংখ্যা (0-9) এবং _ হতে হবে",
                    lang
                )
            )
        }

        // Check uniqueness on Supabase
        val existing = SupabaseRestClient.getUserByUsername(cleanNew)
        if (existing != null) {
            return@withContext KunjoAuthResult.Error(
                bi(
                    "This username (@$cleanNew) is already taken! Please choose another name.",
                    "এই ইউজারনেমটি (@$cleanNew) ইতিমধ্যে অন্য কেউ ব্যবহার করছেন! অন্য একটি নাম পছন্দ করুন।",
                    lang
                )
            )
        }

        val success = SupabaseRestClient.updateUsername(cleanCurrent, cleanNew)
        if (!success) {
            return@withContext KunjoAuthResult.Error(
                bi(
                    "Could not update username in the cloud. Check your internet connection.",
                    "ক্লাউডে ইউজারনেম পরিবর্তন করা সম্ভব হয়নি। ইন্টারনেট চেক করুন।",
                    lang
                )
            )
        }

        settingsRepository.update { prefs ->
            prefs.copy(kunjoUsername = cleanNew)
        }
        KunjoAuthResult.Success(cleanNew, currentAccountType, null)
    }

    suspend fun fetchAdminTelemetry(): UserCountSummary = SupabaseRestClient.fetchUserCounts()
}
