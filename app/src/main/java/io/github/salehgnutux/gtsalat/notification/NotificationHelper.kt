package io.github.salehgnutux.gtsalat.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.salehgnutux.gtsalat.MainActivity
import io.github.salehgnutux.gtsalat.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun ensureChannels() {
        nm.createNotificationChannel(
            NotificationChannel(CH_ADHAN, "الأذان ودخول الوقت", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "إشعارٌ بارز عند دخول وقت الصلاة"
                setSound(null, null) // الصوت يُشغَّل عبر خدمة الأذان لا القناة
                enableVibration(true)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_PRENOTIFY, "تنبيه الاقتراب", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "تنبيهٌ قبل دخول وقت الصلاة"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_SERVICE, "تشغيل الأذان", NotificationManager.IMPORTANCE_LOW).apply {
                description = "إشعار خدمة تشغيل الأذان مع زرّ الإيقاف"
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_STATUS, "الصلاة القادمة (دائم)", NotificationManager.IMPORTANCE_LOW).apply {
                description = "إشعارٌ دائمٌ في القائمة المنسدلة بالصلاة القادمة وعدٍّ تنازليّ"
                setShowBadge(false)
                setSound(null, null)
            }
        )
    }

    private fun contentIntent(): PendingIntent {
        val i = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, 0, i, PI_FLAGS)
    }

    fun prayerNotification(prayerName: String): Notification =
        NotificationCompat.Builder(context, CH_ADHAN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🕌 حان الآن وقت صلاة $prayerName")
            .setContentText("اللّهُ أكبر")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent())
            .build()

    fun preNotifyNotification(prayerName: String, minutes: Int): Notification =
        NotificationCompat.Builder(context, CH_PRENOTIFY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⏰ تبقّى $minutes دقيقة على صلاة $prayerName")
            .setContentText("استعدّ للصلاة")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent())
            .build()

    fun serviceNotification(prayerName: String, stopIntent: PendingIntent): Notification =
        NotificationCompat.Builder(context, CH_SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🔊 يُشغَّل الآن أذان $prayerName")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(R.drawable.ic_notification, "إيقاف", stopIntent)
            .setContentIntent(contentIntent())
            .build()

    /** إشعارٌ دائمٌ بالصلاة القادمة مع عدٍّ تنازليّ حيّ (chronometer) في القائمة المنسدلة. */
    fun statusNotification(prayerName: String, timeText: String, nextEpochMillis: Long): Notification =
        NotificationCompat.Builder(context, CH_STATUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🕌 $prayerName • $timeText")
            .setContentText("الوقت المتبقّي للصلاة")
            .setWhen(nextEpochMillis)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent())
            .build()

    fun showStatus(prayerName: String, timeText: String, nextEpochMillis: Long) =
        nm.notify(ID_STATUS, statusNotification(prayerName, timeText, nextEpochMillis))

    fun cancelStatus() = nm.cancel(ID_STATUS)

    fun notify(id: Int, n: Notification) = nm.notify(id, n)

    companion object {
        const val CH_ADHAN = "adhan"
        const val CH_PRENOTIFY = "prenotify"
        const val CH_SERVICE = "adhan_service"
        const val CH_STATUS = "status"
        const val ID_PRAYER = 2001
        const val ID_PRENOTIFY = 2002
        const val ID_SERVICE = 2003
        const val ID_STATUS = 2004
        val PI_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }
}
