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
    /** [allowIpFallback]=false لمسار الخلفيّة: موقع IP تقريبيٌّ فلا يحلّ بصمتٍ محلّ موقعٍ دقيق. */
    suspend fun detect(allowIpFallback: Boolean = true): DetectedLocation?
}
