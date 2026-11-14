package com.novo.drive.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URI

@Serializable
data class GeoLocation(
    val country: String = "Unknown",
    val city: String = "Unknown",
    val isp: String = "Unknown"
)

object GeoAnalyticsClient {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class IpWhoIsConnection(
        val isp: String? = null
    )

    @Serializable
    private data class IpWhoIsResponse(
        val success: Boolean = false,
        val country: String? = null,
        val city: String? = null,
        val connection: IpWhoIsConnection? = null
    )

    suspend fun fetchCurrentLocation(): GeoLocation = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            // Use HTTPS so Android's default network security config (cleartext disabled) permits the request.
            val url = URI("https://ipwho.is/").toURL()
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val parsed = json.decodeFromString<IpWhoIsResponse>(responseText)
                if (parsed.success) {
                    return@withContext GeoLocation(
                        country = parsed.country?.takeIf { it.isNotBlank() } ?: "Unknown",
                        city = parsed.city?.takeIf { it.isNotBlank() } ?: "Unknown",
                        isp = parsed.connection?.isp?.takeIf { it.isNotBlank() } ?: "Unknown"
                    )
                }
            }
        } catch (_: Throwable) {
            // Non-blocking fallback on rate limits or offline
        } finally {
            conn?.disconnect()
        }
        GeoLocation(country = "Unknown", city = "Unknown", isp = "Unknown")
    }
}

