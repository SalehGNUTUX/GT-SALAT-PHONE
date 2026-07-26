package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
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
import kotlinx.coroutines.launch

private const val I_DHIKR = 3
private const val I_HIKMAH = 4

@Composable
fun DashboardScreen(vm: DashboardViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // إعادة النقر على «الرئيسيّة» → التمرير لرأس الصفحة.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        io.github.salehgnutux.gtsalat.ui.UiEvents.scrollHomeToTop.collect {
            listState.animateScrollToItem(0)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 0 — البطاقة الرئيسيّة (ساعة + تاريخان + الصلاة القادمة) بخلفيّةٍ متدرّجة
        item { HeroCard(ui) }

        // 1 — زرّا ذكر اليوم وحكمة اليوم (ينزلان للمحتوى أسفل الصفحة)
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = { scope.launch { listState.animateScrollToItem(I_DHIKR) } },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.FavoriteBorder, contentDescription = null)
                    Text("  ذكر اليوم")
                }
                FilledTonalButton(
                    onClick = { scope.launch { listState.animateScrollToItem(I_HIKMAH) } },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                    Text("  حكمة اليوم")
                }
            }
        }

        // 2 — قائمة صلوات اليوم والليلة
        item { TodayPrayersCard(ui) }

        // 3 — ذكر اليوم (قابل للتجديد والنسخ)
        item {
            DailyCard(
                title = "ذكر اليوم",
                body = ui.dhikr,
                onRefresh = { vm.refreshDhikr() },
                refreshLabel = "ذكر جديد",
            )
        }

        // 4 — حكمة اليوم
        item {
            val h = ui.hikmah
            DailyCard(
                title = "حكمة اليوم",
                body = h?.text ?: "…",
                caption = h?.let { listOfNotNull(it.sayer.ifBlank { null }, it.source.ifBlank { null }).joinToString(" — ") },
                onRefresh = { vm.refreshHikmah() },
                refreshLabel = "حكمة أخرى",
            )
        }

        // 5 — تذييل
        item {
            Text(
                "تعمل المواقيت والأذان دون إنترنت بعد الإعداد.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HeroCard(ui: DashboardUi) {
    val cs = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    val brush = Brush.verticalGradient(
        listOf(
            lerp(cs.primaryContainer, cs.primary, if (dark) 0.35f else 0.14f),
            cs.primaryContainer,
        ),
    )
    val onHero = cs.onPrimaryContainer

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(
            Modifier.fillMaxWidth().background(brush).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // الموقع + الساعة الحيّة
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = onHero, modifier = Modifier.height(18.dp))
                    Text(
                        if (ui.city.isNotBlank()) ui.city else "موقعك",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onHero,
                    )
                }
                Text(ui.clock, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = onHero)
            }
            // التاريخان
            ui.hijri?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = onHero, fontWeight = FontWeight.Bold) }
            if (ui.gregorian.isNotBlank()) {
                Text(ui.gregorian, style = MaterialTheme.typography.bodySmall, color = onHero.copy(alpha = 0.8f))
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = onHero.copy(alpha = 0.25f))

            // الصلاة القادمة
            Text("الصلاة القادمة", style = MaterialTheme.typography.labelLarge, color = onHero.copy(alpha = 0.85f))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(
                    ui.next?.prayer?.id?.arabic ?: "—",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = onHero,
                )
                Text(
                    ui.next?.let { Format.clock(it.prayer.epochMillis) } ?: "",
                    style = MaterialTheme.typography.headlineSmall,
                    color = onHero,
                )
            }
            if (ui.countdownText.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("المتبقّي", style = MaterialTheme.typography.labelMedium, color = onHero.copy(alpha = 0.85f))
                    Text(ui.countdownText, style = CountdownStyle, fontWeight = FontWeight.Bold, color = onHero)
                }
            }
        }
    }
}

@Composable
private fun TodayPrayersCard(ui: DashboardUi) {
    val today = ui.today
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 6.dp)) {
            Text(
                "مواقيت اليوم والليلة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            if (today == null) {
                Text(
                    "حدّد موقعك من الإعدادات لعرض المواقيت.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
                return@Card
            }
            today.prayers.forEach { p ->
                val isNext = ui.next?.prayer?.id == p.id
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
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
                        color = when {
                            p.id == PrayerId.SUNRISE -> MaterialTheme.colorScheme.outline
                            isNext -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
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
