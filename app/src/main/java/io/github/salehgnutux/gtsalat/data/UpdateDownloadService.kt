package io.github.salehgnutux.gtsalat.data

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.salehgnutux.gtsalat.R
import io.github.salehgnutux.gtsalat.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * خدمةٌ مقدّمةٌ تنزّل تحديث الـAPK في الخلفية (يُكمِل والتطبيق مغلق) عبر [UpdateInstaller.downloadResumable]
 * القابل للاستئناف — فلا يُفقَد التقدّم عند الخروج، ويُستأنف بعد انقطاع النت من حيث توقّف.
 */
@AndroidEntryPoint
class UpdateDownloadService : Service() {

    @Inject lateinit var installer: UpdateInstaller
    @Inject lateinit var notifications: NotificationHelper

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopEverything(); return START_NOT_STICKY }
        val url = intent?.getStringExtra(EXTRA_URL)
        if (url.isNullOrBlank()) { stopSelf(); return START_NOT_STICKY }

        startForeground(NotificationHelper.ID_UPDATE, progressNotification(0))
        job?.cancel()
        job = scope.launch {
            val file = installer.downloadResumable(url) { pct ->
                runCatching { notifications.notify(NotificationHelper.ID_UPDATE, progressNotification(pct)) }
            }
            if (file != null && file.exists() && file.length() > 0L) {
                notifications.notify(NotificationHelper.ID_UPDATE, readyNotification())
                installer.install(file)   // يعمل مباشرةً إن كان التطبيق في المقدّمة؛ وإلّا يفتحه المستخدم من الإشعار
            } else {
                installer.setDownloadFailed()
                notifications.notify(NotificationHelper.ID_UPDATE, failedNotification())
            }
            stopEverything()
        }
        return START_NOT_STICKY
    }

    private fun openAppIntent(): PendingIntent {
        val i = Intent(this, io.github.salehgnutux.gtsalat.MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(this, 20, i, NotificationHelper.PI_FLAGS)
    }

    private fun stopPendingIntent(): PendingIntent {
        val i = Intent(this, UpdateDownloadService::class.java).setAction(ACTION_STOP)
        return PendingIntent.getService(this, 21, i, NotificationHelper.PI_FLAGS)
    }

    private fun progressNotification(percent: Int) =
        NotificationCompat.Builder(this, NotificationHelper.CH_SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⬇️ تنزيل التحديث…")
            .setContentText("$percent٪")
            .setProgress(100, percent, percent <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent())
            .addAction(R.drawable.ic_notification, "إيقاف", stopPendingIntent())
            .build()

    private fun readyNotification() =
        NotificationCompat.Builder(this, NotificationHelper.CH_SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("التحديث جاهزٌ للتثبيت")
            .setContentText("افتح التطبيق لإتمام التثبيت")
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()

    private fun failedNotification() =
        NotificationCompat.Builder(this, NotificationHelper.CH_SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("تعذّر تنزيل التحديث")
            .setContentText("سيُستأنف من حيث توقّف عند إعادة المحاولة")
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()

    private fun stopEverything() {
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    companion object {
        private const val ACTION_STOP = "io.github.salehgnutux.gtsalat.UPDATE_DL_STOP"
        private const val EXTRA_URL = "url"

        fun start(context: Context, url: String) {
            val i = Intent(context, UpdateDownloadService::class.java).putExtra(EXTRA_URL, url)
            runCatching { ContextCompat.startForegroundService(context, i) }
        }
    }
}
