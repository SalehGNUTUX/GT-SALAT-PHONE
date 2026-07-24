package io.github.salehgnutux.gtsalat.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.salehgnutux.gtsalat.alarm.PrayerAlarmScheduler
import io.github.salehgnutux.gtsalat.data.PrayerRepository
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import io.github.salehgnutux.gtsalat.domain.AsrMadhab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUi(
    val detecting: Boolean = false,
    val city: String = "",
    val country: String = "",
    val hasLocation: Boolean = false,
    val methodId: Int = 3,
    val madhab: AsrMadhab = AsrMadhab.SHAFI,
    val error: String? = null,
    val done: Boolean = false,
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val repo: PrayerRepository,
    private val settingsRepo: SettingsRepository,
    private val scheduler: PrayerAlarmScheduler,
) : ViewModel() {

    private val _ui = MutableStateFlow(SetupUi())
    val ui: StateFlow<SetupUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val s = settingsRepo.current()
            _ui.value = _ui.value.copy(
                city = s.city, country = s.country, hasLocation = s.hasLocation,
                methodId = s.methodId, madhab = s.madhab,
            )
        }
    }

    fun detectLocation() {
        _ui.value = _ui.value.copy(detecting = true, error = null)
        viewModelScope.launch {
            val loc = repo.detectAndSaveLocation()
            val s = settingsRepo.current()
            _ui.value = if (loc != null) {
                _ui.value.copy(
                    detecting = false, hasLocation = true,
                    city = loc.city, country = loc.country, methodId = s.methodId,
                )
            } else {
                _ui.value.copy(detecting = false, error = "تعذّر اكتشاف الموقع. تحقّق من الإذن أو الاتّصال.")
            }
        }
    }

    fun setMethod(id: Int) {
        _ui.value = _ui.value.copy(methodId = id)
        viewModelScope.launch { settingsRepo.setMethod(id) }
    }

    fun setMadhab(m: AsrMadhab) {
        _ui.value = _ui.value.copy(madhab = m)
        viewModelScope.launch { settingsRepo.setMadhab(m) }
    }

    fun finish() {
        viewModelScope.launch {
            settingsRepo.setSetupCompleted(true)
            repo.prefetchMonths(6)
            scheduler.scheduleNext()
            _ui.value = _ui.value.copy(done = true)
        }
    }
}
