package io.github.salehgnutux.gtsalat.alarm

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.salehgnutux.gtsalat.data.PrayerRepository
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import io.github.salehgnutux.gtsalat.util.Connectivity

/**
 * عاملٌ دوريّ يُحدّث الكاش المسبق للأشهر ويعيد تسليح إنذار الصلاة القادمة —
 * شبكة أمانٍ إن أُلغيت الإنذارات لأيّ سبب (بطاريّة، قتل النظام…). ويُحدّث الموقع
 * تلقائيّاً (إن فُعّل الخيار) **والتطبيق مغلق**، عند توفّر الإنترنت أو GPS.
 */
@HiltWorker
class RescheduleWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val repo: PrayerRepository,
    private val scheduler: PrayerAlarmScheduler,
    private val settingsRepo: SettingsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        // تحديث الموقع تلقائيّاً في الخلفيّة (إن فُعّل) عند توفّر إنترنت أو GPS.
        if (runCatching { settingsRepo.current().autoUpdateLocation }.getOrDefault(false) &&
            Connectivity.canLocate(context)
        ) {
            runCatching { repo.detectAndSaveLocation(auto = true) }
        }
        repo.prefetchMonths(count = 6)
        scheduler.scheduleNext()
        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }
}
