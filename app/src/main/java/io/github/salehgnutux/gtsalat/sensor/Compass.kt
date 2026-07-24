package io.github.salehgnutux.gtsalat.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * بوصلة الجهاز: تبثّ زاوية اتّجاه أعلى الجهاز عن الشمال المغناطيسيّ بالدرجات (0..360)
 * عبر مستشعر متّجه الدوران (fusion). تُسجَّل المتابعة عند الاشتراك وتُلغى عند الإلغاء.
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

    /** زاوية الشمال المغناطيسيّ بالدرجات (0..360). */
    val azimuth: Flow<Float> = callbackFlow {
        val rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val rot = FloatArray(9)
        val orientation = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rot, event.values)
                SensorManager.getOrientation(rot, orientation)
                val deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                trySend((deg + 360f) % 360f)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotation != null) {
            sensorManager.registerListener(listener, rotation, SensorManager.SENSOR_DELAY_UI)
        }
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
