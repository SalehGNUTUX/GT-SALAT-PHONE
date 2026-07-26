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
import javax.inject.Inject

data class RootState(
    val themeMode: ThemeMode,
    val dynamicColor: Boolean,
    val seedColor: Int,
    val setupCompleted: Boolean,
)

@HiltViewModel
class RootViewModel @Inject constructor(
    settingsRepo: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<RootState?> = settingsRepo.settings
        .map { RootState(it.themeMode, it.dynamicColor, it.seedColor, it.setupCompleted) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
