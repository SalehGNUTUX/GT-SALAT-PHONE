package io.github.salehgnutux.gtsalat.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.salehgnutux.gtsalat.data.PrayerRepository
import io.github.salehgnutux.gtsalat.data.settings.AppSettings
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import io.github.salehgnutux.gtsalat.domain.DayTimetable
import io.github.salehgnutux.gtsalat.domain.NextPrayer
import io.github.salehgnutux.gtsalat.domain.PrayerCalculator
import io.github.salehgnutux.gtsalat.domain.PrayerId
import io.github.salehgnutux.gtsalat.domain.PrayerTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DashboardUi(
    val loading: Boolean = true,
    val hasLocation: Boolean = false,
    val city: String = "",
    val today: DayTimetable? = null,
    val hijri: String? = null,
    val next: NextPrayer? = null,
    val countdownText: String = "",
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: PrayerRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(DashboardUi())
    val ui: StateFlow<DashboardUi> = _ui.asStateFlow()

    private var settings: AppSettings? = null
    private var today: DayTimetable? = null
    private var tomorrowFajr: PrayerTime? = null

    init {
        viewModelScope.launch {
            settingsRepo.settings.collectLatest { s ->
                settings = s
                loadDay(s)
            }
        }
        viewModelScope.launch {
            while (true) {
                tickUpdate()
                delay(1000)
            }
        }
    }

    private suspend fun loadDay(s: AppSettings) {
        if (!s.hasLocation) {
            _ui.value = DashboardUi(loading = false, hasLocation = false)
            today = null
            return
        }
        today = repo.todayTimetable()
        tomorrowFajr = PrayerCalculator
            .computeDay(LocalDate.now().plusDays(1), s.lat!!, s.lon!!, s.methodId, s.madhab)
            .time(PrayerId.FAJR)
        tickUpdate()
    }

    private fun tickUpdate() {
        val s = settings ?: return
        val t = today
        if (!s.hasLocation || t == null) {
            _ui.value = _ui.value.copy(loading = false, hasLocation = s.hasLocation)
            return
        }
        val now = System.currentTimeMillis()
        val next = PrayerCalculator.nextPrayer(t, tomorrowFajr, now)
        _ui.value = DashboardUi(
            loading = false,
            hasLocation = true,
            city = s.city,
            today = t,
            hijri = t.hijri,
            next = next,
            countdownText = next?.let {
                io.github.salehgnutux.gtsalat.util.Format.countdown(it.remainingMillis)
            } ?: "",
        )
    }
}
