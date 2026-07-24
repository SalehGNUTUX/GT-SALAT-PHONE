package io.github.salehgnutux.gtsalat.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * تُمحى إنذارات AlarmManager عند إعادة التشغيل، لذا نعيد تسليحها هنا
 * ونعيد جدولة العامل الدوريّ بعد الإقلاع أو تحديث التطبيق.
 * كذلك نعيد التسليح عند تغيير الوقت أو المنطقة الزمنيّة يدويّاً، فالإنذارات
 * المُطلَقة قد تصبح على أساسٍ خاطئ (شبكة أمانٍ فوريّة قبل العامل الدوريّ).
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: PrayerAlarmScheduler
    @Inject lateinit var workScheduler: WorkScheduler

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.LOCKED_BOOT_COMPLETED",
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        scheduler.scheduleNext()
                        workScheduler.ensurePeriodic()
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
