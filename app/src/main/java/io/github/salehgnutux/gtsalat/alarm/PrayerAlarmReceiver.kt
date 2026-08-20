package io.github.salehgnutux.gtsalat.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.salehgnutux.gtsalat.audio.AdhanService
import io.github.salehgnutux.gtsalat.audio.RingerController
import io.github.salehgnutux.gtsalat.data.settings.AdhanAlertMode
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import io.github.salehgnutux.gtsalat.domain.PrayerId
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
        // قفل استيقاظ قصير يبقي المعالج حيّاً حتى تنطلق الخدمة وتبدأ الصوت (والشاشة مغلقة).
        val wl = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "gtsalat:alarm")
            .apply { runCatching { acquire(60_000L) } }
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
                            if (s.enablePreNotifySound) {
                                // مستوى صوت الأذان يحكم كلّ التذكيرات كذلك.
                                startAdhanService(context, prayerAr, AdhanService.SOUND_PRENOTIFY, volume = s.adhanVolume)
                            }
                        }
                    }
                    ACTION_ADHAN -> {
                        // جدولة الصلاة التالية أوّلاً (النمط الذاتيّ المتسلسل): لو قُتلت العمليّة
                        // أثناء التشغيل لا تنقطع السلسلة، فالإنذار التالي مُسلَّح قبل أيّ عملٍ قد يتأخّر.
                        scheduler.scheduleNext()
                        val now = System.currentTimeMillis()
                        // الكاتم التلقائيّ: نكتم الرنين ونجدول استعادته بعد مدّة الكتم.
                        if (s.autoSilence) {
                            ringer.silence()
                            scheduler.scheduleRestoreSound(now + s.silenceMinutes * 60_000L)
                        }
                        // ذكر ما بعد الصلاة: يُجدوَل بعد دخول الوقت بدقائق.
                        if (s.enablePostDhikr) {
                            scheduler.schedulePostDhikr(now + s.postDhikrMinutes * 60_000L)
                        }
                        if (s.enableSalatNotify && !s.doNotDisturb) {
                            // نافذة الأذان ملء الشاشة فوق القفل (إن فُعّلت) — عبر full-screen intent
                            // + محاولة إطلاقٍ مباشر (تعمل والتطبيق نشطٌ في المقدّمة).
                            val fs = if (s.fullScreenAdhan) {
                                launchFullScreen(context, prayerAr, io.github.salehgnutux.gtsalat.util.Format.clock(now), isDhikr = false)
                                notifications.fullScreenAdhanIntent(prayerAr, io.github.salehgnutux.gtsalat.util.Format.clock(now), isDhikr = false)
                            } else null
                            notifications.notify(
                                NotificationHelper.ID_PRAYER,
                                notifications.prayerNotification(prayerAr, fs),
                            )
                            // النمط الفعّال لهذه الصلاة (مخصّصٌ أو عامّ). الصامت: إشعارٌ بلا صوت.
                            val pid = runCatching { PrayerId.valueOf(intent.getStringExtra(EXTRA_PRAYER) ?: "") }.getOrNull()
                            val mode = pid?.let { s.alertFor(it) } ?: s.adhanAlertMode
                            if (s.enableAdhanSound && mode != AdhanAlertMode.SILENT) {
                                startAdhanService(context, prayerAr, AdhanService.SOUND_ADHAN, mode, s.adhanVolume)
                            }
                        }
                    }
                    ACTION_REFRESH_WIDGETS -> {
                        // إنذارٌ عند حلول الصلاة/منتصف الليل: يقدّم الودجت ثمّ يعيد جدولة نفسه.
                        scheduler.refreshWidgets()
                        scheduler.scheduleWidgetRefresh()
                    }
                    ACTION_TEST -> notifications.notify(
                        NotificationHelper.ID_TEST,
                        notifications.testNotification(),
                    )
                    ACTION_RESTORE_SOUND -> ringer.restore()
                    ACTION_POST_DHIKR -> {
                        if (s.enablePostDhikr && !s.doNotDisturb) {
                            val fs = if (s.fullScreenAdhan) {
                                launchFullScreen(context, prayerAr, "", isDhikr = true)
                                notifications.fullScreenAdhanIntent(prayerAr, "", isDhikr = true)
                            } else null
                            notifications.notify(
                                NotificationHelper.ID_PRAYER,
                                notifications.dhikrNotification(prayerAr, fs),
                            )
                            startAdhanService(context, prayerAr, AdhanService.SOUND_POST_DHIKR, volume = s.adhanVolume)
                        }
                    }
                }
            } finally {
                runCatching { if (wl.isHeld) wl.release() }
                pending.finish()
            }
        }
    }

    /** محاولة إطلاق نافذة ملء الشاشة مباشرةً (تنجح والتطبيق نشطٌ؛ وإلّا يتكفّل الـfull-screen intent). */
    private fun launchFullScreen(context: Context, prayerAr: String, subtitle: String, isDhikr: Boolean) {
        runCatching { context.startActivity(AdhanAlarmActivity.intent(context, prayerAr, subtitle, isDhikr)) }
    }

    /** إطلاق خدمة الأذان المقدّمة بنوع الصوت المطلوب (يعمل والشاشة مغلقة). */
    private fun startAdhanService(
        context: Context,
        prayerAr: String,
        sound: String,
        mode: AdhanAlertMode = AdhanAlertMode.FULL,
        volume: Int = 100,
    ) {
        val svc = Intent(context, AdhanService::class.java).apply {
            putExtra(AdhanService.EXTRA_PRAYER_AR, prayerAr)
            putExtra(AdhanService.EXTRA_SOUND, sound)
            putExtra(AdhanService.EXTRA_ALERT_MODE, mode.name)
            putExtra(AdhanService.EXTRA_VOLUME, volume)
        }
        runCatching { ContextCompat.startForegroundService(context, svc) }
    }

    companion object {
        const val ACTION_ADHAN = "io.github.salehgnutux.gtsalat.ACTION_ADHAN"
        const val ACTION_PRENOTIFY = "io.github.salehgnutux.gtsalat.ACTION_PRENOTIFY"
        const val ACTION_RESTORE_SOUND = "io.github.salehgnutux.gtsalat.ACTION_RESTORE_SOUND"
        const val ACTION_POST_DHIKR = "io.github.salehgnutux.gtsalat.ACTION_POST_DHIKR"
        const val ACTION_TEST = "io.github.salehgnutux.gtsalat.ACTION_TEST"
        const val ACTION_REFRESH_WIDGETS = "io.github.salehgnutux.gtsalat.ACTION_REFRESH_WIDGETS"
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_PRAYER_AR = "prayer_ar"
        const val EXTRA_MINUTES = "minutes"
    }
}
