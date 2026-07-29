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
import io.github.salehgnutux.gtsalat.notification.NotificationHelper
import javax.inject.Inject

/**
 * تشغيل إذاعةٍ حيّةٍ عبر خدمةٍ مقدّمة (mediaPlayback) بـ MediaPlayer — تعمل في الخلفيّة والشاشة مغلقة.
 * البثّ مباشرٌ من رابط الإذاعة (قد يكون HTTP، فيُسمح cleartext في المانيفست).
 */
@AndroidEntryPoint
class RadioService : Service() {

    @Inject lateinit var notifications: NotificationHelper

    private var player: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var name = ""
    private var url = ""

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> stopEverything()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
            AudioManager.AUDIOFOCUS_GAIN -> if (RadioPlayback.state.value.active && !isPlaying()) resume()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopEverything(); return START_NOT_STICKY }
            ACTION_TOGGLE -> { toggle(); return START_STICKY }
        }
        name = intent?.getStringExtra(EXTRA_NAME) ?: "إذاعة"
        url = intent?.getStringExtra(EXTRA_URL) ?: ""
        if (url.isBlank()) { stopEverything(); return START_NOT_STICKY }
        startForeground(NotificationHelper.ID_RADIO, buildNotification())
        requestFocus()
        RadioPlayback.update { RadioState(active = true, name = name, url = url, isPlaying = true, loading = true) }
        play()
        return START_STICKY
    }

    private fun play() {
        releasePlayer()
        player = MediaPlayer().apply {
            setAudioAttributes(attrs)
            setWakeMode(this@RadioService, PowerManager.PARTIAL_WAKE_LOCK)
            val ok = runCatching { setDataSource(url) }.isSuccess
            if (!ok) { stopEverything(); return }
            setOnPreparedListener { start(); RadioPlayback.update { it.copy(loading = false, isPlaying = true) }; updateNotification() }
            setOnErrorListener { _, _, _ -> stopEverything(); true }
            prepareAsync()
        }
    }

    private fun toggle() { if (isPlaying()) pause() else resume() }
    private fun pause() { runCatching { player?.pause() }; RadioPlayback.update { it.copy(isPlaying = false) }; updateNotification() }
    private fun resume() {
        // البثّ الحيّ لا يُستأنَف بدقّة؛ نعيد الاتّصال من جديد.
        if (player == null) play() else { runCatching { player?.start() }; RadioPlayback.update { it.copy(isPlaying = true) }; updateNotification() }
    }
    private fun isPlaying(): Boolean = runCatching { player?.isPlaying == true }.getOrDefault(false)

    private fun buildNotification() = notifications.serviceNotification("📻 $name", stopPendingIntent())
    private fun updateNotification() = notifications.notify(NotificationHelper.ID_RADIO, buildNotification())

    private fun requestFocus() {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager = am
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attrs).setOnAudioFocusChangeListener(focusListener).build()
            focusRequest = req
            am.requestAudioFocus(req)
        }
    }

    private fun abandonFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) focusRequest?.let { am.abandonAudioFocusRequest(it) }
    }

    private fun releasePlayer() { player?.run { runCatching { if (isPlaying) stop() }; release() }; player = null }

    private fun stopEverything() {
        releasePlayer(); abandonFocus(); RadioPlayback.reset()
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }

    override fun onDestroy() { releasePlayer(); abandonFocus(); super.onDestroy() }

    private fun stopPendingIntent(): PendingIntent {
        val i = Intent(this, RadioService::class.java).setAction(ACTION_STOP)
        return PendingIntent.getService(this, 9, i, NotificationHelper.PI_FLAGS)
    }

    companion object {
        const val ACTION_START = "io.github.salehgnutux.gtsalat.RADIO_START"
        const val ACTION_STOP = "io.github.salehgnutux.gtsalat.RADIO_STOP"
        const val ACTION_TOGGLE = "io.github.salehgnutux.gtsalat.RADIO_TOGGLE"
        const val EXTRA_NAME = "name"
        const val EXTRA_URL = "url"
    }
}
