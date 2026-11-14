package com.novo.drive.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

@Serializable
data class KunjoUserDto(
    val id: String? = null,
    val username: String,
    val auth_hash: String,
    val encrypted_session_blob: String,
    val encryption_salt: String,
    val iv_vector: String,
    val primary_channel_id: Long? = null,
    val account_type: String = "MAIN",
    val country: String = "Unknown",
    val city: String = "Unknown",
    val isp: String = "Unknown",
    val device_model: String = "Unknown",
    val ip_address: String? = null,
    val app_version: String = Secrets.APP_VERSION,
    // DewanGram fields
    val dewangram_username: String? = null,
    val telegram_user_id: Long? = null,
    val telegram_linked_at: Long? = null,
    val last_seen_at: Long? = null
)

/** Lightweight search result for DewanGram user discovery — no sensitive data. */
data class DewanGramUserSearchResult(
    val id: String,
    val dewangramUsername: String,
    val telegramUserId: Long,
    val telegramUsername: String? = null
)

/** Internal DTO for search results from Supabase. */
@Serializable
private data class DewanGramSearchDto(
    val id: String? = null,
    val username: String? = null,
    val dewangram_username: String? = null,
    val telegram_user_id: Long? = null,
    val telegram_username: String? = null,
    val ip_address: String? = null
)

@Serializable
private data class KunjoUserCreateRequest(
    val username: String,
    val auth_hash: String,
    val encrypted_session_blob: String,
    val encryption_salt: String,
    val iv_vector: String,
    val primary_channel_id: Long? = null,
    val account_type: String = "MAIN",
    val country: String = "Unknown",
    val city: String = "Unknown",
    val isp: String = "Unknown",
    val device_model: String = "Unknown",
    val app_version: String = Secrets.APP_VERSION
)

@Serializable
private data class SessionCredentialPatch(
    val auth_hash: String,
    val encrypted_session_blob: String,
    val iv_vector: String,
    val encryption_salt: String? = null
)

@Serializable
private data class SessionCredentialWithChannelPatch(
    val auth_hash: String,
    val encrypted_session_blob: String,
    val iv_vector: String,
    val encryption_salt: String? = null,
    val primary_channel_id: Long
)

@Serializable
private data class UsernamePatch(
    val username: String
)

@Serializable
private data class TelegramLinkPatch(
    val telegram_user_id: Long,
    val telegram_linked_at: Long
)

@Serializable
private data class IpAddressTelegramLinkPatch(
    val ip_address: String
)

@Serializable
private data class DewangramUsernamePatch(
    val dewangram_username: String
)

@Serializable
private data class LastSeenPatch(
    val last_seen_at: Long
)

data class UserCountSummary(
    val totalUsers: Int = 0,
    val mainUsers: Int = 0,
    val subUsers: Int = 0,
    val isUnavailable: Boolean = false
)

data class CountryCount(
    val country: String,
    val count: Int
)

@Serializable
data class FamilyMemberPolicy(
    val max_stream_size_mb: Int = 500,
    val can_upload: Boolean = false,
    val allowed_folder: String = "FAMILY"
)

sealed interface SupabaseRegisterOutcome {
    data object Success : SupabaseRegisterOutcome
    data object MissingConfiguration : SupabaseRegisterOutcome
    data object Conflict : SupabaseRegisterOutcome
    data class HttpFailure(val code: Int, val detail: String) : SupabaseRegisterOutcome
    data class NetworkFailure(val message: String) : SupabaseRegisterOutcome
}

object SupabaseRestClient {

    private val BASE_URL get() = Secrets.SUPABASE_URL
    private val API_KEY get() = Secrets.SUPABASE_KEY

    private val isConfigured get() = BASE_URL.isNotBlank() && API_KEY.isNotBlank()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun openConnection(endpoint: String, method: String): HttpURLConnection {
        val url = URI("$BASE_URL$endpoint").toURL()
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.requestMethod = method
        conn.setRequestProperty("apikey", API_KEY)
        conn.setRequestProperty("Authorization", "Bearer $API_KEY")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        return conn
    }

