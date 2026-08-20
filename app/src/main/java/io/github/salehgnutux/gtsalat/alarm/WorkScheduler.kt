package io.github.salehgnutux.gtsalat.alarm

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun ensurePeriodic() {
        // كلّ ٣ ساعات (شبكة أمان أكثف): يعيد تسليح الإنذارات ويحدّث الكاش إن جمّد النظامُ التطبيقَ فانقطعت السلسلة.
        // UPDATE (لا KEEP) ليأخذ التغيير مفعولَه على التثبيتات القديمة المُسلَّحة بالفترة السابقة.
        val request = PeriodicWorkRequestBuilder<RescheduleWorker>(3, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    companion object {
        private const val UNIQUE_NAME = "gt_salat_reschedule"
    }
}
