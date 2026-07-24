package io.github.salehgnutux.gtsalat.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.salehgnutux.gtsalat.data.remote.IpLocationClient
import io.github.salehgnutux.gtsalat.data.remote.NominatimGeocoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * نكهة foss: اكتشاف الموقع عبر LocationManager (بلا خدمات Google) + ترميز OSM،
 * ثمّ يعود إلى كشف IP إن تعذّر GPS. مناسب لـ F-Droid والخصوصيّة.
 */
@Singleton
class PlatformLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geocoder: NominatimGeocoder,
    private val ip: IpLocationClient,
) : LocationProvider {

    override suspend fun detect(): DetectedLocation? {
        val coords = lastKnown()
        if (coords != null) {
            val (city, country) = geocoder.reverse(coords.first, coords.second) ?: ("" to "")
            return DetectedLocation(coords.first, coords.second, city, country)
        }
        return ip.detect()
    }

    private fun lastKnown(): Pair<Double, Double>? {
        if (!hasPermission()) return null
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
            )
            providers.asSequence()
                .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
                .maxByOrNull { it.time }
                ?.let { it.latitude to it.longitude }
        } catch (_: SecurityException) {
            null
        }
    }

    private fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }
}