    suspend fun getUserByUsername(username: String): KunjoUserDto? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null
        var conn: HttpURLConnection? = null
        try {
            val encodedUsername = URLEncoder.encode(username.trim().lowercase(), "UTF-8")
            conn = openConnection("/kunjo_users?username=eq.$encodedUsername&select=*", "GET")
            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val array = json.decodeFromString<List<KunjoUserDto>>(responseText)
                return@withContext array.firstOrNull()
            }
        } catch (_: Throwable) {
            // Log and fallback
        } finally {
            conn?.disconnect()
        }
        null
    }

    suspend fun registerUser(user: KunjoUserDto): Boolean =
        registerUserDetailed(user) is SupabaseRegisterOutcome.Success

    internal fun parseRegisterResponse(code: Int, errorBody: String): SupabaseRegisterOutcome = when {
        code in 200..204 -> SupabaseRegisterOutcome.Success
        code == 409 -> SupabaseRegisterOutcome.Conflict
        else -> SupabaseRegisterOutcome.HttpFailure(code, errorBody.take(160))
    }

    suspend fun registerUserDetailed(user: KunjoUserDto): SupabaseRegisterOutcome = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext SupabaseRegisterOutcome.MissingConfiguration
        var conn: HttpURLConnection? = null
        try {
            conn = openConnection("/kunjo_users", "POST")
            conn.setRequestProperty("Prefer", "return=representation")
            conn.doOutput = true

            val createPayload = KunjoUserCreateRequest(
                username = user.username.trim().lowercase().removePrefix("@"),
                auth_hash = user.auth_hash,
                encrypted_session_blob = user.encrypted_session_blob,
                encryption_salt = user.encryption_salt,
                iv_vector = user.iv_vector,
                primary_channel_id = user.primary_channel_id,
                account_type = user.account_type,
                country = user.country,
                city = user.city,
                isp = user.isp,
                device_model = user.device_model,
                app_version = Secrets.APP_VERSION
            )
            val payload = json.encodeToString(createPayload)
            conn.outputStream.bufferedWriter().use { it.write(payload) }

            val code = conn.responseCode
            val errText = if (code in 200..204 || code == 409) {
                ""
            } else {
                runCatching {
                    conn.errorStream?.bufferedReader()?.use { it.readText() }
                }.getOrNull().orEmpty()
            }
            parseRegisterResponse(code, errText)
        } catch (t: Throwable) {
            SupabaseRegisterOutcome.NetworkFailure(t.message ?: t::class.simpleName.orEmpty())
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun updateSessionCredentials(
        username: String,
        authHash: String,
        encryptedBlob: String,
        ivVector: String,
        primaryChannelId: Long?,
        encryptionSalt: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext false
        var conn: HttpURLConnection? = null
        try {
            val encodedUsername = URLEncoder.encode(username.trim().lowercase(), "UTF-8")
            conn = openConnection("/kunjo_users?username=eq.$encodedUsername", "PATCH")
            conn.setRequestProperty("Prefer", "return=representation")
            conn.doOutput = true

            val updatePayload = if (primaryChannelId != null) {
                json.encodeToString(
                    SessionCredentialWithChannelPatch(
                        auth_hash = authHash,
                        encrypted_session_blob = encryptedBlob,
                        iv_vector = ivVector,
                        encryption_salt = encryptionSalt,
                        primary_channel_id = primaryChannelId
                    )
                )
            } else {
                json.encodeToString(
                    SessionCredentialPatch(
                        auth_hash = authHash,
                        encrypted_session_blob = encryptedBlob,
                        iv_vector = ivVector,
                        encryption_salt = encryptionSalt
                    )
                )
            }

            conn.outputStream.bufferedWriter().use { it.write(updatePayload) }
            val code = conn.responseCode
            if (code in 200..204) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                resp.trim().startsWith("[") && resp.trim() != "[]"
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun updateUsername(oldUsername: String, newUsername: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext false
        var conn: HttpURLConnection? = null
        try {
            val encodedOld = URLEncoder.encode(oldUsername.trim().lowercase(), "UTF-8")
            val cleanNew = newUsername.trim().lowercase().removePrefix("@")
            conn = openConnection("/kunjo_users?username=eq.$encodedOld", "PATCH")
            conn.setRequestProperty("Prefer", "return=representation")
            conn.doOutput = true

            val payload = json.encodeToString(UsernamePatch(cleanNew))
            conn.outputStream.bufferedWriter().use { it.write(payload) }
            val code = conn.responseCode
            if (code in 200..204) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                resp.trim().startsWith("[") && resp.trim() != "[]"
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun fetchUserCounts(): UserCountSummary = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext UserCountSummary(isUnavailable = true)
        var conn: HttpURLConnection? = null
        try {
            conn = openConnection("/kunjo_users?select=account_type", "GET")
            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val root = json.parseToJsonElement(responseText).jsonArray
                var mainCount = 0
                var subCount = 0
                for (item in root) {
                    val obj = item.jsonObject
                    val type = obj["account_type"]?.jsonPrimitive?.contentOrNull ?: "MAIN"
                    if (type.equals("SUB", ignoreCase = true)) subCount++ else mainCount++
                }
                val total = mainCount + subCount
                return@withContext UserCountSummary(
                    totalUsers = total,
                    mainUsers = mainCount,
                    subUsers = subCount,
                    isUnavailable = false
                )
            }
        } catch (_: Throwable) {
            // Fall through to unavailable state without reporting fabricated counts
        } finally {
            conn?.disconnect()
        }
        UserCountSummary(totalUsers = 0, mainUsers = 0, subUsers = 0, isUnavailable = true)
    }

    suspend fun fetchTopCountries(limit: Int = 5): List<CountryCount> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext emptyList()
        var conn: HttpURLConnection? = null
        try {
            conn = openConnection("/kunjo_users?select=country", "GET")
            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val root = json.parseToJsonElement(responseText).jsonArray
                val counts = linkedMapOf<String, Int>()
                for (item in root) {
                    val raw = item.jsonObject["country"]?.jsonPrimitive?.contentOrNull?.trim()
                    val normalized = if (raw.isNullOrBlank()) "Unknown" else raw
                    counts[normalized] = (counts[normalized] ?: 0) + 1
                }
                return@withContext counts.entries
                    .sortedByDescending { it.value }
                    .take(limit)
                    .map { CountryCount(it.key, it.value) }
            }
        } catch (_: Throwable) {
            // Offline / unreachable
        } finally {
            conn?.disconnect()
        }
        emptyList()
    }

    suspend fun getSubAccountPolicy(memberUserId: String): FamilyMemberPolicy? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null
        var conn: HttpURLConnection? = null
        try {
            val encodedMemberId = URLEncoder.encode(memberUserId.trim(), "UTF-8")
            conn = openConnection("/kunjo_family_members?member_user_id=eq.$encodedMemberId&select=*", "GET")
            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val array = json.parseToJsonElement(responseText).jsonArray
                val first = array.firstOrNull()?.jsonObject ?: return@withContext null
                val maxMb = first["max_stream_size_mb"]?.jsonPrimitive?.intOrNull ?: 500
                val canUpload = first["can_upload"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false
                val folder = first["allowed_folder"]?.jsonPrimitive?.contentOrNull ?: "FAMILY"
                return@withContext FamilyMemberPolicy(
                    max_stream_size_mb = maxMb,
                    can_upload = canUpload,
                    allowed_folder = folder
                )
            }
        } catch (_: Throwable) {
            // fallback
        } finally {
            conn?.disconnect()
        }
        null
    }

    internal fun encodeTelegramLinkMarker(telegramUserId: Long, telegramUsername: String?): String {
        val cleanTgUser = telegramUsername?.trim()?.removePrefix("@").orEmpty()
        return "tg:$telegramUserId:$cleanTgUser"
    }

    internal fun decodeTelegramLinkMarker(ipAddress: String?): Pair<Long, String?> {
        val raw = ipAddress?.trim().orEmpty()
        if (!raw.startsWith("tg:")) return 0L to null
        val parts = raw.split(":", limit = 3)
        val uid = parts.getOrNull(1)?.toLongOrNull()?.takeIf { it > 0L } ?: 0L
        val uname = parts.getOrNull(2)?.trim()?.removePrefix("@")?.takeIf { it.isNotBlank() }
        return uid to uname
    }

    internal fun parseDewangramSearchJson(responseText: String): List<DewanGramUserSearchResult> {
        val trimmed = responseText.trim()
        if (trimmed.isBlank()) return emptyList()
        val array = if (trimmed.startsWith("[")) {
            json.decodeFromString<List<DewanGramSearchDto>>(trimmed)
        } else {
            listOf(json.decodeFromString<DewanGramSearchDto>(trimmed))
        }
        return array.mapNotNull { dto ->
            val handle = dto.dewangram_username?.takeIf { it.isNotBlank() }
                ?: dto.username?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val rowId = dto.id?.takeIf { it.isNotBlank() } ?: handle
            val (markerId, markerTgUsername) = decodeTelegramLinkMarker(dto.ip_address)
            val resolvedTgId = dto.telegram_user_id?.takeIf { it > 0L } ?: markerId
            val resolvedTgUsername = dto.telegram_username?.trim()?.removePrefix("@")?.takeIf { it.isNotEmpty() }
                ?: markerTgUsername
            DewanGramUserSearchResult(
                id = rowId,
                dewangramUsername = handle,
                telegramUserId = resolvedTgId,
                telegramUsername = resolvedTgUsername
            )
        }
    }

    private fun patchRepresentation(endpoint: String, payload: String): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            conn = openConnection(endpoint, "PATCH")
            conn.setRequestProperty("Prefer", "return=representation")
            conn.doOutput = true
            conn.outputStream.bufferedWriter().use { it.write(payload) }
            if (conn.responseCode !in 200..299) return false
            val trimmed = conn.inputStream.bufferedReader().use { it.readText() }.trim()
            trimmed.startsWith("[") && trimmed != "[]"
        } catch (_: Throwable) {
            false
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Find a user by their DewanGram username (with fallback to primary Kunjo username column).
     * Returns the user's KunjoUserDto if found, null otherwise.
     */
    suspend fun getUserByDewangramUsername(dewangramUsername: String): KunjoUserDto? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null
        val encoded = URLEncoder.encode(dewangramUsername.trim().lowercase().removePrefix("@"), "UTF-8")
        val endpoints = listOf(
            "/kunjo_users?dewangram_username=eq.$encoded&select=*",
            "/kunjo_users?username=eq.$encoded&select=*"
        )
        for (endpoint in endpoints) {
            var conn: HttpURLConnection? = null
            try {
                conn = openConnection(endpoint, "GET")
                if (conn.responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val found = json.decodeFromString<List<KunjoUserDto>>(responseText).firstOrNull()
                    if (found != null) {
                        val (markerId, _) = decodeTelegramLinkMarker(found.ip_address)
                        return@withContext if (found.telegram_user_id == null && markerId > 0L) {
                            found.copy(telegram_user_id = markerId)
                        } else {
                            found
                        }
                    }
                    return@withContext null
                }
            } catch (_: Throwable) {
                // try fallback query
            } finally {
                conn?.disconnect()
            }
        }
        null
    }

    /**
     * Search DewanGram users by username/dewangram_username substring for user discovery.
     * Falls back to `username` + `ip_address` marker when extended DewanGram columns are absent.
     */
    suspend fun searchDewangramUsers(query: String): List<DewanGramUserSearchResult> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim().lowercase().removePrefix("@")
        if (!isConfigured || cleanQuery.length < 2) return@withContext emptyList()
        // Sanitize: escape SQL wildcards _ and % which would match unintended results.
        // URLEncoder only encodes URL special chars, not SQL wildcards.
        val sanitized = cleanQuery
            .replace("_", "\\_")
            .replace("%", "\\%")
        if (sanitized.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(sanitized, "UTF-8")
        val endpoints = listOf(
            "/kunjo_users?or=(dewangram_username.ilike.*$encoded*,username.ilike.*$encoded*)&select=id,username,dewangram_username,telegram_user_id,ip_address",
            "/kunjo_users?username=ilike.*$encoded*&select=id,username,ip_address"
        )
        for (endpoint in endpoints) {
            var conn: HttpURLConnection? = null
            try {
                conn = openConnection(endpoint, "GET")
                if (conn.responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    return@withContext parseDewangramSearchJson(responseText)
                }
            } catch (_: Throwable) {
                // try fallback query
            } finally {
                conn?.disconnect()
            }
        }
        emptyList()
    }

    /**
     * Link a Telegram user ID (and optional Telegram username) to an existing Kunjo user.
     * Falls back to storing a `tg:<id>:<username>` marker in `ip_address` when the dedicated
     * `telegram_user_id` column is not present on the live table.
     */
    suspend fun linkTelegramUserId(
        kunjoUsername: String,
        telegramUserId: Long,
        telegramUsername: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured || telegramUserId <= 0L) return@withContext false
        val cleanUser = kunjoUsername.trim().lowercase().removePrefix("@")
        if (cleanUser.isBlank()) return@withContext false
        val encoded = URLEncoder.encode(cleanUser, "UTF-8")
        val endpoint = "/kunjo_users?username=eq.$encoded"

        val primaryPayload = json.encodeToString(
            TelegramLinkPatch(
                telegram_user_id = telegramUserId,
                telegram_linked_at = System.currentTimeMillis()
            )
        )
        val ipPayload = json.encodeToString(
            IpAddressTelegramLinkPatch(ip_address = encodeTelegramLinkMarker(telegramUserId, telegramUsername))
        )

        val primaryOk = patchRepresentation(endpoint, primaryPayload)
        if (primaryOk) {
            if (!telegramUsername.isNullOrBlank()) {
                patchRepresentation(endpoint, ipPayload)
            }
            return@withContext true
        }
        patchRepresentation(endpoint, ipPayload)
    }

    /**
     * Register or update a DewanGram username for a Kunjo user.
     * Returns true only if the username was set/updated successfully.
     */
    suspend fun setDewangramUsername(kunjoUsername: String, dewangramUsername: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext false
        var conn: HttpURLConnection? = null
        try {
            val encoded = URLEncoder.encode(kunjoUsername.trim().lowercase(), "UTF-8")
            conn = openConnection("/kunjo_users?username=eq.$encoded", "PATCH")
            conn.setRequestProperty("Prefer", "return=representation")
            conn.doOutput = true
            val payload = json.encodeToString(DewangramUsernamePatch(dewangram_username = dewangramUsername.trim().lowercase()))
            conn.outputStream.bufferedWriter().use { it.write(payload) }
            val code = conn.responseCode
            if (code !in 200..299) return@withContext false
            // Verify at least one row was actually updated
            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val trimmed = responseText.trim()
            trimmed.startsWith("[") && trimmed != "[]"
        } catch (_: Throwable) {
            false
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Update last_seen_at timestamp for a user.
     */
    suspend fun updateLastSeen(kunjoUsername: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext false
        var conn: HttpURLConnection? = null
        try {
            val encoded = URLEncoder.encode(kunjoUsername.trim().lowercase(), "UTF-8")
            conn = openConnection("/kunjo_users?username=eq.$encoded", "PATCH")
            conn.doOutput = true
            val payload = json.encodeToString(LastSeenPatch(last_seen_at = System.currentTimeMillis()))
            conn.outputStream.bufferedWriter().use { it.write(payload) }
            conn.responseCode in 200..299
        } catch (_: Throwable) {
            false
        } finally {
            conn?.disconnect()
        }
    }
}
