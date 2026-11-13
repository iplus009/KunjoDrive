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
    private data class IpApiResponse(
        val status: String? = null,
        val country: String? = null,
        val city: String? = null,
        val isp: String? = null
    )

    suspend fun fetchCurrentLocation(): GeoLocation = withContext(Dispatchers.IO) {
        try {
            val url = URI("http://ip-api.com/json/?fields=status,country,city,isp").toURL()
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val parsed = json.decodeFromString<IpApiResponse>(responseText)
                if (parsed.status == "success") {
                    return@withContext GeoLocation(
                        country = parsed.country ?: "Unknown",
                        city = parsed.city ?: "Unknown",
                        isp = parsed.isp ?: "Unknown"
                    )
                }
            }
        } catch (_: Throwable) {
            // Non-blocking fallback on rate limits or offline
        }
        GeoLocation(country = "Unknown", city = "Unknown", isp = "Unknown")
    }
}

