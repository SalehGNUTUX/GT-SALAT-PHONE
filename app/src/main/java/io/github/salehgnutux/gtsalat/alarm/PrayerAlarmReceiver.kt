package io.github.salehgnutux.gtsalat.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.salehgnutux.gtsalat.audio.AdhanService
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import io.github.salehgnutux.gtsalat.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PrayerAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var notifications: NotificationHelper
    @Inject lateinit var scheduler: PrayerAlarmScheduler
    @Inject lateinit var settingsRepo: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val prayerAr = intent.getStringExtra(EXTRA_PRAYER_AR) ?: "الصلاة"
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val s = settingsRepo.current()
                when (intent.action) {
                    ACTION_PRENOTIFY -> {
                        val minutes = intent.getIntExtra(EXTRA_MINUTES, s.preNotifyMinutes)
                        if (s.enablePreNotify && !s.doNotDisturb) {
                            notifications.notify(
                                NotificationHelper.ID_PRENOTIFY,
                                notifications.preNotifyNotification(prayerAr, minutes),
                            )
                        }
                    }
                    ACTION_ADHAN -> {
                        // جدولة الصلاة التالية أوّلاً (النمط الذاتيّ المتسلسل): لو قُتلت العمليّة
                        // أثناء التشغيل لا تنقطع السلسلة، فالإنذار التالي مُسلَّح قبل أيّ عملٍ قد يتأخّر.
                        scheduler.scheduleNext()
                        if (s.enableSalatNotify && !s.doNotDisturb) {
                            notifications.notify(
                                NotificationHelper.ID_PRAYER,
                                notifications.prayerNotification(prayerAr),
                            )
                            if (s.enableAdhanSound) {
                                val svc = Intent(context, AdhanService::class.java).apply {
                                    putExtra(AdhanService.EXTRA_PRAYER_AR, prayerAr)
                                }
                                // في مسار السقوط (إنذار غير دقيق) قد تمنع أندرويد 12+ إطلاق خدمة
                                // المقدّمة من الخلفيّة؛ نلتقط الاستثناء بدل الانهيار (الإشعار ظاهرٌ أصلاً).
                                runCatching { ContextCompat.startForegroundService(context, svc) }
                            }
                        }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_ADHAN = "io.github.salehgnutux.gtsalat.ACTION_ADHAN"
        const val ACTION_PRENOTIFY = "io.github.salehgnutux.gtsalat.ACTION_PRENOTIFY"
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_PRAYER_AR = "prayer_ar"
        const val EXTRA_MINUTES = "minutes"
    }
}
