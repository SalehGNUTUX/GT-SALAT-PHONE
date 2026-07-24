package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.salehgnutux.gtsalat.ui.theme.AmiriQuran

@Composable
fun AdhkarSessionScreen(onBack: () -> Unit, vm: AdhkarSessionViewModel = hiltViewModel()) {
    val remaining by vm.remaining.collectAsStateWithLifecycle()
    val title = if (vm.isEvening) "أذكار المساء" else "أذكار الصباح"
    val doneCount = remaining.count { it == 0 }
    val total = vm.items.size

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) { SubScreenHeader(title, onBack) }
            TextButton(onClick = { vm.reset() }) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Text("  تصفير")
            }
        }

        // شريط تقدّم الجلسة
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                "أكملتَ $doneCount من $total",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else doneCount.toFloat() / total },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(vm.items) { i, dhikr ->
                val left = remaining.getOrElse(i) { dhikr.count }
                val done = left == 0
                Card(
                    Modifier.fillMaxWidth().clickable(enabled = !done) { vm.tap(i) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (done) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            dhikr.text,
                            fontFamily = AmiriQuran,
                            fontSize = 22.sp,
                            lineHeight = 40.sp,
                            color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text(
                                if (dhikr.count > 1) "العدد المأثور: ${dhikr.count}" else "مرّة واحدة",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.outline,
                            )
                            CounterBadge(left = left, total = dhikr.count, done = done)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CounterBadge(left: Int, total: Int, done: Boolean) {
    Box(
        Modifier.size(56.dp).clip(CircleShape)
            .background(if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Icon(Icons.Filled.Check, contentDescription = "اكتمل", tint = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text(
                "$left",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
