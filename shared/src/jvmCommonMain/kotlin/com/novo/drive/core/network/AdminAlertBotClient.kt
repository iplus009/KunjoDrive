package com.novo.drive.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

object AdminAlertBotClient {

    private val BOT_TOKEN get() = Secrets.BOT_TOKEN
    private val ADMIN_CHAT_ID get() = Secrets.ADMIN_CHAT_ID

    suspend fun notifyNewSignup(
        username: String,
        accountType: String,
        parentUsername: String? = null,
        country: String,
        city: String,
        isp: String,
        deviceModel: String,
        totalUsers: Int,
        mainUsers: Int,
        subUsers: Int
    ) = withContext(Dispatchers.IO) {
        if (BOT_TOKEN.isBlank() || ADMIN_CHAT_ID.isBlank()) return@withContext
        var conn: HttpURLConnection? = null
        try {
            val typeTitle = if (accountType.equals("SUB", ignoreCase = true)) {
                "👨‍👩‍👧 Sub-Account (Family of @${parentUsername ?: "Admin"})"
            } else {
                "👑 Main Account (Cloud Drive Host)"
            }

            val text = buildString {
                appendLine("🚀 *নতুন কুঞ্জ ড্রাইভ অ্যাকাউন্ট!*")
                appendLine("━━━━━━━━━━━━━━━━━━━━━━")
                appendLine("👤 *ইউজারনেম:* `@$username`")
                appendLine("🏷️ *টাইপ:* $typeTitle")
                appendLine("📍 *লোকেশন:* $city, $country 🇧🇩")
                appendLine("📶 *নেটওয়ার্ক:* $isp")
                appendLine("📱 *ডিভাইস:* $deviceModel")
                appendLine("━━━━━━━━━━━━━━━━━━━━━━")
                appendLine("📊 *কুঞ্জ ড্রাইভ অ্যাকাউন্ট সামারি:*")
                appendLine("   • 👑 মেইন অ্যাকাউন্ট: $mainUsers জন")
                appendLine("   • 👨‍👩‍👧 সাব অ্যাকাউন্ট: $subUsers জন")
                appendLine("   • 👥 মোট সর্বমোট ইউজার: $totalUsers জন")
                append("━━━━━━━━━━━━━━━━━━━━━━")
            }

            val encodedText = URLEncoder.encode(text, "UTF-8")
            val endpoint = "https://api.telegram.org/bot$BOT_TOKEN/sendMessage?chat_id=$ADMIN_CHAT_ID&text=$encodedText&parse_mode=Markdown"

            val url = URI(endpoint).toURL()
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.outputStream.close()

            // Read response silently to flush stream
            conn.responseCode
        } catch (_: Throwable) {
            // Failure to send bot alert should never block or crash user app
        } finally {
            conn?.disconnect()
        }
    }
}

