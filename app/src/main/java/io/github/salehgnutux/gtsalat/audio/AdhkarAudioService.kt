package io.github.salehgnutux.gtsalat.audio

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import dagger.hilt.android.AndroidEntryPoint
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
 * تشغيل تسجيلٍ صوتيٍّ واحدٍ مضمَّن للأذكار (صباح/مساء/نوم) عبر خدمةٍ مقدّمة (mediaPlayback) —
 * تعمل في الخلفيّة والشاشة مقفلة. تدعم: تشغيل/إيقاف مؤقّت · تقديم (seek) · إيقاف. الحالة في [AdhkarPlayback].
 */
@AndroidEntryPoint
class AdhkarAudioService : Service() {

    @Inject lateinit var notifications: NotificationHelper

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ticker: Job? = null
    private var player: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    private var key = ""
    private var title = ""
    private var route = ""
    private var rawResId = 0
    private var repeat = false

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> stopEverything()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
            AudioManager.AUDIOFOCUS_GAIN -> if (AdhkarPlayback.state.value.active && !isPlaying()) resume()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopEverything(); return START_NOT_STICKY }
            ACTION_TOGGLE -> { toggle(); return START_STICKY }
            ACTION_REPEAT -> { repeat = !repeat; publish(); return START_STICKY }
            ACTION_SEEK -> { runCatching { player?.seekTo(intent.getIntExtra(EXTRA_POSITION_MS, 0)) }; publish(); return START_STICKY }
        }
        // ACTION_START
        key = intent?.getStringExtra(EXTRA_KEY) ?: ""
        title = intent?.getStringExtra(EXTRA_TITLE) ?: "الأذكار"
        route = intent?.getStringExtra(EXTRA_ROUTE) ?: ""
        rawResId = intent?.getIntExtra(EXTRA_RAW, 0) ?: 0
        if (rawResId == 0) { stopSelf(); return START_NOT_STICKY }

        startForeground(NotificationHelper.ID_ADHKAR_AUDIO, buildNotification())
        requestFocus()
        play()
        return START_STICKY
    }

    private fun play() {
        publish(loading = true, playing = true)
        releasePlayer()
        val uri = Uri.parse("android.resource://$packageName/$rawResId")
        player = MediaPlayer().apply {
            setAudioAttributes(attrs)
            setWakeMode(this@AdhkarAudioService, PowerManager.PARTIAL_WAKE_LOCK)
            val ok = runCatching { setDataSource(this@AdhkarAudioService, uri) }.isSuccess
            if (!ok) { stopEverything(); return }
            setOnPreparedListener { start(); publish(loading = false, playing = true); updateNotification(); startTicker() }
            setOnCompletionListener { if (repeat) { runCatching { seekTo(0); start() }; publish(playing = true) } else stopEverything() }
            setOnErrorListener { _, _, _ -> stopEverything(); true }
            prepareAsync()
        }
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
                AdhkarPlayback.update { it.copy(posMs = p, durMs = d) }
            }
        }
    }

    private fun publish(loading: Boolean? = null, playing: Boolean? = null) {
        AdhkarPlayback.update {
            it.copy(
                active = true, key = key, title = title, route = route, repeat = repeat,
                isPlaying = playing ?: it.isPlaying,
                loading = loading ?: it.loading,
            )
        }
    }

    private fun buildNotification() = notifications.serviceNotification("📿 $title", stopPendingIntent())
    private fun updateNotification() = notifications.notify(NotificationHelper.ID_ADHKAR_AUDIO, buildNotification())

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
        AdhkarPlayback.reset()
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }

    override fun onDestroy() { scope.cancel(); releasePlayer(); abandonFocus(); super.onDestroy() }

    private fun stopPendingIntent(): PendingIntent {
        val i = Intent(this, AdhkarAudioService::class.java).setAction(ACTION_STOP)
        return PendingIntent.getService(this, 11, i, NotificationHelper.PI_FLAGS)
    }

    companion object {
        const val ACTION_START = "io.github.salehgnutux.gtsalat.ADHKAR_AUDIO_START"
        const val ACTION_STOP = "io.github.salehgnutux.gtsalat.ADHKAR_AUDIO_STOP"
        const val ACTION_TOGGLE = "io.github.salehgnutux.gtsalat.ADHKAR_AUDIO_TOGGLE"
        const val ACTION_REPEAT = "io.github.salehgnutux.gtsalat.ADHKAR_AUDIO_REPEAT"
        const val ACTION_SEEK = "io.github.salehgnutux.gtsalat.ADHKAR_AUDIO_SEEK"
        const val EXTRA_KEY = "key"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ROUTE = "route"
        const val EXTRA_RAW = "raw"
        const val EXTRA_POSITION_MS = "position_ms"
    }
}
