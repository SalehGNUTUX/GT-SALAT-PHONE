package io.github.salehgnutux.gtsalat.ui.location

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import io.github.salehgnutux.gtsalat.util.Connectivity

/**
 * نكهة full: يعمل الإجراء فوراً إن كانت خدمة الموقع مفعَّلة، وإلّا تظهر **نافذة النظام** لتفعيلها
 * (خدمات Google)، ويُتابَع الإجراء عند الموافقة. وإن تعذّرت النافذة فصفحة الموقع في الإعدادات.
 */
@Composable
fun rememberLocationGate(): ((() -> Unit) -> Unit) {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun finish(enabled: Boolean) {
        val action = pending
        pending = null
        if (enabled) action?.invoke()
    }

    val resolve = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        finish(it.resultCode == Activity.RESULT_OK || Connectivity.hasGps(context))
    }
    val settings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        finish(Connectivity.hasGps(context))
    }
    return remember {
        { action ->
            if (Connectivity.hasGps(context)) action()
            else {
                pending = action
                val request = LocationSettingsRequest.Builder()
                    .addLocationRequest(LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000L).build())
                    .setAlwaysShow(true)
                    .build()
                LocationServices.getSettingsClient(context).checkLocationSettings(request)
                    .addOnSuccessListener { finish(true) }
                    .addOnFailureListener { e ->
                        val shown = e is ResolvableApiException && runCatching {
                            resolve.launch(IntentSenderRequest.Builder(e.resolution).build())
                        }.isSuccess
                        if (!shown) {
                            runCatching { settings.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                                .onFailure { finish(false) }
                        }
                    }
            }
        }
    }
}
