package io.github.salehgnutux.gtsalat.audio

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import dagger.hilt.android.AndroidEntryPoint
import io.github.salehgnutux.gtsalat.domain.Quran
import io.github.salehgnutux.gtsalat.notification.NotificationHelper
import javax.inject.Inject

/**
 * تشغيل تلاوة القرآن عبر خدمة مقدّمة (mediaPlayback) بـ MediaPlayer — تعمل في الخلفيّة
 * والشاشة مغلقة. وضعان: آية-بآية (متزامن مع النصّ) وسورة كاملة (يتابع تلقائيّاً للسورة التالية).
 * الصوت يُبثّ من الشبكة (everyayah / mp3quran) ويُخزَّن في كاش النظام. الحالة تُنشَر عبر [QuranPlayback].
 */
@AndroidEntryPoint
class QuranAudioService : Service() {

    @Inject lateinit var notifications: NotificationHelper
    @Inject lateinit var downloader: io.github.salehgnutux.gtsalat.data.QuranDownloader

    private var player: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    // حالة التشغيل الداخليّة
    private var mode = QuranMode.AYAH
    private var surah = 1
    private var surahName = ""
    private var reciterId = ""
    private var reciterName = ""
    private var folder = ""
    private var verses = 0
    private var curAyah = 1
    private var playingBasmala = false

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> stopEverything()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
            AudioManager.AUDIOFOCUS_GAIN -> if (QuranPlayback.state.value.active && !isPlaying()) resume()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopEverything(); return START_NOT_STICKY }
            ACTION_TOGGLE -> { toggle(); return START_STICKY }
            ACTION_NEXT -> { skipSurah(+1); return START_STICKY }
            ACTION_PREV -> { skipSurah(-1); return START_STICKY }
            ACTION_SEEK_AYAH -> {
                curAyah = intent.getIntExtra(EXTRA_AYAH, curAyah).coerceIn(1, maxOf(1, verses))
                playingBasmala = false
                playCurrent()
                return START_STICKY
            }
        }
        // ACTION_START
        mode = if (intent?.getStringExtra(EXTRA_MODE) == MODE_SURAH) QuranMode.SURAH else QuranMode.AYAH
        surah = intent?.getIntExtra(EXTRA_SURAH, 1) ?: 1
        surahName = intent?.getStringExtra(EXTRA_SURAH_NAME) ?: "سورة $surah"
        reciterId = intent?.getStringExtra(EXTRA_RECITER_ID) ?: ""
        reciterName = intent?.getStringExtra(EXTRA_RECITER_NAME) ?: ""
        folder = intent?.getStringExtra(EXTRA_FOLDER) ?: ""
        verses = intent?.getIntExtra(EXTRA_VERSES, 0) ?: 0
        curAyah = (intent?.getIntExtra(EXTRA_AYAH, 1) ?: 1).coerceAtLeast(1)

        startForeground(NotificationHelper.ID_RECITATION, buildNotification())
        requestFocus()

        // بسملةٌ قبل الآية الأولى (عدا الفاتحة والتوبة) في الوضع النصّيّ.
        playingBasmala = mode == QuranMode.AYAH && curAyah == 1 && surah != 1 && surah != 9
        publish(loading = true, playing = true, ayah = if (playingBasmala) 0 else curAyah)
        playCurrent()
        return START_STICKY
    }

    /** يشغّل الرابط الحاليّ حسب الوضع وحالة البسملة. */
    private fun playCurrent() {
        val url = when {
            // في التلاوة الكاملة نستعمل الملفّ المحلّيّ إن نُزِّل (دون إنترنت).
            mode == QuranMode.SURAH ->
                downloader.localSurah(reciterId, surah)?.absolutePath ?: Quran.surahAudioUrl(folder, surah)
            playingBasmala -> Quran.basmalaUrl(folder)
            else -> Quran.ayahAudioUrl(folder, surah, curAyah)
        }
        publish(
            loading = true,
            playing = true,
            ayah = if (mode == QuranMode.AYAH && !playingBasmala) curAyah else if (playingBasmala) 0 else curAyah,
        )
        releasePlayer()
        player = MediaPlayer().apply {
            setAudioAttributes(attrs)
            setWakeMode(this@QuranAudioService, PowerManager.PARTIAL_WAKE_LOCK)
            val ok = runCatching { setDataSource(url) }.isSuccess
            if (!ok) { onTrackEnd(); return }
            setOnPreparedListener {
                start()
                publish(loading = false, playing = true)
                updateNotification()
            }
            setOnCompletionListener { onTrackEnd() }
            setOnErrorListener { _, _, _ -> onTrackEnd(); true }
            prepareAsync()
        }
    }

    /** انتهاء مقطع: تقدّمٌ في التتابع حسب الوضع. */
    private fun onTrackEnd() {
        when (mode) {
            QuranMode.SURAH -> {
                if (surah < Quran.TOTAL_SURAHS) { surah++; surahName = "سورة $surah"; curAyah = 1; playCurrent(); updateNotification() }
                else stopEverything()
            }
            QuranMode.AYAH -> {
                if (playingBasmala) { playingBasmala = false; playCurrent() }
                else if (curAyah < verses) { curAyah++; playCurrent() }
                else stopEverything()
            }
        }
    }

    private fun skipSurah(delta: Int) {
        val next = (surah + delta).coerceIn(1, Quran.TOTAL_SURAHS)
        if (next == surah) return
        surah = next; surahName = "سورة $surah"; curAyah = 1
        playingBasmala = mode == QuranMode.AYAH && surah != 1 && surah != 9
        playCurrent(); updateNotification()
    }

    private fun toggle() { if (isPlaying()) pause() else resume() }

    private fun pause() {
        runCatching { player?.pause() }
        publish(playing = false)
        updateNotification()
    }

    private fun resume() {
        runCatching { player?.start() }
        publish(playing = true)
        updateNotification()
    }

    private fun isPlaying(): Boolean = runCatching { player?.isPlaying == true }.getOrDefault(false)

    private fun publish(loading: Boolean? = null, playing: Boolean? = null, ayah: Int? = null) {
        QuranPlayback.update {
            it.copy(
                active = true,
                mode = mode,
                surah = surah,
                surahName = surahName,
                ayah = ayah ?: it.ayah,
                reciterId = reciterId,
                reciterName = reciterName,
                isPlaying = playing ?: it.isPlaying,
                loading = loading ?: it.loading,
            )
        }
    }

    private fun buildNotification() =
        notifications.serviceNotification("🎧 $surahName — $reciterName", stopPendingIntent())

    private fun updateNotification() =
        notifications.notify(NotificationHelper.ID_RECITATION, buildNotification())

    private fun requestFocus() {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager = am
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener(focusListener)
                .build()
            focusRequest = req
            am.requestAudioFocus(req)
        }
    }

    private fun abandonFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) focusRequest?.let { am.abandonAudioFocusRequest(it) }
    }

    private fun releasePlayer() {
        player?.run { runCatching { if (isPlaying) stop() }; release() }
        player = null
    }

    private fun stopEverything() {
        releasePlayer()
        abandonFocus()
        QuranPlayback.reset()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        releasePlayer()
        abandonFocus()
        super.onDestroy()
    }

    private fun stopPendingIntent(): PendingIntent {
        val i = Intent(this, QuranAudioService::class.java).setAction(ACTION_STOP)
        return PendingIntent.getService(this, 7, i, NotificationHelper.PI_FLAGS)
    }

    companion object {
        const val ACTION_START = "io.github.salehgnutux.gtsalat.QURAN_START"
        const val ACTION_STOP = "io.github.salehgnutux.gtsalat.QURAN_STOP"
        const val ACTION_TOGGLE = "io.github.salehgnutux.gtsalat.QURAN_TOGGLE"
        const val ACTION_NEXT = "io.github.salehgnutux.gtsalat.QURAN_NEXT"
        const val ACTION_PREV = "io.github.salehgnutux.gtsalat.QURAN_PREV"
        const val ACTION_SEEK_AYAH = "io.github.salehgnutux.gtsalat.QURAN_SEEK_AYAH"
        const val EXTRA_MODE = "mode"
        const val EXTRA_SURAH = "surah"
        const val EXTRA_SURAH_NAME = "surah_name"
        const val EXTRA_RECITER_ID = "reciter_id"
        const val EXTRA_RECITER_NAME = "reciter_name"
        const val EXTRA_FOLDER = "folder"
        const val EXTRA_VERSES = "verses"
        const val EXTRA_AYAH = "ayah"
        const val MODE_AYAH = "ayah"
        const val MODE_SURAH = "surah"
    }
}
