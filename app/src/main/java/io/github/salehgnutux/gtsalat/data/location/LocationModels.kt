package io.github.salehgnutux.gtsalat.data.location

/** موقعٌ مكتشَف مع اسم المدينة والبلد. */
data class DetectedLocation(
    val lat: Double,
    val lon: Double,
    val city: String,
    val country: String,
)

/**
 * تجريد اكتشاف الموقع. له تطبيقان بحسب النكهة:
 * foss (LocationManager + OSM) و full (Fused Location). كلاهما يعود إلى كشف IP.
 */
interface LocationProvider {
    suspend fun detect(): DetectedLocation?
}
