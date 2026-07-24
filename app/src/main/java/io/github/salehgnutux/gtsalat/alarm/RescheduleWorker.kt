package io.github.salehgnutux.gtsalat.alarm

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.salehgnutux.gtsalat.data.PrayerRepository

/**
 * عاملٌ دوريّ يُحدّث الكاش المسبق للأشهر ويعيد تسليح إنذار الصلاة القادمة —
 * شبكة أمانٍ إن أُلغيت الإنذارات لأيّ سبب (بطاريّة، قتل النظام…).
 */
@HiltWorker
class RescheduleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repo: PrayerRepository,
    private val scheduler: PrayerAlarmScheduler,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        repo.prefetchMonths(count = 6)
        scheduler.scheduleNext()
        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }
}
