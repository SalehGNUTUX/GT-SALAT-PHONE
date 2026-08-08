package io.github.salehgnutux.gtsalat.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.icu.util.IslamicCalendar
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint
import io.github.salehgnutux.gtsalat.data.ContentRepository
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import io.github.salehgnutux.gtsalat.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * تذكيراتٌ يوميّة (وِرد التلاوة + الأيّام البيض + آية اليوم) تنطلق مرّةً في اليوم،
 * ثمّ تعيد جدولة نفسها لليوم التالي (نمط ذاتيّ التسلسل).
 */
@AndroidEntryPoint
class DailyReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsRepo: SettingsRepository
    @Inject lateinit var notifications: NotificationHelper
    @Inject lateinit var content: ContentRepository
    @Inject lateinit var scheduler: PrayerAlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // تذكيرا أذكار الصباح/المساء مستقلّان عن التذكير اليوميّ الرئيسيّ.
                when (intent.action) {
                    ACTION_MORNING_ADHKAR -> {
                        if (settingsRepo.current().enableMorningAdhkar) notifications.showMorningAdhkar()
                        scheduler.rescheduleAdhkar(morning = true)
                        return@launch
                    }
                    ACTION_EVENING_ADHKAR -> {
                        if (settingsRepo.current().enableEveningAdhkar) notifications.showEveningAdhkar()
                        scheduler.rescheduleAdhkar(morning = false)
                        return@launch
                    }
                }
                val s = settingsRepo.current()
                if (s.enableRecitationReminder) notifications.showRecitationReminder()

                if (s.enableWhiteDaysReminder) {
                    val hijriDay = hijriDayOfMonth()
                    when (hijriDay) {
                        12 -> notifications.showWhiteDaysReminder("تبدأ غداً الأيّامُ البيض (13/14/15). صيامها سنّةٌ مستحبّة.")
                        13, 14, 15 -> notifications.showWhiteDaysReminder("اليومُ من الأيّام البيض ($hijriDay). صيامها سنّةٌ مستحبّة.")
                    }
                }

                if (s.enableSunnahReminder) {
                    io.github.salehgnutux.gtsalat.domain.SunnahReminders
                        .messageFor(hijriMonth(), hijriDayOfMonth(), LocalDate.now().dayOfWeek)
                        ?.let { (title, text) -> notifications.showSunnahReminder(title, text) }
                }

                if (s.enableDailyAyah) {
                    content.dailyAyah(LocalDate.now().dayOfYear)?.let {
                        notifications.showDailyAyah(it.surah, it.n, it.text)
                    }
                }
            } finally {
                scheduler.scheduleDailyReminder()   // اليوم التالي
                pending.finish()
            }
        }
    }

    /** يوم الشهر الهجريّ عبر تقويم ICU (أمّ القرى)؛ يعيد -1 إن تعذّر. */
    private fun hijriDayOfMonth(): Int = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) IslamicCalendar().get(IslamicCalendar.DAY_OF_MONTH) else -1
    }.getOrDefault(-1)

    /** الشهر الهجريّ 1..12 (ICU يعيده 0..11 فنزيده 1)؛ يعيد -1 إن تعذّر. */
    private fun hijriMonth(): Int = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) IslamicCalendar().get(IslamicCalendar.MONTH) + 1 else -1
    }.getOrDefault(-1)

    companion object {
        const val ACTION_DAILY_REMINDER = "io.github.salehgnutux.gtsalat.ACTION_DAILY_REMINDER"
        const val ACTION_MORNING_ADHKAR = "io.github.salehgnutux.gtsalat.ACTION_MORNING_ADHKAR"
        const val ACTION_EVENING_ADHKAR = "io.github.salehgnutux.gtsalat.ACTION_EVENING_ADHKAR"
    }
}
