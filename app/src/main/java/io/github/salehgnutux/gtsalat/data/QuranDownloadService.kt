package io.github.salehgnutux.gtsalat.data

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.salehgnutux.gtsalat.R
import io.github.salehgnutux.gtsalat.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * يُبقي تنزيلات القرآن حيّةً بعد مغادرة القسم أو الخروج من التطبيق (خدمة مقدّمة dataSync)، بإشعار تقدّمٍ وزرّ إيقاف.
 * التنزيل نفسه في [QuranDownloader] على نطاق التطبيق؛ هذه الخدمة تحمي العمليّة وتتوقّف وحدها حين لا يبقى تنزيل.
 */
@AndroidEntryPoint
class QuranDownloadService : Service() {

    @Inject lateinit var downloader: QuranDownloader
    @Inject lateinit var notifications: NotificationHelper

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watching = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            downloader.cancelAll()
            stopNow()
            return START_NOT_STICKY
        }
        val started = runCatching {
            ServiceCompat.startForeground(
                this, NotificationHelper.ID_QURAN_DOWNLOAD, build(downloader.activity.value),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0,
            )
        }
        if (started.isFailure) { stopSelf(); return START_NOT_STICKY }
        if (!watching) {
            watching = true
            scope.launch {
                downloader.activity.collect { a ->
                    if (a.isIdle) stopNow()
                    else notifications.notify(NotificationHelper.ID_QURAN_DOWNLOAD, build(a))
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun build(a: DownloadActivity) =
        NotificationCompat.Builder(this, NotificationHelper.CH_SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⬇️ تنزيل القرآن للاستعمال دون إنترنت")
            .setContentText(a.summary)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .apply { a.percent?.let { setProgress(100, it, false) } ?: setProgress(0, 0, true) }
            .addAction(R.drawable.ic_notification, "إيقاف التنزيل", cancelIntent())
            .build()

    private fun cancelIntent(): PendingIntent = PendingIntent.getService(
        this, 31, Intent(this, QuranDownloadService::class.java).setAction(ACTION_CANCEL), NotificationHelper.PI_FLAGS,
    )

    private fun stopNow() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** أندرويد 15: لخدمات dataSync حدٌّ يوميّ؛ عند بلوغه نتوقّف بأمان ويُستأنف التنزيل لاحقاً من حيث وصل. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        downloader.cancelAll()
        stopNow()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_CANCEL = "io.github.salehgnutux.gtsalat.QURAN_DOWNLOAD_CANCEL"
    }
}
