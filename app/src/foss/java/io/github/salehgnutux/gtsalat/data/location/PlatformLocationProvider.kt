package io.github.salehgnutux.gtsalat.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.salehgnutux.gtsalat.data.remote.IpLocationClient
import io.github.salehgnutux.gtsalat.data.remote.NominatimGeocoder
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

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
        // آخر موقعٍ مخزّن أوّلاً (فوريّ)، وإلّا **تثبيتٌ حيٌّ من GPS** (يعمل دون إنترنت)، وإلّا كشف IP.
        val coords = lastKnown() ?: freshFix()
        if (coords != null) {
            // اسم المدينة يحتاج إنترنت (Nominatim)؛ إن تعذّر نُبقي الإحداثيّات فقط.
            val (city, country) = geocoder.reverse(coords.first, coords.second) ?: ("" to "")
            return DetectedLocation(coords.first, coords.second, city, country)
        }
        return ip.detect()
    }

    /** يطلب تثبيتاً حيّاً واحداً من GPS/الشبكة (بمهلة)، يعمل دون إنترنت بإحداثيّات الأقمار. */
    private suspend fun freshFix(): Pair<Double, Double>? {
        if (!hasPermission()) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        if (providers.isEmpty()) return null
        return withTimeoutOrNull(30_000L) {
            suspendCancellableCoroutine { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(loc: Location) {
                        runCatching { lm.removeUpdates(this) }
                        if (cont.isActive) cont.resume(loc.latitude to loc.longitude)
                    }
                    override fun onProviderDisabled(provider: String) {}
                    override fun onProviderEnabled(provider: String) {}
                    @Deprecated("legacy") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                }
                try {
                    providers.forEach { p -> lm.requestLocationUpdates(p, 0L, 0f, listener, Looper.getMainLooper()) }
                } catch (_: SecurityException) {
                    if (cont.isActive) cont.resume(null)
                }
                cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }
            }
        }
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
