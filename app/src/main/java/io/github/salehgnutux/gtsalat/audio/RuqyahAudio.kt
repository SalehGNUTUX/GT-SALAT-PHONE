package io.github.salehgnutux.gtsalat.audio

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** واجهةٌ مختصرةٌ لإرسال أوامر تشغيل الرقية المسموعة إلى [RuqyahAudioService]. */
object RuqyahAudio {

    /** تشغيل قائمة آياتٍ (متوازية: السور/الآيات/العناوين) بدءًا من [startIndex]. */
    fun play(ctx: Context, surahs: IntArray, ayahs: IntArray, labels: Array<String>, startIndex: Int = 0) {
        val i = Intent(ctx, RuqyahAudioService::class.java).apply {
            action = RuqyahAudioService.ACTION_START
            putExtra(RuqyahAudioService.EXTRA_SURAHS, surahs)
            putExtra(RuqyahAudioService.EXTRA_AYAHS, ayahs)
            putExtra(RuqyahAudioService.EXTRA_LABELS, labels)
            putExtra(RuqyahAudioService.EXTRA_INDEX, startIndex)
        }
        ContextCompat.startForegroundService(ctx, i)
    }

    fun toggle(ctx: Context) = command(ctx, RuqyahAudioService.ACTION_TOGGLE)
    fun next(ctx: Context) = command(ctx, RuqyahAudioService.ACTION_NEXT)
    fun prev(ctx: Context) = command(ctx, RuqyahAudioService.ACTION_PREV)
    fun stop(ctx: Context) = command(ctx, RuqyahAudioService.ACTION_STOP)
    fun toggleRepeat(ctx: Context) = command(ctx, RuqyahAudioService.ACTION_REPEAT)

    private fun command(ctx: Context, action: String) {
        val i = Intent(ctx, RuqyahAudioService::class.java).setAction(action)
        ContextCompat.startForegroundService(ctx, i)
    }
}
