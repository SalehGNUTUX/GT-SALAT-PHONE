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
    settingsRepo: SettingsRepository,
    private val updateRepo: io.github.salehgnutux.gtsalat.data.UpdateRepository,
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
                kotlinx.coroutines.delay(3 * 60 * 60 * 1000L)
            }
        }
    }
}
