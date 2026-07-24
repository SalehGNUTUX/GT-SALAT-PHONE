package io.github.salehgnutux.gtsalat.data.remote

import io.github.salehgnutux.gtsalat.data.location.DetectedLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/** ترميز جغرافيّ عكسيّ عبر OpenStreetMap (Nominatim) — بلا اعتماد على Google. */
@Singleton
class NominatimGeocoder @Inject constructor(private val http: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true }

    /** يعيد (المدينة، البلد) أو null. */
    suspend fun reverse(lat: Double, lon: Double): Pair<String, String>? = withContext(Dispatchers.IO) {
        val url = "https://nominatim.openstreetmap.org/reverse?format=jsonv2" +
            "&lat=$lat&lon=$lon&accept-language=ar&zoom=10"
        try {
            val req = Request.Builder().url(url)
                .header("User-Agent", "GT-SALAT-Android/0.1 (islamic prayer times)")
                .build()
            http.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@withContext null
                val body = res.body?.string() ?: return@withContext null
                val r = json.decodeFromString<NominatimResult>(body)
                val a = r.address ?: return@withContext null
                val city = a.city ?: a.town ?: a.village ?: a.state ?: ""
                val country = a.country ?: ""
                if (city.isBlank() && country.isBlank()) null else city to country
            }
        } catch (_: Exception) {
            null
        }
    }
}

/** كشف الموقع التقريبيّ من عنوان IP — احتياطيّ حين يتعذّر GPS. */
@Singleton
class IpLocationClient @Inject constructor(private val http: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun detect(): DetectedLocation? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("https://ipapi.co/json/").build()
            http.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@withContext null
                val body = res.body?.string() ?: return@withContext null
                val r = json.decodeFromString<IpApiResult>(body)
                val lat = r.latitude ?: return@withContext null
                val lon = r.longitude ?: return@withContext null
                DetectedLocation(lat, lon, r.city ?: "", r.country_name ?: "")
            }
        } catch (_: Exception) {
            null
        }
    }
}

@Serializable private data class NominatimResult(val address: NominatimAddress? = null)
@Serializable private data class NominatimAddress(
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val state: String? = null,
    val country: String? = null,
)
@Serializable private data class IpApiResult(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val city: String? = null,
    val country_name: String? = null,
)
