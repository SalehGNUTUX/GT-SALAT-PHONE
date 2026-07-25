package io.github.salehgnutux.gtsalat.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.salehgnutux.gtsalat.data.PrayerRepository
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import io.github.salehgnutux.gtsalat.domain.CalendarKind
import io.github.salehgnutux.gtsalat.domain.DayTimetable
import io.github.salehgnutux.gtsalat.domain.GregorianMonths
import io.github.salehgnutux.gtsalat.domain.MonthScheme
import io.github.salehgnutux.gtsalat.util.Format
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class TimetableUi(
    val loading: Boolean = true,
    val monthLabel: String = "",
    val days: List<DayTimetable> = emptyList(),
    val todayIso: String = LocalDate.now().toString(),
    val hasLocation: Boolean = true,
    val calendar: CalendarKind = CalendarKind.HIJRI,
    val scheme: MonthScheme = MonthScheme.STANDARD,
    /** فهرس اليوم داخل قائمة أيّام الشهر المعروض (-1 إن لم يكن ضمنه). */
    val todayIndex: Int = -1,
)

@HiltViewModel
class TimetableViewModel @Inject constructor(
    private val repo: PrayerRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    private var current = YearMonth.now()

    private val _ui = MutableStateFlow(TimetableUi())
    val ui: StateFlow<TimetableUi> = _ui.asStateFlow()

    init { load() }

    fun nextMonth() { current = current.plusMonths(1); load() }
    fun prevMonth() { current = current.minusMonths(1); load() }

    /** العودة لشهر اليوم (تُستدعى عند دخول الشاشة أو تبديل القسم). */
    fun resetToday() {
        if (current != YearMonth.now()) { current = YearMonth.now(); load() } else load()
    }

    private fun load() {
        viewModelScope.launch {
            val s = settingsRepo.current()
            val scheme = GregorianMonths.effective(s.monthScheme, s.country)
            _ui.value = _ui.value.copy(loading = true, monthLabel = Format.monthYear(current.year, current.monthValue, scheme))
            val days = repo.monthTimetable(current.year, current.monthValue)
            val todayIso = LocalDate.now().toString()
            _ui.value = TimetableUi(
                loading = false,
                monthLabel = Format.monthYear(current.year, current.monthValue, scheme),
                days = days,
                todayIso = todayIso,
                hasLocation = days.isNotEmpty(),
                calendar = s.timetableCalendar,
                scheme = scheme,
                todayIndex = days.indexOfFirst { it.dateIso == todayIso },
            )
        }
    }
}
