package io.github.salehgnutux.gtsalat.audio

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import io.github.salehgnutux.gtsalat.domain.Reciter

/** واجهةٌ مختصرةٌ لإرسال الأوامر إلى [QuranAudioService] من شاشات القرآن. */
object QuranAudio {

    /** تلاوةٌ آية-بآية متزامنة مع النصّ (الوضع النصّيّ) — المصدر مجلّد everyayah. */
    fun playAyat(ctx: Context, surah: Int, surahName: String, verses: Int, reciter: Reciter, startAyah: Int = 1) =
        start(ctx, QuranAudioService.MODE_AYAH, surah, surahName, verses, reciter.everyayah, reciter.id, reciter.ar, startAyah)

    /** تلاوة سورةٍ كاملة (القرآن المسموع) — المصدر رابط خادمٍ كاملٍ من mp3quran. */
    fun playSurah(ctx: Context, surah: Int, surahName: String, reciterId: String, reciterName: String, server: String) =
        start(ctx, QuranAudioService.MODE_SURAH, surah, surahName, 0, server, reciterId, reciterName, 1)

    private fun start(
        ctx: Context, mode: String, surah: Int, surahName: String, verses: Int,
        folder: String, reciterId: String, reciterName: String, startAyah: Int,
    ) {
        val i = Intent(ctx, QuranAudioService::class.java).apply {
            action = QuranAudioService.ACTION_START
            putExtra(QuranAudioService.EXTRA_MODE, mode)
            putExtra(QuranAudioService.EXTRA_SURAH, surah)
            putExtra(QuranAudioService.EXTRA_SURAH_NAME, surahName)
            putExtra(QuranAudioService.EXTRA_VERSES, verses)
            putExtra(QuranAudioService.EXTRA_FOLDER, folder)
            putExtra(QuranAudioService.EXTRA_RECITER_ID, reciterId)
            putExtra(QuranAudioService.EXTRA_RECITER_NAME, reciterName)
            putExtra(QuranAudioService.EXTRA_AYAH, startAyah)
        }
        ContextCompat.startForegroundService(ctx, i)
    }

    fun toggle(ctx: Context) = command(ctx, QuranAudioService.ACTION_TOGGLE)
    fun stop(ctx: Context) = command(ctx, QuranAudioService.ACTION_STOP)
    fun next(ctx: Context) = command(ctx, QuranAudioService.ACTION_NEXT)
    fun prev(ctx: Context) = command(ctx, QuranAudioService.ACTION_PREV)

    fun seekAyah(ctx: Context, ayah: Int) {
        val i = Intent(ctx, QuranAudioService::class.java)
            .setAction(QuranAudioService.ACTION_SEEK_AYAH)
            .putExtra(QuranAudioService.EXTRA_AYAH, ayah)
        ContextCompat.startForegroundService(ctx, i)
    }

    private fun command(ctx: Context, action: String) {
        val i = Intent(ctx, QuranAudioService::class.java).setAction(action)
        ContextCompat.startForegroundService(ctx, i)
    }
}
