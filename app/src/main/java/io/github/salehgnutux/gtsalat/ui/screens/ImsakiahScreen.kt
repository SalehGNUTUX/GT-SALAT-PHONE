package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.salehgnutux.gtsalat.data.PrayerRepository
import io.github.salehgnutux.gtsalat.domain.DayTimetable
import io.github.salehgnutux.gtsalat.domain.PrayerId
import io.github.salehgnutux.gtsalat.domain.Ramadan
import io.github.salehgnutux.gtsalat.util.Format
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import javax.inject.Inject

@HiltViewModel
class ImsakiahViewModel @Inject constructor(
    private val repo: PrayerRepository,
) : ViewModel() {

    data class ImsakRow(val day: Int, val greg: String, val imsak: Long, val fajr: Long, val maghrib: Long)

    private val _rows = MutableStateFlow<List<ImsakRow>>(emptyList())
    val rows: StateFlow<List<ImsakRow>> = _rows.asStateFlow()
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                val range = Ramadan.ramadanGregorianRange() ?: return@runCatching
                val (start, end) = range
                val days = mutableListOf<DayTimetable>()
                var ym = YearMonth.from(start)
                val endYm = YearMonth.from(end)
                while (!ym.isAfter(endYm)) {
                    days += repo.monthTimetable(ym.year, ym.monthValue)
                    ym = ym.plusMonths(1)
                }
                val imsakMs = Ramadan.IMSAK_BEFORE_FAJR_MIN * 60_000L
                _rows.value = days.mapNotNull { d ->
                    val date = runCatching { LocalDate.parse(d.dateIso) }.getOrNull() ?: return@mapNotNull null
                    if (date.isBefore(start) || date.isAfter(end)) return@mapNotNull null
                    val fajr = d.time(PrayerId.FAJR)?.epochMillis ?: return@mapNotNull null
                    val maghrib = d.time(PrayerId.MAGHRIB)?.epochMillis ?: return@mapNotNull null
                    val rDay = runCatching { HijrahDate.from(date).get(ChronoField.DAY_OF_MONTH) }.getOrDefault(0)
                    ImsakRow(rDay, "${date.dayOfMonth}/${date.monthValue}", fajr - imsakMs, fajr, maghrib)
                }.sortedBy { it.day }
            }
            _loading.value = false
        }
    }
}

@Composable
fun ImsakiahScreen(onBack: () -> Unit, vm: ImsakiahViewModel = hiltViewModel()) {
    val rows by vm.rows.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("إمساكيّة رمضان", onBack)
        when {
            loading -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
            rows.isEmpty() -> Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("🌙", style = MaterialTheme.typography.displaySmall)
                Text(
                    "تعذّر بناء الإمساكيّة.\nتأكّد من تحديد موقعك في الإعدادات.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                item("head") {
                    Row(
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(vertical = 10.dp, horizontal = 8.dp),
                    ) {
                        HeadCell("رمضان"); HeadCell("الميلاديّ"); HeadCell("الإمساك"); HeadCell("الفجر"); HeadCell("الإفطار")
                    }
                }
                items(rows, key = { it.day }) { r ->
                    val isToday = r.day == Ramadan.dayOfRamadan()
                    Row(
                        Modifier.fillMaxWidth()
                            .background(if (isToday) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface)
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BodyCell("${r.day}", bold = isToday)
                        BodyCell(r.greg)
                        BodyCell(Format.clock(r.imsak))
                        BodyCell(Format.clock(r.fajr))
                        BodyCell(Format.clock(r.maghrib), bold = true)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeadCell(text: String) {
    Text(text, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, textAlign = TextAlign.Center)
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BodyCell(text: String, bold: Boolean = false) {
    Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
}
