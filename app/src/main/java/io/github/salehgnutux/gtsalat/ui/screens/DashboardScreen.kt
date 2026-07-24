package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.salehgnutux.gtsalat.domain.PrayerId
import io.github.salehgnutux.gtsalat.ui.theme.AmiriQuran
import io.github.salehgnutux.gtsalat.ui.theme.CountdownStyle
import io.github.salehgnutux.gtsalat.util.Format

@Composable
fun DashboardScreen(vm: DashboardViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    if (ui.loading) {
        Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ترويسة الموقع والتاريخ (هجريّ + ميلاديّ)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (ui.city.isNotBlank()) ui.city else "موقعك",
                style = MaterialTheme.typography.titleMedium,
            )
            ui.hijri?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            if (ui.gregorian.isNotBlank()) {
                Text(ui.gregorian, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }

        // ذكر اليوم — قابل للتجديد والنسخ
        DailyCard(
            title = "ذكر اليوم",
            body = ui.dhikr,
            onRefresh = { vm.refreshDhikr() },
            refreshLabel = "ذكر جديد",
        )

        // حكمة اليوم — قابلة للتجديد والنسخ
        ui.hikmah?.takeIf { it.text.isNotBlank() }?.let { h ->
            DailyCard(
                title = "حكمة اليوم",
                body = h.text,
                caption = listOfNotNull(h.sayer.ifBlank { null }, h.source.ifBlank { null }).joinToString(" — "),
                onRefresh = { vm.refreshHikmah() },
                refreshLabel = "حكمة أخرى",
            )
        }

        // بطاقة الصلاة القادمة
        val next = ui.next
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("الصلاة القادمة", style = MaterialTheme.typography.labelLarge)
                Text(
                    next?.prayer?.id?.arabic ?: "—",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    next?.let { Format.clock(it.prayer.epochMillis) } ?: "",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text("المتبقّي", style = MaterialTheme.typography.labelMedium)
                Text(ui.countdownText, style = CountdownStyle, fontWeight = FontWeight.Bold)
            }
        }

        // مواقيت اليوم
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 6.dp)) {
                ui.today?.prayers?.forEach { p ->
                    val isNext = next?.prayer?.id == p.id
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            p.id.arabic,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                            color = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            Format.clock(p.epochMillis),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                            color = if (p.id == PrayerId.SUNRISE) MaterialTheme.colorScheme.outline
                                    else if (isNext) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        Text(
            "تعمل المواقيت والأذان دون إنترنت بعد الإعداد.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** بطاقة محتوى يوميّ (ذكر/حكمة) بنصٍّ بخطّ أميري وأزرار تجديدٍ ونسخ. */
@Composable
private fun DailyCard(
    title: String,
    body: String,
    onRefresh: () -> Unit,
    refreshLabel: String,
    caption: String? = null,
) {
    val clipboard = LocalClipboardManager.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { clipboard.setText(AnnotatedString(body)) }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "نسخ")
                    }
                    TextButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = refreshLabel)
                        Text("  $refreshLabel")
                    }
                }
            }
            Text(
                body.ifBlank { "…" },
                fontFamily = AmiriQuran,
                fontSize = 22.sp,
                lineHeight = 38.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
            if (!caption.isNullOrBlank()) {
                Text(
                    caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
