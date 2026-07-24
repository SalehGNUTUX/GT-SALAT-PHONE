package io.github.salehgnutux.gtsalat.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.salehgnutux.gtsalat.data.PrayerRepository
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
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
) {
    private val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun scheduleNext() {
        cancelAll()
        val s = settingsRepo.current()
        if (!s.setupCompleted || !s.hasLocation || s.doNotDisturb || !s.enableSalatNotify) return

        val next = repo.nextPrayer() ?: return
        val prayerAt = next.prayer.epochMillis
        val id = next.prayer.id
        val now = System.currentTimeMillis()
        if (prayerAt <= now) return

        setExact(prayerAt, adhanIntent(id.name, id.arabic))

        if (s.enablePreNotify) {
            val preAt = prayerAt - s.preNotifyMinutes * 60_000L
            if (preAt > now) setExact(preAt, preNotifyIntent(id.arabic, s.preNotifyMinutes))
        }
    }

    fun cancelAll() {
        am.cancel(adhanIntent("", ""))
        am.cancel(preNotifyIntent("", 0))
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

    companion object {
        private const val RC_ADHAN = 1001
        private const val RC_PRENOTIFY = 1002
        private val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }
}
