package io.github.salehgnutux.gtsalat.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.salehgnutux.gtsalat.BuildConfig
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** يقرّر عرض «ما الجديد» مرّةً بعد تحديث الإصدار (مقارنة versionCode المخزَّن بالحاليّ). */
@HiltViewModel
class WhatsNewViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    val shouldShow: StateFlow<Boolean> = settingsRepo.settings
        .map { it.lastWhatsNewCode != BuildConfig.VERSION_CODE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun markShown() = viewModelScope.launch { settingsRepo.setLastWhatsNewCode(BuildConfig.VERSION_CODE) }
}
