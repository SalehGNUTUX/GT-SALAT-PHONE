package io.github.salehgnutux.gtsalat.audio

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** أوامر تشغيل الإذاعة من الواجهة. */
object RadioAudio {
    fun play(ctx: Context, name: String, url: String) {
        val i = Intent(ctx, RadioService::class.java).apply {
            action = RadioService.ACTION_START
            putExtra(RadioService.EXTRA_NAME, name)
            putExtra(RadioService.EXTRA_URL, url)
        }
        ContextCompat.startForegroundService(ctx, i)
    }

    fun toggle(ctx: Context) = command(ctx, RadioService.ACTION_TOGGLE)
    fun stop(ctx: Context) = command(ctx, RadioService.ACTION_STOP)

    private fun command(ctx: Context, action: String) =
        ContextCompat.startForegroundService(ctx, Intent(ctx, RadioService::class.java).setAction(action))
}
