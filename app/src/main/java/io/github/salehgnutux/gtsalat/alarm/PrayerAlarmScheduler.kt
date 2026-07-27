package io.github.salehgnutux.gtsalat.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.salehgnutux.gtsalat.data.PrayerRepository
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import io.github.salehgnutux.gtsalat.notification.NotificationHelper
import io.github.salehgnutux.gtsalat.util.Format
import javax.inject.Inject
import javax.inject.Singleton

/**
 * جدولة إنذارٍ دقيق للصلاة القادمة فقط (نمط self-rescheduling): عند إطلاق أذان الصلاة
 * يُعاد استدعاء scheduleNext لجدولة التالية. يستعمل setExactAndAllowWhileIdle لاختراق Doze،
 * مع سقوطٍ آمن إلى setAndAllowWhileIdle إن رُفض إذن الإنذار الدقيق (Android 12+).
 */
@Singleton
class PrayerAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: PrayerRepository,
    private val settingsRepo: SettingsRepository,
    private val notifications: NotificationHelper,
) {
    private val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun scheduleNext() {
        cancelAll()
        val s = settingsRepo.current()
        refreshStatus(s)          // الإشعار الدائم مستقلٌّ عن حارسات الأذان أدناه
        refreshWidgets()          // وكذلك ودجتات سطح الهاتف
        scheduleDailyReminder()   // والتذكيرات اليوميّة (وِرد/أيّام بيض/آية)
        if (!s.setupCompleted || !s.hasLocation || s.doNotDisturb || !s.enableSalatNotify) return

        val next = repo.nextPrayer() ?: return
        val prayerAt = next.prayer.epochMillis
        val id = next.prayer.id
        val now = System.currentTimeMillis()
        if (prayerAt <= now) return

        // الأذان عبر setAlarmClock: لا يؤجّله Doze/توفير البطاريّة إطلاقاً (كإنذار المنبّه)،
        // فيصدر في وقته والشاشة مغلقة، ولا يحتاج إذن SCHEDULE_EXACT_ALARM.
        setAlarmClock(prayerAt, adhanIntent(id.name, id.arabic))

        if (s.enablePreNotify) {
            val preAt = prayerAt - s.preNotifyMinutes * 60_000L
            if (preAt > now) setAlarmClock(preAt, preNotifyIntent(id.arabic, s.preNotifyMinutes))
        }
    }

    fun cancelAll() {
        am.cancel(adhanIntent("", ""))
        am.cancel(preNotifyIntent("", 0))
        // لا نُلغي إنذارَي استعادة الرنين وذكر ما بعد الصلاة كي لا تنقطع نافذةٌ جاريةٌ عند إعادة الجدولة.
    }

    /** جدولة استعادة وضع الرنين بعد انتهاء نافذة الكتم (يُستدعى من مُستقبِل الأذان). */
    fun scheduleRestoreSound(triggerAt: Long) {
        setExact(triggerAt, restoreIntent())
    }

    /**
     * تنبيهٌ اختباريٌّ بعد دقيقة (عبر setAlarmClock كالأذان تماماً) — ليتحقّق المستخدم
     * أنّ التنبيه يصل والشاشة مغلقة/التطبيق في الخلفيّة (تشخيص إدارة البطاريّة).
     */
    fun scheduleTest() {
        setAlarmClock(System.currentTimeMillis() + 60_000L, testIntent())
    }

    private fun testIntent(): PendingIntent {
        val i = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_TEST
        }
        return PendingIntent.getBroadcast(context, RC_TEST, i, FLAGS)
    }

    /** جدولة ذكر ما بعد الصلاة (بعد دخول الوقت بدقائق). */
    fun schedulePostDhikr(triggerAt: Long) {
        setAlarmClock(triggerAt, postDhikrIntent())
    }

    /** جدولة التذكيرات اليوميّة (وِرد/أيّام بيض/آية) عند ساعةٍ محدّدة، تعيد جدولة نفسها. */
    suspend fun scheduleDailyReminder() {
        val hour = settingsRepo.current().reminderHour.coerceIn(0, 23)
        val now = java.time.LocalDateTime.now()
        var at = now.withHour(hour).withMinute(0).withSecond(0).withNano(0)
        if (!at.isAfter(now)) at = at.plusDays(1)
        val millis = at.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        setExact(millis, reminderIntent())
    }

    private fun reminderIntent(): PendingIntent {
        val i = Intent(context, DailyReminderReceiver::class.java).apply {
            action = DailyReminderReceiver.ACTION_DAILY_REMINDER
        }
        return PendingIntent.getBroadcast(context, RC_REMINDER, i, FLAGS)
    }

    /** تحديث الإشعار الدائم بالصلاة القادمة (عدٌّ تنازليّ حيّ)، أو إلغاؤه. */
    private suspend fun refreshStatus(s: io.github.salehgnutux.gtsalat.data.settings.AppSettings) {
        if (!s.persistentNotification || !s.hasLocation) {
            notifications.cancelStatus()
            return
        }
        val next = repo.nextPrayer()
        if (next == null) {
            notifications.cancelStatus()
            return
        }
        notifications.showStatus(
            next.prayer.id.arabic,
            Format.clock(next.prayer.epochMillis),
            next.prayer.epochMillis,
        )
    }

    /** تحديث ودجتات سطح الهاتف (يُستدعى مع كلّ إعادة جدولة، فتبقى محدَّثة والشاشة مغلقة). */
    suspend fun refreshWidgets() {
        runCatching { io.github.salehgnutux.gtsalat.widget.updateAllPrayerWidgets(context) }
    }

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()

    private fun setExact(triggerAt: Long, pi: PendingIntent) {
        if (canScheduleExact()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            // إذن الإنذار الدقيق مرفوض — تقريبٌ آمن بدل الصمت
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    /** أدقّ وأوثق من setExact: يُعامَل كإنذار منبّه فلا يُؤجَّل في Doze، بلا حاجة لإذنٍ خاصّ. */
    private fun setAlarmClock(triggerAt: Long, pi: PendingIntent) {
        val show = PendingIntent.getActivity(
            context, RC_SHOW,
            Intent(context, io.github.salehgnutux.gtsalat.MainActivity::class.java),
            FLAGS,
        )
        runCatching { am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, show), pi) }
            .onFailure { setExact(triggerAt, pi) }
    }

    private fun adhanIntent(prayerName: String, prayerAr: String): PendingIntent {
        val i = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_ADHAN
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER, prayerName)
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_AR, prayerAr)
        }
        return PendingIntent.getBroadcast(context, RC_ADHAN, i, FLAGS)
    }

    private fun preNotifyIntent(prayerAr: String, minutes: Int): PendingIntent {
        val i = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_PRENOTIFY
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_AR, prayerAr)
            putExtra(PrayerAlarmReceiver.EXTRA_MINUTES, minutes)
        }
        return PendingIntent.getBroadcast(context, RC_PRENOTIFY, i, FLAGS)
    }

    private fun restoreIntent(): PendingIntent {
        val i = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_RESTORE_SOUND
        }
        return PendingIntent.getBroadcast(context, RC_RESTORE, i, FLAGS)
    }

    private fun postDhikrIntent(): PendingIntent {
        val i = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_POST_DHIKR
        }
        return PendingIntent.getBroadcast(context, RC_POSTDHIKR, i, FLAGS)
    }

    companion object {
        private const val RC_ADHAN = 1001
        private const val RC_PRENOTIFY = 1002
        private const val RC_RESTORE = 1003
        private const val RC_POSTDHIKR = 1004
        private const val RC_SHOW = 1005
        private const val RC_REMINDER = 1006
        private const val RC_TEST = 1007
        private val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }
}
