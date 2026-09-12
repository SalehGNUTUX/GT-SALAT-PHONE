package io.github.salehgnutux.gtsalat.util

import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/**
 * فحوصٌ خفيفةٌ لتوفّر مصادر تحديد الموقع، تُستعمل لبوّابة تحديث الموقع التلقائيّ:
 * لا معنى لبذل الجهد إن غاب الإنترنت وGPS معاً. (إذن ACCESS_NETWORK_STATE معلَنٌ سلفاً.)
 */
object Connectivity {

    /** هل توجد شبكةٌ فعّالةٌ ذات اتّصالٍ بالإنترنت (Wi‑Fi أو بيانات)؟ */
    fun hasInternet(context: Context): Boolean = runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
    }.getOrDefault(false)

    /** هل مزوّد GPS مفعَّلٌ (فيمكن تحديدٌ محلّيٌّ دون إنترنت)؟ */
    fun hasGps(context: Context): Boolean = runCatching {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }.getOrDefault(false)

    /** بوّابة تحديث الموقع التلقائيّ: إنترنت أو GPS متاح. */
    fun canLocate(context: Context): Boolean = hasInternet(context) || hasGps(context)
}
