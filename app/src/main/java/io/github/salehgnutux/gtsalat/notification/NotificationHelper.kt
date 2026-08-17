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
        nm.createNotificationChannel(
            NotificationChannel(CH_REMINDERS, "التذكيرات اليوميّة", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "آية اليوم ووِرد التلاوة والأيّام البيض"
            }
        )
    }

    private fun reminder(title: String, text: String, route: String? = null, requestCode: Int = 0): Notification =
        NotificationCompat.Builder(context, CH_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(route, requestCode))
            .build()

    /** تنبيهٌ اختباريٌّ (لقياس وصول التنبيهات والشاشة مغلقة) — على قناة الأذان ليُسمَع كالأذان. */
    fun testNotification(): Notification =
        NotificationCompat.Builder(context, CH_PRENOTIFY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("✅ تنبيهٌ اختباريّ")
            .setContentText("وصلك هذا التنبيه بنجاح — التنبيهات تعمل والشاشة مغلقة.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent())
            .build()

    /** إشعارُ توفّر نسخةٍ جديدة — نقره يفتح صفحة الإصدار. */
    fun showUpdate(version: String, url: String) {
        val open = PendingIntent.getActivity(
            context, 8,
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)),
            PI_FLAGS,
        )
        nm.notify(
            ID_UPDATE,
            NotificationCompat.Builder(context, CH_REMINDERS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("تحديثٌ متوفّر: GT-SALAT v$version")
                .setContentText("اضغط لفتح صفحة التنزيل.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(open)
                .build(),
        )
    }

    fun showRecitationReminder() = nm.notify(ID_RECITATION, reminder("📖 وِرد التلاوة", "لا تنسَ وردك اليوميّ من تلاوة القرآن الكريم."))
    fun showMorningAdhkar() = nm.notify(ID_MORNING_ADHKAR, reminder("🌅 أذكار الصباح", "حان وقت أذكار الصباح — «أصبحنا وأصبح الملك لله».", "adhkar_session/morning", 91))
    fun showEveningAdhkar() = nm.notify(ID_EVENING_ADHKAR, reminder("🌇 أذكار المساء", "حان وقت أذكار المساء — «أمسينا وأمسى الملك لله».", "adhkar_session/evening", 92))
    fun showWhiteDaysReminder(text: String) = nm.notify(ID_WHITEDAYS, reminder("🌕 الأيّام البيض", text))
    fun showSunnahReminder(title: String, text: String) = nm.notify(ID_SUNNAH, reminder("🌙 سُنّة: $title", text))
    fun showDailyAyah(surah: String, n: Int, text: String) = nm.notify(ID_AYAH, reminder("آية اليوم — سورة $surah [$n]", text))

    private fun contentIntent(route: String? = null, requestCode: Int = 0): PendingIntent {
        val i = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (route != null) i.putExtra(MainActivity.EXTRA_ROUTE, route)
        return PendingIntent.getActivity(context, requestCode, i, PI_FLAGS)
    }

    fun prayerNotification(prayerName: String, fullScreen: PendingIntent? = null): Notification =
        NotificationCompat.Builder(context, CH_ADHAN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🕌 حان الآن وقت صلاة $prayerName")
            .setContentText("اللّهُ أكبر")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setTimeoutAfter(25 * 60_000L)   // يُمسَح تلقائيّاً بعد ربع ساعةٍ فلا يعلق (حتى لو جُمّد التطبيق)
            .setContentIntent(contentIntent())
            .apply { if (fullScreen != null) setFullScreenIntent(fullScreen, true) }
            .build()

    /** إشعارُ أذكار ما بعد الصلاة (بنافذةٍ ملء الشاشة اختياريّاً كالأذان). */
    fun dhikrNotification(prayerName: String, fullScreen: PendingIntent? = null): Notification =
        NotificationCompat.Builder(context, CH_ADHAN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("📿 أذكارٌ بعد صلاة $prayerName")
            .setContentText("سبحان الله والحمد لله ولا إله إلّا الله والله أكبر")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent())
            .apply { if (fullScreen != null) setFullScreenIntent(fullScreen, true) }
            .build()

    /** يبني full-screen PendingIntent يُطلق [io.github.salehgnutux.gtsalat.alarm.AdhanAlarmActivity]. */
    fun fullScreenAdhanIntent(title: String, subtitle: String, isDhikr: Boolean): PendingIntent {
        val i = io.github.salehgnutux.gtsalat.alarm.AdhanAlarmActivity.intent(context, title, subtitle, isDhikr)
        return PendingIntent.getActivity(context, if (isDhikr) 71 else 70, i, PI_FLAGS)
    }

    fun preNotifyNotification(prayerName: String, minutes: Int): Notification =
        NotificationCompat.Builder(context, CH_PRENOTIFY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⏰ تبقّى $minutes دقيقة على صلاة $prayerName")
            .setContentText("استعدّ للصلاة")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            // يُمسَح تلقائيّاً عند دخول وقت الصلاة (بعد المدّة + دقيقة) فلا يبقى معلّقاً لساعات.
            .setTimeoutAfter((minutes + 1) * 60_000L)
            .setContentIntent(contentIntent())
            .build()

    fun serviceNotification(title: String, stopIntent: PendingIntent): Notification =
        NotificationCompat.Builder(context, CH_SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
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

    /** يمسح تنبيهات الأذان/التنبيه المسبق العالقة (تُعاد عند إطلاق أذان الصلاة الحاليّ). */
    fun cancelPrayerAlerts() { nm.cancel(ID_PRAYER); nm.cancel(ID_PRENOTIFY) }

    /** يمسح إشعار خدمة الأذان العالق (يُستدعى فقط حين لا يكون ثمّة صوتٌ يُشغَّل الآن). */
    fun cancelAdhanService() = nm.cancel(ID_SERVICE)

    fun notify(id: Int, n: Notification) = nm.notify(id, n)

    companion object {
        const val CH_ADHAN = "adhan"
        const val CH_PRENOTIFY = "prenotify"
        const val CH_SERVICE = "adhan_service"
        const val CH_STATUS = "status"
        const val CH_REMINDERS = "reminders"
        const val ID_PRAYER = 2001
        const val ID_PRENOTIFY = 2002
        const val ID_SERVICE = 2003
        const val ID_STATUS = 2004
        const val ID_RECITATION = 2005
        const val ID_WHITEDAYS = 2006
        const val ID_AYAH = 2007
        const val ID_TEST = 2008
        const val ID_RADIO = 2009
        const val ID_UPDATE = 2010
        const val ID_MORNING_ADHKAR = 2011
        const val ID_EVENING_ADHKAR = 2012
        const val ID_SUNNAH = 2013
        const val ID_RUQYAH = 2014
        val PI_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }
}
