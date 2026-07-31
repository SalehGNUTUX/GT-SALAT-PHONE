package io.github.salehgnutux.gtsalat.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * بوصلة الجهاز: تبثّ زاوية اتّجاه أعلى الجهاز عن الشمال المغناطيسيّ بالدرجات (0..360)
 * عبر مستشعر متّجه الدوران (fusion). تُسجَّل المتابعة عند الاشتراك وتُلغى عند الإلغاء.
 * تُبثّ أيضاً دقّةُ المستشعر لتنبيه المستخدم بالمعايرة (حركة 8) عند انخفاضها.
 */
@Singleton
class Compass @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    /** هل في الجهاز مستشعرٌ يصلح للبوصلة؟ */
    val isAvailable: Boolean
        get() = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null ||
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null

    /** دقّةُ آخر قراءة (SensorManager.SENSOR_STATUS_*): -1 مبدئيّاً. */
    private val _accuracy = MutableStateFlow(-1)
    val accuracy: StateFlow<Int> = _accuracy.asStateFlow()

    /** زاوية الشمال المغناطيسيّ بالدرجات (0..360). */
    val azimuth: Flow<Float> = callbackFlow {
        val rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val rot = FloatArray(9)
        val orientation = FloatArray(3)
        // بعض الأجهزة تُرجع أطول من 4 قيمٍ لمتّجه الدوران؛ نمرّر أوّل 4 فقط (وإلّا مصفوفةٌ خاطئة).
        val vec = FloatArray(4)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val n = minOf(event.values.size, 4)
                System.arraycopy(event.values, 0, vec, 0, n)
                runCatching {
                    SensorManager.getRotationMatrixFromVector(rot, if (event.values.size > 4) vec else event.values)
                    SensorManager.getOrientation(rot, orientation)
                    val deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    trySend((deg + 360f) % 360f)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { _accuracy.value = accuracy }
        }

        if (rotation != null) {
            sensorManager.registerListener(listener, rotation, SensorManager.SENSOR_DELAY_GAME)
        }
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
