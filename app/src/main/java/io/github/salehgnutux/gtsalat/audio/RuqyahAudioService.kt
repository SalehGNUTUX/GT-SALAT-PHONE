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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * تشغيل الرقية المسموعة كقائمةٍ من الآيات (everyayah) عبر خدمةٍ مقدّمة — تعمل في الخلفيّة
 * والشاشة مقفلة. تدعم: تشغيل/إيقاف مؤقّت · سابق/تالٍ · تكرار المقطع · شريط تقدّم. الحالة في [RuqyahPlayback].
 */
@AndroidEntryPoint
class RuqyahAudioService : Service() {

    @Inject lateinit var notifications: NotificationHelper

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ticker: Job? = null
    private var player: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    private var surahs = IntArray(0)
    private var ayahs = IntArray(0)
    private var labels = arrayOf<String>()
    private var index = 0
    private var repeatMode = 0   // 0 بلا · 1 المقطع · 2 الكلّ

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> stopEverything()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
            AudioManager.AUDIOFOCUS_GAIN -> if (RuqyahPlayback.state.value.active && !isPlaying()) resume()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopEverything(); return START_NOT_STICKY }
            ACTION_TOGGLE -> { toggle(); return START_STICKY }
            ACTION_NEXT -> { skip(+1); return START_STICKY }
            ACTION_PREV -> { skip(-1); return START_STICKY }
            ACTION_REPEAT -> { repeatMode = (repeatMode + 1) % 3; publish(); return START_STICKY }
        }
        // ACTION_START
        surahs = intent?.getIntArrayExtra(EXTRA_SURAHS) ?: IntArray(0)
        ayahs = intent?.getIntArrayExtra(EXTRA_AYAHS) ?: IntArray(0)
        labels = intent?.getStringArrayExtra(EXTRA_LABELS) ?: Array(surahs.size) { "" }
        index = (intent?.getIntExtra(EXTRA_INDEX, 0) ?: 0).coerceIn(0, maxOf(0, surahs.size - 1))
        if (surahs.isEmpty()) { stopSelf(); return START_NOT_STICKY }

        startForeground(NotificationHelper.ID_RUQYAH, buildNotification())
        requestFocus()
        playCurrent()
        return START_STICKY
    }

    private fun playCurrent() {
        if (index !in surahs.indices) { stopEverything(); return }
        val url = Quran.ayahAudioUrl(RECITER, surahs[index], ayahs[index])
        publish(loading = true, playing = true)
        releasePlayer()
        player = MediaPlayer().apply {
            setAudioAttributes(attrs)
            setWakeMode(this@RuqyahAudioService, PowerManager.PARTIAL_WAKE_LOCK)
            val ok = runCatching { setDataSource(url) }.isSuccess
            if (!ok) { onTrackEnd(); return }
            setOnPreparedListener { start(); publish(loading = false, playing = true); updateNotification(); startTicker() }
            setOnCompletionListener { onTrackEnd() }
            setOnErrorListener { _, _, _ -> onTrackEnd(); true }
            prepareAsync()
        }
    }

    private fun onTrackEnd() {
        if (repeatMode == 1) { playCurrent(); return }              // تكرار المقطع
        if (index < surahs.size - 1) { index++; playCurrent() }
        else if (repeatMode == 2) { index = 0; playCurrent() }      // تكرار الكلّ من البداية
        else stopEverything()
    }

    private fun skip(delta: Int) {
        val next = (index + delta).coerceIn(0, surahs.size - 1)
        if (next == index && delta > 0) return
        index = next
        playCurrent(); updateNotification()
    }

    private fun toggle() { if (isPlaying()) pause() else resume() }

    private fun pause() { runCatching { player?.pause() }; publish(playing = false); updateNotification() }
    private fun resume() { runCatching { player?.start() }; publish(playing = true); updateNotification() }
    private fun isPlaying(): Boolean = runCatching { player?.isPlaying == true }.getOrDefault(false)

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                delay(500)
                val p = runCatching { player?.currentPosition ?: 0 }.getOrDefault(0)
                val d = runCatching { player?.duration ?: 0 }.getOrDefault(0)
                RuqyahPlayback.update { it.copy(posMs = p, durMs = d) }
            }
        }
    }

    private fun publish(loading: Boolean? = null, playing: Boolean? = null) {
        RuqyahPlayback.update {
            it.copy(
                active = true,
                index = index,
                total = surahs.size,
                surah = surahs.getOrElse(index) { 0 },
                ayah = ayahs.getOrElse(index) { 0 },
                label = labels.getOrElse(index) { "" },
                isPlaying = playing ?: it.isPlaying,
                loading = loading ?: it.loading,
                repeatMode = repeatMode,
            )
        }
    }

    private fun buildNotification() =
        notifications.serviceNotification("🌿 الرقية — ${labels.getOrElse(index) { "" }}", stopPendingIntent())

    private fun updateNotification() = notifications.notify(NotificationHelper.ID_RUQYAH, buildNotification())

    private fun requestFocus() {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager = am
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs).setOnAudioFocusChangeListener(focusListener).build()
            focusRequest = req
            am.requestAudioFocus(req)
        }
    }

    private fun abandonFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) focusRequest?.let { am.abandonAudioFocusRequest(it) }
    }

    private fun releasePlayer() {
        ticker?.cancel()
        player?.run { runCatching { if (isPlaying) stop() }; release() }
        player = null
    }

    private fun stopEverything() {
        releasePlayer(); abandonFocus()
        RuqyahPlayback.reset()
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }

    override fun onDestroy() { scope.cancel(); releasePlayer(); abandonFocus(); super.onDestroy() }

    private fun stopPendingIntent(): PendingIntent {
        val i = Intent(this, RuqyahAudioService::class.java).setAction(ACTION_STOP)
        return PendingIntent.getService(this, 9, i, NotificationHelper.PI_FLAGS)
    }

    companion object {
        const val ACTION_START = "io.github.salehgnutux.gtsalat.RUQYAH_START"
        const val ACTION_STOP = "io.github.salehgnutux.gtsalat.RUQYAH_STOP"
        const val ACTION_TOGGLE = "io.github.salehgnutux.gtsalat.RUQYAH_TOGGLE"
        const val ACTION_NEXT = "io.github.salehgnutux.gtsalat.RUQYAH_NEXT"
        const val ACTION_PREV = "io.github.salehgnutux.gtsalat.RUQYAH_PREV"
        const val ACTION_REPEAT = "io.github.salehgnutux.gtsalat.RUQYAH_REPEAT"
        const val EXTRA_SURAHS = "surahs"
        const val EXTRA_AYAHS = "ayahs"
        const val EXTRA_LABELS = "labels"
        const val EXTRA_INDEX = "index"
        private const val RECITER = "Alafasy_128kbps"   // مشاري العفاسي (everyayah)
    }
}
