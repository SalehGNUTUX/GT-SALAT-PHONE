package io.github.salehgnutux.gtsalat.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.salehgnutux.gtsalat.audio.AdhanService
import io.github.salehgnutux.gtsalat.audio.RingerController
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
    @Inject lateinit var ringer: RingerController

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
                        // الكاتم التلقائيّ: نكتم الرنين ونجدول استعادته بعد مدّة الكتم (المنبّه/الأذان يبقى مسموعاً).
                        if (s.autoSilence) {
                            ringer.silence()
                            scheduler.scheduleRestoreSound(
                                System.currentTimeMillis() + s.silenceMinutes * 60_000L,
                            )
                        }
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
                    ACTION_RESTORE_SOUND -> ringer.restore()
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_ADHAN = "io.github.salehgnutux.gtsalat.ACTION_ADHAN"
        const val ACTION_PRENOTIFY = "io.github.salehgnutux.gtsalat.ACTION_PRENOTIFY"
        const val ACTION_RESTORE_SOUND = "io.github.salehgnutux.gtsalat.ACTION_RESTORE_SOUND"
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_PRAYER_AR = "prayer_ar"
        const val EXTRA_MINUTES = "minutes"
    }
}
