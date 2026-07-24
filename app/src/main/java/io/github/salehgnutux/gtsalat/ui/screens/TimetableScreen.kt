package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.salehgnutux.gtsalat.domain.DayTimetable
import io.github.salehgnutux.gtsalat.domain.PrayerId
import io.github.salehgnutux.gtsalat.util.Format

@Composable
fun TimetableScreen(vm: TimetableViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // في RTL: الزرّ الأيمن للشهر السابق
            IconButton(onClick = { vm.prevMonth() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "الشهر السابق")
            }
            Text(ui.monthLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = { vm.nextMonth() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "الشهر التالي")
            }
        }

        if (!ui.hasLocation) {
            Text("اضبط موقعك أوّلاً من الإعدادات.", Modifier.padding(16.dp))
            return
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ui.days) { day -> DayCard(day, isToday = day.dateIso == ui.todayIso) }
        }
    }
}

@Composable
private fun DayCard(day: DayTimetable, isToday: Boolean) {
    Card(
        Modifier.fillMaxWidth(),
        colors = if (isToday) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                 else CardDefaults.cardColors(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                day.hijri ?: day.dateIso,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
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
