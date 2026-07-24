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
import io.github.salehgnutux.gtsalat.R
import io.github.salehgnutux.gtsalat.data.settings.AdhanType
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import io.github.salehgnutux.gtsalat.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * تشغيل الأذان كاملاً عبر خدمة مقدّمة (mediaPlayback) بـ MediaPlayer.
 * يستعمل USAGE_ALARM ليُسمَع في الوضع الصامت، ويطلب audio focus، ويمسك wake lock،
 * ويوفّر زرّ إيقاف في الإشعار. يشغّل دعاء ما بعد الأذان تلقائيّاً إن كان مفعّلاً.
 */
@AndroidEntryPoint
class AdhanService : Service() {

    @Inject lateinit var notifications: NotificationHelper
    @Inject lateinit var settingsRepo: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopEverything()
            return START_NOT_STICKY
        }
        val prayerAr = intent?.getStringExtra(EXTRA_PRAYER_AR) ?: "الصلاة"
        startForeground(NotificationHelper.ID_SERVICE, notifications.serviceNotification(prayerAr, stopPendingIntent()))

        scope.launch {
            val s = settingsRepo.current()
            requestFocus()
            playUri(adhanUri(s)) {
                if (s.enableDuaAfterAdhan) {
                    playUri(resUri(R.raw.dua_after_adhan)) { stopEverything() }
                } else {
                    stopEverything()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun resUri(resId: Int): Uri = Uri.parse("android.resource://$packageName/$resId")

    /** مصدر الأذان حسب النوع؛ يسقط إلى الكامل إن تعذّر الأذان المخصّص. */
    private fun adhanUri(s: io.github.salehgnutux.gtsalat.data.settings.AppSettings): Uri = when (s.adhanType) {
        AdhanType.SHORT -> resUri(R.raw.adhan_short)
        AdhanType.CUSTOM -> s.customAdhanUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?: resUri(R.raw.adhan_full)
        AdhanType.FULL -> resUri(R.raw.adhan_full)
    }

    private fun playUri(uri: Uri, onComplete: () -> Unit) {
        releasePlayer()
        player = MediaPlayer().apply {
            setAudioAttributes(audioAttributes)
            setWakeMode(this@AdhanService, PowerManager.PARTIAL_WAKE_LOCK)
            // إن فشل المسار المخصّص (حُذف الملفّ/سُحب الإذن) نسقط إلى الأذان الكامل.
            val ok = runCatching { setDataSource(this@AdhanService, uri) }.isSuccess
            if (!ok) runCatching { setDataSource(this@AdhanService, resUri(R.raw.adhan_full)) }
            setOnCompletionListener { onComplete() }
            setOnErrorListener { _, _, _ -> stopEverything(); true }
            setOnPreparedListener { start() }
            prepareAsync()
        }
    }

    private fun requestFocus() {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager = am
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(audioAttributes)
                .build()
            focusRequest = req
            am.requestAudioFocus(req)
        }
    }

    private fun abandonFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { am.abandonAudioFocusRequest(it) }
        }
    }

    private fun releasePlayer() {
        player?.run { runCatching { if (isPlaying) stop() }; release() }
        player = null
    }

    private fun stopEverything() {
        releasePlayer()
        abandonFocus()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        releasePlayer()
        abandonFocus()
        super.onDestroy()
    }

    private fun stopPendingIntent(): PendingIntent {
        val i = Intent(this, AdhanService::class.java).setAction(ACTION_STOP)
        return PendingIntent.getService(this, 0, i, NotificationHelper.PI_FLAGS)
    }

    companion object {
        const val EXTRA_PRAYER_AR = "prayer_ar"
        const val ACTION_STOP = "io.github.salehgnutux.gtsalat.ADHAN_STOP"
    }
}
