package io.github.salehgnutux.gtsalat.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.salehgnutux.gtsalat.data.PrayerRepository
import io.github.salehgnutux.gtsalat.domain.DayTimetable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class TimetableUi(
    val loading: Boolean = true,
    val monthLabel: String = "",
    val days: List<DayTimetable> = emptyList(),
    val todayIso: String = LocalDate.now().toString(),
    val hasLocation: Boolean = true,
)

@HiltViewModel
class TimetableViewModel @Inject constructor(
    private val repo: PrayerRepository,
) : ViewModel() {

    private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ar"))
    private var current = YearMonth.now()

    private val _ui = MutableStateFlow(TimetableUi())
    val ui: StateFlow<TimetableUi> = _ui.asStateFlow()

    init { load() }

    fun nextMonth() { current = current.plusMonths(1); load() }
    fun prevMonth() { current = current.minusMonths(1); load() }

    private fun load() {
        _ui.value = _ui.value.copy(loading = true, monthLabel = current.atDay(1).format(monthFmt))
        viewModelScope.launch {
            val days = repo.monthTimetable(current.year, current.monthValue)
            _ui.value = TimetableUi(
                loading = false,
                monthLabel = current.atDay(1).format(monthFmt),
                days = days,
                hasLocation = days.isNotEmpty(),
            )
        }
    }
}
