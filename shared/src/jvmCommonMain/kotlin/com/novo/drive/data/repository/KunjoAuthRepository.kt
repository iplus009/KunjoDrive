package com.novo.drive.data.repository

import com.novo.drive.core.crypto.ZeroKnowledgeVault
import com.novo.drive.core.network.AdminAlertBotClient
import com.novo.drive.core.network.GeoAnalyticsClient
import com.novo.drive.core.network.KunjoUserDto
import com.novo.drive.core.network.Secrets
import com.novo.drive.core.network.SupabaseRestClient
import com.novo.drive.core.network.UserCountSummary
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

    suspend fun registerUsernameOnly(
        username: String,
        storageChannelId: Long? = null
    ): KunjoAuthResult = withContext(Dispatchers.IO) {
        val cleanUser = username.trim().lowercase().removePrefix("@")
        if (cleanUser.length < 3) {
            return@withContext KunjoAuthResult.Error("ইউজারনেম সর্বনিম্ন ৩ অক্ষরের হতে হবে")
        }
        if (!cleanUser.matches(Regex("^[a-z0-9_]{3,20}$"))) {
            return@withContext KunjoAuthResult.Error("ইউজারনেম শুধুমাত্র ছোট হাতের ইংরেজি অক্ষর (a-z), সংখ্যা (0-9) এবং _ হতে হবে")
        }

        val existing = SupabaseRestClient.getUserByUsername(cleanUser)
        val currentLocalUser = settingsRepository.preferences.first().kunjoUsername
        if (existing != null && existing.username != currentLocalUser) {
            return@withContext KunjoAuthResult.Error("এই ইউজারনেমটি (@$cleanUser) ইতিমধ্যে অন্য কেউ ব্যবহার করছেন! অন্য একটি ইউনিক ইউজারনেম নির্বাচন করুন।")
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
            SupabaseRestClient.registerUser(dto)
        }

        settingsRepository.update { prefs ->
            prefs.copy(
                kunjoUsername = cleanUser,
                kunjoAccountType = "MAIN",
                kunjoCanUpload = true,
                storageChatId = storageChannelId ?: prefs.storageChatId
            )
        }

        KunjoAuthResult.Success(cleanUser, "MAIN", null)
    }

    suspend fun registerAccount(
        username: String,
        password: String,
        accountType: String = "MAIN",
        parentUsername: String? = null,
        sessionString: String? = null,
        storageChannelId: Long? = null
    ): KunjoAuthResult = withContext(Dispatchers.IO) {
        val cleanUser = username.trim().lowercase().removePrefix("@")
        if (cleanUser.length < 3) {
            return@withContext KunjoAuthResult.Error("ইউজারনেম সর্বনিম্ন ৩ অক্ষরের হতে হবে")
        }
        if (!cleanUser.matches(Regex("^[a-z0-9_]{3,20}$"))) {
            return@withContext KunjoAuthResult.Error("ইউজারনেম শুধুমাত্র ছোট হাতের ইংরেজি অক্ষর (a-z), সংখ্যা (0-9) এবং _ হতে হবে")
        }
        if (password.length < 6) {
            return@withContext KunjoAuthResult.Error("পাসওয়ার্ড সর্বনিম্ন ৬ অক্ষরের হতে হবে")
        }

        // Check uniqueness on Supabase Cloud
        val existing = SupabaseRestClient.getUserByUsername(cleanUser)
        if (existing != null) {
            val derivedCheck = ZeroKnowledgeVault.deriveKeys(password.toCharArray(), existing.encryption_salt.ifBlank { null })
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
                return@withContext KunjoAuthResult.Error("এই ইউজারনেমটি (@$cleanUser) ইতিমধ্যে তৈরি করা আছে! এটি যদি আপনার অ্যাকাউন্ট হয় তবে সঠিক পাসওয়ার্ড দিন, অথবা অন্য একটি ইউনিক ইউজারনেম দিন।")
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
        val isRegisteredOnline = SupabaseRestClient.registerUser(dto)
        if (!isRegisteredOnline) {
            derived.wipe()
            return@withContext KunjoAuthResult.Error("ক্লাউডে অ্যাকাউন্ট সেভ করা সম্ভব হয়নি। ইন্টারনেট সংযোগ চেক করে পুনরায় চেষ্টা করুন।")
        }

        // 6. Update local user preferences
        settingsRepository.update { prefs ->
            prefs.copy(
                kunjoUsername = cleanUser,
                kunjoAccountType = accountType.uppercase(),
                kunjoAuthHash = derived.authHash,
                kunjoCanUpload = (accountType.uppercase() == "MAIN")
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
        val cleanUser = username.trim().lowercase().removePrefix("@")
        if (cleanUser.isBlank() || password.isBlank()) {
            return@withContext KunjoAuthResult.Error("ইউজারনেম এবং পাসওয়ার্ড দুটোই প্রয়োজন")
        }

        // 1. Fetch from Supabase (Pure online-only authentication)
        val remoteUser = SupabaseRestClient.getUserByUsername(cleanUser)
            ?: return@withContext KunjoAuthResult.Error("অ্যাকাউন্ট খুঁজে পাওয়া যায়নি! ইউজারনেম ঠিক আছে কিনা বা ইন্টারনেট কানেকশন চেক করুন।")

        val authHashToCheck = remoteUser.auth_hash
        val saltToCheck = remoteUser.encryption_salt
        val encryptedBlob = remoteUser.encrypted_session_blob
        val ivVector = remoteUser.iv_vector
        val accountType = remoteUser.account_type
        val channelId = remoteUser.primary_channel_id

        // 2. Derive key locally using remote salt
        val derived = ZeroKnowledgeVault.deriveKeys(password.toCharArray(), saltToCheck.ifBlank { null })

        // 3. Verify Auth Hash
        if (derived.authHash != authHashToCheck) {
            derived.wipe()
            return@withContext KunjoAuthResult.Error("ভুল পাসওয়ার্ড! পুনরায় চেষ্টা করুন।")
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
        freshSessionString: String
    ): KunjoAuthResult = withContext(Dispatchers.IO) {
        val cleanUser = username.trim().lowercase().removePrefix("@")
        if (cleanUser.length < 3) {
            return@withContext KunjoAuthResult.Error("সঠিক ইউজারনেম দিন")
        }
        if (newPassword.length < 6) {
            return@withContext KunjoAuthResult.Error("নতুন পাসওয়ার্ড সর্বনিম্ন ৬ অক্ষরের হতে হবে")
        }

        val derived = ZeroKnowledgeVault.deriveKeys(newPassword.toCharArray())
        val sessionPayload = freshSessionString.ifBlank { "UNINITIALIZED" }
        val encryptedPayload = ZeroKnowledgeVault.encrypt(sessionPayload, derived.encryptionKey)

        val success = SupabaseRestClient.updateSessionCredentials(
            username = cleanUser,
            authHash = derived.authHash,
            encryptedBlob = encryptedPayload.ciphertextBase64,
            ivVector = encryptedPayload.ivBase64,
            primaryChannelId = null,
            encryptionSalt = derived.saltBase64
        )

        if (!success) {
            derived.wipe()
            return@withContext KunjoAuthResult.Error("ক্লাউডে পাসওয়ার্ড আপডেট করা যায়নি। ইন্টারনেট সংযোগ বা ইউজারনেম যাচাই করে আবার চেষ্টা করুন।")
        }

        val accountType = SupabaseRestClient.getUserByUsername(cleanUser)?.account_type ?: "MAIN"
        settingsRepository.update { prefs ->
            prefs.copy(
                kunjoUsername = cleanUser,
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
        val cleanCurrent = currentUsername.trim().lowercase().removePrefix("@")
        val cleanNew = newUsername.trim().lowercase().removePrefix("@")

        if (cleanNew == cleanCurrent) {
            return@withContext KunjoAuthResult.Success(cleanNew, "MAIN", null)
        }
        if (cleanNew.length < 3) {
            return@withContext KunjoAuthResult.Error("ইউজারনেম সর্বনিম্ন ৩ অক্ষরের হতে হবে")
        }
        if (!cleanNew.matches(Regex("^[a-z0-9_]{3,20}$"))) {
            return@withContext KunjoAuthResult.Error("ইউজারনেম শুধুমাত্র ছোট হাতের ইংরেজি অক্ষর (a-z), সংখ্যা (0-9) এবং _ হতে হবে")
        }

        // Check uniqueness on Supabase
        val existing = SupabaseRestClient.getUserByUsername(cleanNew)
        if (existing != null) {
            return@withContext KunjoAuthResult.Error("এই ইউজারনেমটি (@$cleanNew) ইতিমধ্যে অন্য কেউ ব্যবহার করছেন! অন্য একটি নাম পছন্দ করুন।")
        }

        val success = SupabaseRestClient.updateUsername(cleanCurrent, cleanNew)
        if (!success) {
            return@withContext KunjoAuthResult.Error("ক্লাউডে ইউজারনেম পরিবর্তন করা সম্ভব হয়নি। ইন্টারনেট চেক করুন।")
        }

        settingsRepository.update { prefs ->
            prefs.copy(kunjoUsername = cleanNew)
        }
        KunjoAuthResult.Success(cleanNew, "MAIN", null)
    }

    suspend fun fetchAdminTelemetry(): UserCountSummary = SupabaseRestClient.fetchUserCounts()
}
