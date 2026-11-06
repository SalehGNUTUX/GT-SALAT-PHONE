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
                val now = System.currentTimeMillis()
                // الموعد المقرَّر: يكشف ما حبسه النظام أو إدارة البطاريّة حتى فتح الهاتف (0 في إنذارٍ قديم).
                val scheduledAt = intent.getLongExtra(EXTRA_SCHEDULED_AT, 0L).takeIf { it > 0L } ?: now
                val late = now - scheduledAt
                when (intent.action) {
                    ACTION_PRENOTIFY -> {
                        val minutes = intent.getIntExtra(EXTRA_MINUTES, s.preNotifyMinutes)
                        val stale = late > MAX_LATE_PRENOTIFY_MS
                        AlarmLog.add(context, "تنبيه اقتراب $prayerAr", scheduledAt, if (stale) "أُسقِط: فات موعده" else "")
                        if (!stale && s.enablePreNotify && !s.doNotDisturb) {
                            // مستوى صوت الأذان يحكم كلّ التذكيرات كذلك. وإشعار الصوت هو المنبثق بزرّ الإيقاف.
                            val sounding = s.enablePreNotifySound &&
                                startAdhanService(context, prayerAr, AdhanService.SOUND_PRENOTIFY, volume = s.adhanVolume)
                            notifications.notify(
                                NotificationHelper.ID_PRENOTIFY,
                                notifications.preNotifyNotification(prayerAr, minutes, silent = sounding),
                            )
                        }
                    }
                    ACTION_ADHAN -> {
                        // الإشعار والصوت أوّلاً، فإذن بدء خدمة المقدّمة بعد الإنذار قصير الأمد. وإعادة
                        // الجدولة (في finally) لا تسبقهما؛ فالصلوات التالية مسلَّحةٌ مقدّماً على أيّ حال.
                        val stale = late > MAX_LATE_ADHAN_MS
                        AlarmLog.add(context, "أذان $prayerAr", scheduledAt, if (stale) "فات موعده: إشعارٌ صامت" else "")
                        notifications.cancelPreNotify()
                        // الكاتم التلقائيّ: مدّته من دخول الوقت، فلا يُكتَم الهاتف إن انقضت.
                        val silenceEnd = scheduledAt + s.silenceMinutes * 60_000L
                        if (s.autoSilence && silenceEnd > now) {
                            ringer.silence()
                            scheduler.scheduleRestoreSound(silenceEnd)
                        }
                        // ذكر ما بعد الصلاة: يُجدوَل بعد دخول الوقت بدقائق.
                        if (s.enablePostDhikr && !stale) {
                            scheduler.schedulePostDhikr(scheduledAt + s.postDhikrMinutes * 60_000L, prayerAr)
                        }
                        if (stale) {
                            // وصل متأخّراً (جمّده النظام حتى فتح الهاتف): نُعلِم بصمتٍ ولا نؤذّن خارج الوقت.
                            if (s.enableSalatNotify && !s.doNotDisturb) {
                                notifications.notify(
                                    NotificationHelper.ID_PRAYER,
                                    notifications.latePrayerNotification(prayerAr, io.github.salehgnutux.gtsalat.util.Format.clock(scheduledAt)),
                                )
                            }
                        } else if (s.enableSalatNotify && !s.doNotDisturb) {
                            // نافذة الأذان ملء الشاشة فوق القفل (إن فُعّلت) — عبر full-screen intent
                            // + محاولة إطلاقٍ مباشر (تعمل والتطبيق نشطٌ في المقدّمة).
                            val fs = if (s.fullScreenAdhan) {
                                launchFullScreen(context, prayerAr, io.github.salehgnutux.gtsalat.util.Format.clock(now), isDhikr = false)
                                notifications.fullScreenAdhanIntent(prayerAr, io.github.salehgnutux.gtsalat.util.Format.clock(now), isDhikr = false)
                            } else null
                            // النمط الفعّال لهذه الصلاة (مخصّصٌ أو عامّ). الصامت: إشعارٌ بلا صوت.
                            val pid = runCatching { PrayerId.valueOf(intent.getStringExtra(EXTRA_PRAYER) ?: "") }.getOrNull()
                            val mode = pid?.let { s.alertFor(it) } ?: s.adhanAlertMode
                            val clock = io.github.salehgnutux.gtsalat.util.Format.clock(now)
                            // مع الصوت: إشعاره هو المنبثق بزرّ الإيقاف ويحمل نافذة ملء الشاشة، ويبقى هذا سجلّاً هادئاً للوقت.
                            val sounding = s.enableAdhanSound && mode != AdhanAlertMode.SILENT &&
                                startAdhanService(context, prayerAr, AdhanService.SOUND_ADHAN, mode, s.adhanVolume, s.fullScreenAdhan, clock)
                            notifications.notify(
                                NotificationHelper.ID_PRAYER,
                                notifications.prayerNotification(prayerAr, fs.takeUnless { sounding }, silent = sounding),
                            )
                        }
                    }
                    ACTION_REFRESH_WIDGETS -> {
                        // إنذارٌ عند حلول الصلاة/منتصف الليل: يقدّم الودجت ثمّ يعيد جدولة نفسه.
                        scheduler.refreshWidgets()
                        scheduler.scheduleWidgetRefresh()
                    }
                    ACTION_TEST -> {
                        AlarmLog.add(context, "تنبيهٌ اختباريّ", scheduledAt)
                        notifications.notify(NotificationHelper.ID_TEST, notifications.testNotification(late / 1000))
                    }
                    ACTION_RESTORE_SOUND -> ringer.restore()
                    ACTION_POST_DHIKR -> {
                        val stale = late > MAX_LATE_DHIKR_MS
                        AlarmLog.add(context, "ذكر بعد صلاة $prayerAr", scheduledAt, if (stale) "أُسقِط: فات موعده" else "")
                        if (!stale && s.enablePostDhikr && !s.doNotDisturb) {
                            val fs = if (s.fullScreenAdhan) {
                                launchFullScreen(context, prayerAr, "", isDhikr = true)
                                notifications.fullScreenAdhanIntent(prayerAr, "", isDhikr = true)
                            } else null
                            val sounding = startAdhanService(context, prayerAr, AdhanService.SOUND_POST_DHIKR, volume = s.adhanVolume, fullScreen = s.fullScreenAdhan)
                            notifications.notify(
                                NotificationHelper.ID_PRAYER,
                                notifications.dhikrNotification(prayerAr, fs.takeUnless { sounding }, silent = sounding),
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                AlarmLog.add(context, "خطأ في معالجة التنبيه", detail = e.toString())
            } finally {
                // إعادة التسليح بعد الأذان حتى لو فشل ما سبق، بلا شبكة.
                if (intent.action == ACTION_ADHAN) {
                    runCatching { scheduler.scheduleNext(allowNetwork = false) }
                        .onFailure { AlarmLog.add(context, "تعذّرت إعادة الجدولة", detail = it.toString()) }
                }
                runCatching { if (wl.isHeld) wl.release() }
                pending.finish()
            }
        }
    }

    /** محاولة إطلاق نافذة ملء الشاشة مباشرةً (تنجح والتطبيق نشطٌ؛ وإلّا يتكفّل الـfull-screen intent). */
    private fun launchFullScreen(context: Context, prayerAr: String, subtitle: String, isDhikr: Boolean) {
        runCatching { context.startActivity(AdhanAlarmActivity.intent(context, prayerAr, subtitle, isDhikr)) }
    }

    /** إطلاق خدمة الأذان المقدّمة بنوع الصوت المطلوب (يعمل والشاشة مغلقة). يعيد نجاح الإطلاق. */
    private fun startAdhanService(
        context: Context,
        prayerAr: String,
        sound: String,
        mode: AdhanAlertMode = AdhanAlertMode.FULL,
        volume: Int = 100,
        fullScreen: Boolean = false,
        subtitle: String = "",
    ): Boolean {
        val svc = Intent(context, AdhanService::class.java).apply {
            putExtra(AdhanService.EXTRA_PRAYER_AR, prayerAr)
            putExtra(AdhanService.EXTRA_SOUND, sound)
            putExtra(AdhanService.EXTRA_ALERT_MODE, mode.name)
            putExtra(AdhanService.EXTRA_VOLUME, volume)
            putExtra(AdhanService.EXTRA_FULL_SCREEN, fullScreen)
            putExtra(AdhanService.EXTRA_SUBTITLE, subtitle)
        }
        return runCatching { ContextCompat.startForegroundService(context, svc) }
            .onFailure { AlarmLog.add(context, "تعذّر تشغيل الصوت", detail = it.toString()) }
            .isSuccess
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
        const val EXTRA_SCHEDULED_AT = "scheduled_at"

        // أقصى تأخّرٍ مقبولٍ للصوت؛ ما بعده يعني أنّ النظام حبس الإنذار، فلا نُسمِعه خارج موعده.
        private const val MAX_LATE_ADHAN_MS = 3 * 60_000L
        private const val MAX_LATE_PRENOTIFY_MS = 2 * 60_000L
        private const val MAX_LATE_DHIKR_MS = 3 * 60_000L
    }
}
