package io.github.salehgnutux.gtsalat.audio

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** أوامرُ تشغيل صوت الأذكار (يُرسل نيّاتٍ إلى [AdhkarAudioService]). */
object AdhkarAudio {

    /** يبدأ تشغيل تسجيلٍ مضمَّن للأذكار. [key]: morning/evening/sleep · [route]: وجهة القسم للعودة إليه. */
    fun play(context: Context, key: String, title: String, route: String, rawResId: Int) {
        val i = Intent(context, AdhkarAudioService::class.java).apply {
            action = AdhkarAudioService.ACTION_START
            putExtra(AdhkarAudioService.EXTRA_KEY, key)
            putExtra(AdhkarAudioService.EXTRA_TITLE, title)
            putExtra(AdhkarAudioService.EXTRA_ROUTE, route)
            putExtra(AdhkarAudioService.EXTRA_RAW, rawResId)
        }
        runCatching { ContextCompat.startForegroundService(context, i) }
    }

    fun toggle(context: Context) = send(context, AdhkarAudioService.ACTION_TOGGLE)
    fun stop(context: Context) = send(context, AdhkarAudioService.ACTION_STOP)
    fun toggleRepeat(context: Context) = send(context, AdhkarAudioService.ACTION_REPEAT)

    fun seek(context: Context, positionMs: Int) {
        val i = Intent(context, AdhkarAudioService::class.java).apply {
            action = AdhkarAudioService.ACTION_SEEK
            putExtra(AdhkarAudioService.EXTRA_POSITION_MS, positionMs)
        }
        runCatching { ContextCompat.startForegroundService(context, i) }
    }

    private fun send(context: Context, action: String) {
        val i = Intent(context, AdhkarAudioService::class.java).setAction(action)
        runCatching { ContextCompat.startForegroundService(context, i) }
    }
}
