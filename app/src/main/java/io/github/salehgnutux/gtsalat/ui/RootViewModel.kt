package io.github.salehgnutux.gtsalat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import io.github.salehgnutux.gtsalat.data.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RootState(
    val themeMode: ThemeMode,
    val dynamicColor: Boolean,
    val seedColor: Int,
    val gradTopLight: Int,
    val gradBotLight: Int,
    val gradTopDark: Int,
    val gradBotDark: Int,
    val clock24h: Boolean,
    val setupCompleted: Boolean,
)

@HiltViewModel
class RootViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val updateRepo: io.github.salehgnutux.gtsalat.data.UpdateRepository,
    private val repo: io.github.salehgnutux.gtsalat.data.PrayerRepository,
    private val scheduler: io.github.salehgnutux.gtsalat.alarm.PrayerAlarmScheduler,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
) : ViewModel() {
    val state: StateFlow<RootState?> = settingsRepo.settings
        .map {
            RootState(
                it.themeMode, it.dynamicColor, it.seedColor,
                it.gradTopLight, it.gradBotLight, it.gradTopDark, it.gradBotDark,
                it.clock24h, it.setupCompleted,
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // فحص التحديثات عند الإقلاع ثمّ دوريّاً كلّ 3 ساعات ما دام التطبيق مفتوحاً —
        // فيظهر شريط التحديث دون الحاجة إلى إغلاق التطبيق وإعادة فتحه.
        viewModelScope.launch {
            while (isActive) {
                runCatching { updateRepo.checkForUpdate() }
                // تحديث الموقع تلقائيّاً (إن فُعّل): عند فتح التطبيق ودوريّاً — للمتنقّلين كثيراً.
                // بوّابةٌ خفيفة: لا نبذل جهداً إن غاب الإنترنت وGPS معاً.
                if (runCatching { settingsRepo.current().autoUpdateLocation }.getOrDefault(false) &&
                    io.github.salehgnutux.gtsalat.util.Connectivity.canLocate(appContext)
                ) {
                    runCatching {
                        val loc = repo.detectAndSaveLocation()
                        if (loc != null) { repo.prefetchMonths(3); scheduler.scheduleNext() }
                    }
                }
                kotlinx.coroutines.delay(3 * 60 * 60 * 1000L)
            }
        }
    }
}
