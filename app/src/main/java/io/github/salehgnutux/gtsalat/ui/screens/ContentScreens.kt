package io.github.salehgnutux.gtsalat.ui.screens

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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.salehgnutux.gtsalat.domain.DuaCategory
import io.github.salehgnutux.gtsalat.domain.HadithCollection
import io.github.salehgnutux.gtsalat.domain.HikamCategory
import io.github.salehgnutux.gtsalat.ui.theme.AmiriQuran

/* ============================ الأحاديث ============================ */

@Composable
fun HadithScreen(onBack: () -> Unit, vm: HadithViewModel = hiltViewModel()) {
    val collections by vm.collections.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("الأربعون والأحاديث", onBack)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            collections.forEach { col: HadithCollection ->
                item(key = "h_" + col.id) {
                    SectionTitle(col.name, "${col.hadiths.size} حديثاً · ${col.author}")
                }
                items(col.hadiths, key = { "${col.id}_${it.n}" }) { h ->
                    ContentCard(
                        number = h.n,
                        title = h.chapter,
                        subtitle = h.narrator,
                        body = h.text,
                        chips = listOfNotNull(h.source.ifBlank { null }, h.grade.ifBlank { null }),
                        onCopy = { clipboard.setText(AnnotatedString(h.text)) },
                    )
                }
            }
        }
    }
}

/* ============================ الأدعية ============================ */

@Composable
fun DuasScreen(onBack: () -> Unit, vm: DuasViewModel = hiltViewModel()) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("الأدعية المأثورة", onBack)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            categories.forEach { cat: DuaCategory ->
                item(key = "d_${cat.id}") {
                    SectionTitle("${cat.icon} ${cat.name}".trim(), "${cat.items.size} دعاءً")
                }
                items(cat.items, key = { "${cat.id}_${it.n}" }) { d ->
                    ContentCard(
                        number = d.n,
                        title = "",
                        subtitle = d.context,
                        body = d.text,
                        chips = listOfNotNull(d.source.ifBlank { null }),
                        onCopy = { clipboard.setText(AnnotatedString(d.text)) },
                    )
                }
            }
        }
    }
}

/* ============================ الحِكَم ============================ */

@Composable
fun HikamScreen(onBack: () -> Unit, vm: HikamViewModel = hiltViewModel()) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("حِكَم ومواعظ السلف", onBack)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            categories.forEach { cat: HikamCategory ->
                item(key = "k_${cat.id}") {
                    SectionTitle(cat.name, "${cat.items.size} حكمة")
                }
                items(cat.items, key = { "${cat.id}_${it.n}" }) { w ->
                    ContentCard(
                        number = w.n,
                        title = "",
                        subtitle = w.sayer,
                        body = w.text,
                        chips = listOfNotNull(w.source.ifBlank { null }),
                        onCopy = { clipboard.setText(AnnotatedString(w.text)) },
                    )
                }
            }
        }
    }
}

/* ============================ عناصر مشتركة ============================ */

@Composable
private fun SectionTitle(title: String, meta: String) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun ContentCard(
    number: Int,
    title: String,
    subtitle: String,
    body: String,
    chips: List<String>,
    onCopy: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    if (title.isNotBlank()) {
                        Text("$number. $title", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
                IconButton(onClick = onCopy) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "نسخ", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                body,
                fontFamily = AmiriQuran,
                fontSize = 21.sp,
                lineHeight = 38.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (chips.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    chips.forEach { c ->
                        AssistChip(
                            onClick = {},
                            label = { Text(c, style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(),
                        )
                    }
                }
            }
        }
    }
}
