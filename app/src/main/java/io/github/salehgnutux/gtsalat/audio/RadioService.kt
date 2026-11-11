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
    private var wifiLock: android.net.wifi.WifiManager.WifiLock? = null
    private var name = ""
    private var url = ""
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val sleepRunnable = Runnable { stopEverything() }

    // إعادة الاتّصال: انقطاعٌ لحظيٌّ للشبكة شائعٌ والشاشة مطفأة (نوم الواي فاي، أو التحوّل إلى البيانات)،
    // وكان أيّ خطأٍ يوقف البثّ نهائيّاً. نعيد المحاولة بتباعدٍ متزايد، وفور عودة الشبكة.
    private var retries = 0
    private var reconnectPending = false
    private val reconnectRunnable = Runnable { reconnectPending = false; play() }
    private var reconnectWake: PowerManager.WakeLock? = null
    private var netCallback: android.net.ConnectivityManager.NetworkCallback? = null

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
            ACTION_SLEEP -> { setSleepTimer(intent.getIntExtra(EXTRA_MINUTES, 0)); return START_STICKY }
        }
        name = intent?.getStringExtra(EXTRA_NAME) ?: "إذاعة"
        url = intent?.getStringExtra(EXTRA_URL) ?: ""
        if (url.isBlank()) { stopEverything(); return START_NOT_STICKY }
        startForeground(NotificationHelper.ID_RADIO, buildNotification())
        requestFocus()
        acquireWifiLock()   // يبقي الواي‑فاي حيّاً للبثّ والشاشة مغلقة
        // المؤقّت يبقى عند الانتقال بين الإذاعات.
        clearReconnect()
        RadioPlayback.update { RadioState(active = true, name = name, url = url, isPlaying = true, loading = true, sleepUntil = it.sleepUntil, sleepTotal = it.sleepTotal) }
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
            setOnPreparedListener { retries = 0; releaseReconnectWake(); start(); RadioPlayback.update { it.copy(loading = false, isPlaying = true) }; updateNotification() }
            setOnErrorListener { _, what, extra -> scheduleReconnect("خطأ $what/$extra"); true }
            // البثّ الحيّ لا ينتهي؛ انتهاؤه يعني انقطاع الاتّصال.
            setOnCompletionListener { scheduleReconnect("انقطع البثّ") }
            setOnInfoListener { _, what, _ ->
                when (what) {
                    MediaPlayer.MEDIA_INFO_BUFFERING_START -> RadioPlayback.update { it.copy(loading = true) }
                    MediaPlayer.MEDIA_INFO_BUFFERING_END -> RadioPlayback.update { it.copy(loading = false) }
                }
                false
            }
            prepareAsync()
        }
    }

    private fun setSleepTimer(minutes: Int) {
        handler.removeCallbacks(sleepRunnable)
        if (minutes <= 0) {
            RadioPlayback.update { it.copy(sleepUntil = 0L, sleepTotal = 0L) }
            return
        }
        val delay = minutes * 60_000L
        handler.postDelayed(sleepRunnable, delay)
        RadioPlayback.update { it.copy(sleepUntil = android.os.SystemClock.elapsedRealtime() + delay, sleepTotal = delay) }
    }

    private fun scheduleReconnect(reason: String) {
        if (!RadioPlayback.state.value.active || reconnectPending) return
        releasePlayer()
        if (retries >= MAX_RETRIES) {
            io.github.salehgnutux.gtsalat.alarm.AlarmLog.add(this, "الإذاعة توقّفت بعد $MAX_RETRIES محاولات", detail = reason)
            stopEverything()
            return
        }
        retries++
        io.github.salehgnutux.gtsalat.alarm.AlarmLog.add(this, "الإذاعة: إعادة اتّصال ($retries)", detail = reason)
        RadioPlayback.update { it.copy(loading = true) }
        // قفل استيقاظٍ قصير، وإلّا نام المعالج والشاشة مطفأة فلم تأتِ المحاولة.
        runCatching {
            if (reconnectWake == null) {
                reconnectWake = (getSystemService(POWER_SERVICE) as PowerManager)
                    .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "gtsalat:radio-reconnect").apply { setReferenceCounted(false) }
            }
            reconnectWake?.acquire(90_000L)
        }
        reconnectPending = true
        handler.postDelayed(reconnectRunnable, minOf(30_000L, 1_000L shl retries))
        registerNetworkCallback()
    }

    /** عودة الشبكة تختصر الانتظار: نعيد الاتّصال فوراً. */
    private fun registerNetworkCallback() {
        if (netCallback != null) return
        val cm = getSystemService(CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return
        val cb = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                handler.post {
                    if (reconnectPending) {
                        handler.removeCallbacks(reconnectRunnable)
                        reconnectPending = false
                        play()
                    }
                }
            }
        }
        if (runCatching { cm.registerDefaultNetworkCallback(cb) }.isSuccess) netCallback = cb
    }

    private fun releaseReconnectWake() { runCatching { reconnectWake?.let { if (it.isHeld) it.release() } } }

    private fun clearReconnect() {
        handler.removeCallbacks(reconnectRunnable)
        reconnectPending = false
        retries = 0
        releaseReconnectWake()
        netCallback?.let { cb ->
            runCatching { (getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager).unregisterNetworkCallback(cb) }
        }
        netCallback = null
    }

    private fun toggle() { if (isPlaying()) pause() else resume() }
    private fun pause() { runCatching { player?.pause() }; RadioPlayback.update { it.copy(isPlaying = false) }; updateNotification() }
    private fun resume() {
        // أثناء الإيقاف المؤقّت قد ينام الجهاز فيتأخّر المؤقّت، فلا نستأنف بعد انقضائه.
        val until = RadioPlayback.state.value.sleepUntil
        if (until > 0L && android.os.SystemClock.elapsedRealtime() >= until) { stopEverything(); return }
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

    private fun acquireWifiLock() {
        runCatching {
            val wifi = applicationContext.getSystemService(WIFI_SERVICE) as android.net.wifi.WifiManager
            wifiLock = wifi.createWifiLock(android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF, "gtsalat:radio")
                .apply { setReferenceCounted(false); acquire() }
        }
    }

    private fun releaseWifiLock() { runCatching { wifiLock?.let { if (it.isHeld) it.release() } }; wifiLock = null }

    private fun releasePlayer() { player?.run { runCatching { if (isPlaying) stop() }; release() }; player = null }

    private fun stopEverything() {
        handler.removeCallbacks(sleepRunnable)
        clearReconnect()
        releasePlayer(); abandonFocus(); releaseWifiLock(); RadioPlayback.reset()
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }

    override fun onDestroy() { handler.removeCallbacks(sleepRunnable); clearReconnect(); releasePlayer(); abandonFocus(); releaseWifiLock(); super.onDestroy() }

    private fun stopPendingIntent(): PendingIntent {
        val i = Intent(this, RadioService::class.java).setAction(ACTION_STOP)
        return PendingIntent.getService(this, 9, i, NotificationHelper.PI_FLAGS)
    }

    companion object {
        const val ACTION_START = "io.github.salehgnutux.gtsalat.RADIO_START"
        const val ACTION_STOP = "io.github.salehgnutux.gtsalat.RADIO_STOP"
        const val ACTION_TOGGLE = "io.github.salehgnutux.gtsalat.RADIO_TOGGLE"
        private const val MAX_RETRIES = 8
        const val ACTION_SLEEP = "io.github.salehgnutux.gtsalat.RADIO_SLEEP"
        const val EXTRA_MINUTES = "minutes"
        const val EXTRA_NAME = "name"
        const val EXTRA_URL = "url"
    }
}
