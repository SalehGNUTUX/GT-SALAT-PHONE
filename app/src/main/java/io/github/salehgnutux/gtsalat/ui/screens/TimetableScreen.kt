package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.salehgnutux.gtsalat.domain.CalendarKind
import io.github.salehgnutux.gtsalat.domain.DayTimetable
import io.github.salehgnutux.gtsalat.domain.MonthScheme
import io.github.salehgnutux.gtsalat.domain.PrayerId
import io.github.salehgnutux.gtsalat.util.Format
import java.time.LocalDate

@Composable
fun TimetableScreen(vm: TimetableViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // عند دخول الشاشة (أو تبديل القسم): العودة لشهر اليوم والتمرير لبطاقة صلوات اليوم.
    LaunchedEffect(Unit) { vm.resetToday() }
    LaunchedEffect(ui.todayIndex, ui.days.size) {
        if (ui.todayIndex >= 0) listState.scrollToItem(ui.todayIndex)
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.prevMonth() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "الشهر السابق", tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                ui.monthLabel,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            IconButton(onClick = { vm.nextMonth() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "الشهر التالي", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (!ui.hasLocation) {
            Text("اضبط موقعك أوّلاً من الإعدادات.", Modifier.padding(16.dp))
            return
        }

        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ui.days, key = { it.dateIso }) { day ->
                DayCard(day, isToday = day.dateIso == ui.todayIso, calendar = ui.calendar, scheme = ui.scheme)
            }
        }
    }
}

@Composable
private fun DayCard(day: DayTimetable, isToday: Boolean, calendar: CalendarKind, scheme: MonthScheme) {
    val date = runCatching { LocalDate.parse(day.dateIso) }.getOrNull()
    val gregorian = date?.let { "${Format.weekdayName(it)} ${it.dayOfMonth} ${io.github.salehgnutux.gtsalat.domain.GregorianMonths.monthName(it.monthValue, scheme)} ${it.year}" } ?: day.dateIso
    val hijri = day.hijri?.takeIf { it.isNotBlank() }

    // التقويم المختار أساسٌ، والآخر تحته (فيظهر الميلاديّ الموافق للهجريّ دائماً).
    val primary = if (calendar == CalendarKind.GREGORIAN) gregorian else (hijri ?: gregorian)
    val secondary = if (calendar == CalendarKind.GREGORIAN) hijri else (if (hijri != null) gregorian else null)

    Card(
        Modifier.fillMaxWidth(),
        colors = if (isToday) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                 else CardDefaults.cardColors(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(primary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            if (!secondary.isNullOrBlank()) {
                Text(secondary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf(PrayerId.FAJR, PrayerId.DHUHR, PrayerId.ASR, PrayerId.MAGHRIB, PrayerId.ISHA).forEach { id ->
                    val p = day.time(id)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(id.arabic, style = MaterialTheme.typography.labelSmall)
                        Text(
                            p?.let { Format.clock(it.epochMillis) } ?: "—",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
