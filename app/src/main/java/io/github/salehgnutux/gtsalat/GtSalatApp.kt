package io.github.salehgnutux.gtsalat

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import io.github.salehgnutux.gtsalat.alarm.WorkScheduler
import io.github.salehgnutux.gtsalat.notification.NotificationHelper
import javax.inject.Inject

@HiltAndroidApp
class GtSalatApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notifications: NotificationHelper
    @Inject lateinit var workScheduler: WorkScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        notifications.ensureChannels()
        workScheduler.ensurePeriodic()
    }
}
