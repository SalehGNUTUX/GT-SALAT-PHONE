package io.github.salehgnutux.gtsalat.ui.screens

import android.hardware.GeomagneticField
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import io.github.salehgnutux.gtsalat.domain.PrayerCalculator
import io.github.salehgnutux.gtsalat.sensor.Compass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QiblaUi(
    val hasLocation: Boolean = false,
    val sensorAvailable: Boolean = true,
    val city: String = "",
    /** اتّجاه القبلة عن الشمال الحقيقيّ بالدرجات. */
    val qiblaBearing: Float = 0f,
    /** اتّجاه الجهاز عن الشمال الحقيقيّ بالدرجات (مُصحَّح بالانحراف المغناطيسيّ). */
    val deviceBearing: Float = 0f,
) {
    /** زاوية دوران سهم القبلة على الشاشة: موجبٌ إلى اليمين. */
    val arrowAngle: Float get() = ((qiblaBearing - deviceBearing) + 360f) % 360f
    /** هل يواجه الجهاز القبلة الآن (ضمن هامشٍ صغير)؟ */
    val aligned: Boolean get() = arrowAngle <= ALIGN_TOLERANCE || arrowAngle >= 360f - ALIGN_TOLERANCE

    companion object { const val ALIGN_TOLERANCE = 5f }
}

@HiltViewModel
class QiblaViewModel @Inject constructor(
    settingsRepo: SettingsRepository,
    private val compass: Compass,
) : ViewModel() {

    private val _ui = MutableStateFlow(QiblaUi(sensorAvailable = compass.isAvailable))
    val ui: StateFlow<QiblaUi> = _ui.asStateFlow()

    private var declination = 0f

    init {
        viewModelScope.launch {
            val s = settingsRepo.current()
            val lat = s.lat
            val lon = s.lon
            if (lat != null && lon != null) {
                val bearing = PrayerCalculator.qiblaDirection(lat, lon).toFloat()
                declination = GeomagneticField(
                    lat.toFloat(), lon.toFloat(), 0f, System.currentTimeMillis(),
                ).declination
                _ui.value = _ui.value.copy(hasLocation = true, city = s.city, qiblaBearing = bearing)
            }
        }
        viewModelScope.launch {
            var smoothed = Float.NaN
            compass.azimuth.collect { magnetic ->
                val trueNorth = (magnetic + declination + 360f) % 360f
                smoothed = if (smoothed.isNaN()) trueNorth else lowPass(smoothed, trueNorth)
                _ui.value = _ui.value.copy(deviceBearing = smoothed)
            }
        }
    }

    /** تنعيمٌ بمعالجة التفاف الزاوية (359°→0°) لتقليل الاهتزاز. */
    private fun lowPass(prev: Float, next: Float, alpha: Float = 0.18f): Float {
        val diff = ((next - prev + 540f) % 360f) - 180f
        return (prev + alpha * diff + 360f) % 360f
    }
}
