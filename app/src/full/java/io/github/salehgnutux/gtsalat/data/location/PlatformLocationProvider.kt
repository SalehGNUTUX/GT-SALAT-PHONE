package io.github.salehgnutux.gtsalat.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.salehgnutux.gtsalat.data.remote.IpLocationClient
import io.github.salehgnutux.gtsalat.data.remote.NominatimGeocoder
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * نكهة full: اكتشاف الموقع عبر خدمات Google (Fused Location) للأدقّ والأسرع + ترميز OSM،
 * ثمّ يعود إلى كشف IP إن تعذّر ذلك.
 */
@Singleton
class PlatformLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geocoder: NominatimGeocoder,
    private val ip: IpLocationClient,
) : LocationProvider {

    override suspend fun detect(allowIpFallback: Boolean): DetectedLocation? {
        val coords = fused()
        if (coords != null) {
            val (city, country) = geocoder.reverse(coords.first, coords.second) ?: ("" to "")
            return DetectedLocation(coords.first, coords.second, city, country)
        }
        return if (allowIpFallback) ip.detect() else null
    }

    private suspend fun fused(): Pair<Double, Double>? {
        if (!hasPermission()) return null
        val client = LocationServices.getFusedLocationProviderClient(context)
        // آخر موقعٍ معروفٍ أوّلاً (فوريّ)، فإن غاب فطلبٌ نشطٌ بمهلةٍ (للمتنقّلين).
        val last = try {
            suspendCancellableCoroutine<Pair<Double, Double>?> { cont ->
                client.lastLocation
                    .addOnSuccessListener { loc -> cont.resume(loc?.let { it.latitude to it.longitude }) }
                    .addOnFailureListener { cont.resume(null) }
            }
        } catch (_: SecurityException) {
            return null
        }
        if (last != null) return last
        // طلبٌ نشطٌ لموقعٍ حاليٍّ بمهلة 15 ثانية (توازنٌ بين الدقّة والطاقة).
        return withTimeoutOrNull(15_000L) {
            val cts = CancellationTokenSource()
            try {
                suspendCancellableCoroutine<Pair<Double, Double>?> { cont ->
                    cont.invokeOnCancellation { cts.cancel() }
                    client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                        .addOnSuccessListener { loc -> cont.resume(loc?.let { it.latitude to it.longitude }) }
                        .addOnFailureListener { cont.resume(null) }
                }
            } catch (_: SecurityException) {
                null
            }
        }
    }

    private fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }
}
