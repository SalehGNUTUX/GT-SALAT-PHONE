package io.github.salehgnutux.gtsalat.ui.screens

import android.icu.util.IslamicCalendar
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.salehgnutux.gtsalat.data.ContentRepository
import io.github.salehgnutux.gtsalat.domain.HistoryEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventsUi(val all: List<HistoryEvent> = emptyList(), val today: List<HistoryEvent> = emptyList())

@HiltViewModel
class EventsViewModel @Inject constructor(private val repo: ContentRepository) : ViewModel() {
    private val _ui = MutableStateFlow(EventsUi())
    val ui: StateFlow<EventsUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val all = repo.events()
            val today = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                runCatching {
                    val c = IslamicCalendar()
                    repo.eventsToday(c.get(IslamicCalendar.MONTH) + 1, c.get(IslamicCalendar.DAY_OF_MONTH))
                }.getOrDefault(emptyList())
            } else emptyList()
            _ui.value = EventsUi(all, today)
        }
    }
}

@Composable
fun EventsScreen(onBack: () -> Unit, vm: EventsViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val q = query.trim()
    val results = remember(q, ui.all) {
        if (q.isBlank()) ui.all
        else ui.all.filter { it.title.contains(q) || it.text.contains(q) || it.year.contains(q) }
    }

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("أحداثٌ تاريخيّة", onBack)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("ابحث في الأحداث (اسمٌ أو سنة أو كلمة)…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (q.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, contentDescription = "مسح") }
            },
            singleLine = true,
        )
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (q.isBlank() && ui.today.isNotEmpty()) {
                item {
                    Text("حدث اليوم في التاريخ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                items(ui.today, key = { "t_${it.title}" }) { EventCard(it, highlight = true) }
                item {
                    Text("كلّ الأحداث (${ui.all.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                }
            } else if (q.isNotBlank()) {
                item {
                    Text(if (results.isEmpty()) "لا نتائج لـ«$q»" else "${results.size} نتيجة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            items(results, key = { "a_${it.title}" }) { EventCard(it, highlight = false) }
        }
    }
}

@Composable
private fun EventCard(e: HistoryEvent, highlight: Boolean) {
    Card(
        Modifier.fillMaxWidth(),
        colors = if (highlight) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                 else CardDefaults.cardColors(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Text(e.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(e.year, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (e.text.isNotBlank()) {
                Text(e.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
