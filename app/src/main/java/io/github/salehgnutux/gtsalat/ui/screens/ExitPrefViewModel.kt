package io.github.salehgnutux.gtsalat.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** تفضيل طريقة الخروج: "" لم تُختَر · "dialog" نافذة تأكيد · "double" نقرتان للرجوع. */
@HiltViewModel
class ExitPrefViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    val exitMode: StateFlow<String> = settingsRepo.settings
        .map { it.exitMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun setExitMode(mode: String) = viewModelScope.launch { settingsRepo.setExitMode(mode) }
}
