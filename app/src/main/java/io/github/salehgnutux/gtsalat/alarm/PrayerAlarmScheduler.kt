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
        scheduleWidgetRefresh()   // إنذارٌ يقدّم الودجت عند كلّ صلاة/منتصف ليل (ولو كان الأذان مطفأً)
        // امسح تنبيهات الأذان/التنبيه المسبق القديمة العالقة (تُعاد لحظةَ إطلاق أذان الصلاة الحاليّ)،
        // وخدمة الأذان إن لم يكن ثمّة صوتٌ يُشغَّل الآن — تفاديّاً لإشعارٍ «زومبي» يبقى بعد تجميد النظام.
        notifications.cancelPrayerAlerts()
        if (!io.github.salehgnutux.gtsalat.audio.AdhanPlayback.active.value) notifications.cancelAdhanService()
        scheduleDailyReminder()   // والتذكيرات اليوميّة (وِرد/أيّام بيض/آية)
        scheduleAdhkarReminders(s) // وأذكار الصباح/المساء (إن فُعّلت)
        if (!s.setupCompleted || !s.hasLocation || s.doNotDisturb || !s.enableSalatNotify) return

        val now = System.currentTimeMillis()
        // نُسلّح **عدّة صلواتٍ مقدّماً** (لا القادمة فقط): شبكةُ أمانٍ لو جُمّد التطبيق فلم
        // يُعِد الجدولة الذاتيّة؛ فتبقى إنذارات النظام مسلَّحةً لعدّة أيّام.
        val upcoming = repo.upcomingPrayers(AHEAD, now)
        upcoming.forEachIndexed { i, p ->
            // الأذان عبر setAlarmClock: لا يؤجّله Doze/توفير البطاريّة إطلاقاً (كإنذار المنبّه).
            setAlarmClock(p.epochMillis, adhanIntent(p.id.name, p.id.arabic, i))
            if (s.enablePreNotify) {
                val preAt = p.epochMillis - s.preNotifyMinutes * 60_000L
                if (preAt > now) setAlarmClock(preAt, preNotifyIntent(p.id.arabic, s.preNotifyMinutes, i))
            }
        }
    }

    fun cancelAll() {
        for (i in 0 until AHEAD) {
            am.cancel(adhanIntent("", "", i))
            am.cancel(preNotifyIntent("", 0, i))
        }
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

    /** جدولة تذكيرَي أذكار الصباح/المساء عند ساعتيهما (يعيد كلٌّ جدولة نفسه). */
    fun scheduleAdhkarReminders(s: io.github.salehgnutux.gtsalat.data.settings.AppSettings) {
        am.cancel(adhkarIntent(morning = true))
        am.cancel(adhkarIntent(morning = false))
        if (s.enableMorningAdhkar) setExact(nextDailyMillis(s.morningAdhkarHour), adhkarIntent(morning = true))
        if (s.enableEveningAdhkar) setExact(nextDailyMillis(s.eveningAdhkarHour), adhkarIntent(morning = false))
    }

    /** إعادة جدولة تذكير أذكارٍ واحدٍ لليوم التالي (يُستدعى من المُستقبِل). */
    suspend fun rescheduleAdhkar(morning: Boolean) {
        val s = settingsRepo.current()
        val on = if (morning) s.enableMorningAdhkar else s.enableEveningAdhkar
        if (!on) return
        val hour = if (morning) s.morningAdhkarHour else s.eveningAdhkarHour
        setExact(nextDailyMillis(hour), adhkarIntent(morning))
    }

    private fun nextDailyMillis(hour: Int): Long {
        val now = java.time.LocalDateTime.now()
        var at = now.withHour(hour.coerceIn(0, 23)).withMinute(0).withSecond(0).withNano(0)
        if (!at.isAfter(now)) at = at.plusDays(1)
        return at.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun adhkarIntent(morning: Boolean): PendingIntent {
        val i = Intent(context, DailyReminderReceiver::class.java).apply {
            action = if (morning) DailyReminderReceiver.ACTION_MORNING_ADHKAR else DailyReminderReceiver.ACTION_EVENING_ADHKAR
        }
        return PendingIntent.getBroadcast(context, if (morning) RC_MORNING_ADHKAR else RC_EVENING_ADHKAR, i, FLAGS)
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

    /**
     * إنذارٌ دقيقٌ **مستقلٌّ عن إعداد الأذان** يعيد رسم الودجتات عند حلول الصلاة القادمة (أو منتصف
     * الليل لتبديل التاريخ)، فيتقدّم الودجت تلقائيّاً — تُظهر الصلاةَ الجديدة وعدّاداً صحيحاً —
     * حتى والتطبيق في الخلفيّة. يُعيد جدولة نفسه من المُستقبِل عبر [ACTION_REFRESH_WIDGETS].
     * (لا يتجاوز قيود «الإيقاف القسريّ» على بعض الأجهزة؛ علاجها إعفاء البطاريّة والتشغيل التلقائيّ.)
     */
    suspend fun scheduleWidgetRefresh() {
        val now = System.currentTimeMillis()
        val nextPrayer = runCatching { repo.nextPrayer(now)?.prayer?.epochMillis }.getOrNull()
        val midnight = java.time.LocalDate.now().plusDays(1)
            .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val at = listOfNotNull(nextPrayer?.plus(2_000L), midnight).filter { it > now }.minOrNull()
            ?: (now + 30 * 60_000L)
        setExact(at, widgetRefreshIntent())
    }

    private fun widgetRefreshIntent(): PendingIntent {
        val i = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_REFRESH_WIDGETS
        }
        return PendingIntent.getBroadcast(context, RC_WIDGET, i, FLAGS)
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

    private fun adhanIntent(prayerName: String, prayerAr: String, index: Int = 0): PendingIntent {
        val i = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_ADHAN
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER, prayerName)
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_AR, prayerAr)
        }
        return PendingIntent.getBroadcast(context, RC_ADHAN_BASE + index, i, FLAGS)
    }

    private fun preNotifyIntent(prayerAr: String, minutes: Int, index: Int = 0): PendingIntent {
        val i = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_PRENOTIFY
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_AR, prayerAr)
            putExtra(PrayerAlarmReceiver.EXTRA_MINUTES, minutes)
        }
        return PendingIntent.getBroadcast(context, RC_PRENOTIFY_BASE + index, i, FLAGS)
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
        private const val AHEAD = 12           // عدد الصلوات المُسلَّحة مقدّماً (شبكة أمان ~يومان ونصف)
        private const val RC_RESTORE = 1003
        private const val RC_POSTDHIKR = 1004
        private const val RC_SHOW = 1005
        private const val RC_REMINDER = 1006
        private const val RC_TEST = 1007
        private const val RC_MORNING_ADHKAR = 1008
        private const val RC_EVENING_ADHKAR = 1009
        private const val RC_WIDGET = 1010
        private const val RC_ADHAN_BASE = 1100     // 1100..1107
        private const val RC_PRENOTIFY_BASE = 1200 // 1200..1207
        private val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }
}
