package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MenuBook
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
private const val I_AYAH = 5

@Composable
fun DashboardScreen(onOpenSettings: () -> Unit = {}, vm: DashboardViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val tick by vm.tick.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // دخول الشاشة (أو العودة إليها من قسمٍ آخر) يبدأ من رأس الصفحة — لا من آخر موضعٍ محفوظ.
    androidx.compose.runtime.LaunchedEffect(Unit) { listState.scrollToItem(0) }
    // إعادة النقر على «الرئيسيّة» وهي ظاهرة → التمرير لرأس الصفحة.
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
        // 0 — صفٌّ علويٌّ (اختصار الإعدادات + تبديل السِمة) ثمّ البطاقة الرئيسيّة (بلا تغيير المؤشّرات)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "الإعدادات", tint = MaterialTheme.colorScheme.primary)
                    }
                    Text("GT-SALAT", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ClockFormatToggleButton()
                        ThemeToggleButton()
                    }
                }
                UpdateBanner()
                HeroCard(ui, tick)
            }
        }

        // 1 — أزرار ذكر/حكمة/آية اليوم (تنزل للمحتوى أسفل الصفحة) — متّسقةٌ بسطرٍ واحد
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                DailyNavButton("ذكر اليوم", Icons.Outlined.FavoriteBorder, Modifier.weight(1f)) {
                    scope.launch { listState.animateScrollToItem(I_DHIKR) }
                }
                DailyNavButton("حكمة اليوم", Icons.Outlined.AutoAwesome, Modifier.weight(1f)) {
                    scope.launch { listState.animateScrollToItem(I_HIKMAH) }
                }
                if (ui.showAyah) {
                    DailyNavButton("آية اليوم", Icons.Outlined.MenuBook, Modifier.weight(1f)) {
                        scope.launch { listState.animateScrollToItem(I_AYAH) }
                    }
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

        // 5 — آية اليوم (قابلة للتجديد والنسخ)
        if (ui.showAyah) {
            item {
                val a = ui.ayah
                DailyCard(
                    title = "آية اليوم",
                    body = a?.text ?: "…",
                    caption = a?.let { "سورة ${it.surah} — الآية ${it.n}" },
                    onRefresh = { vm.refreshAyah() },
                    refreshLabel = "آية أخرى",
                )
            }
        }

        // 6 — تذييل
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

/** زرُّ تنقّلٍ يوميّ متّسق: أيقونةٌ فوق نصٍّ بسطرٍ واحد (فلا يلتفّ ولا يصير دائرة). */
@Composable
private fun DailyNavButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp, horizontal = 4.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(icon, contentDescription = null)
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
private fun HeroCard(ui: DashboardUi, tick: DashboardTick) {
    val cs = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    val brush = androidx.compose.runtime.remember(cs.primaryContainer, cs.primary, dark) {
        Brush.verticalGradient(
            listOf(
                lerp(cs.primaryContainer, cs.primary, if (dark) 0.35f else 0.14f),
                cs.primaryContainer,
            ),
        )
    }
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
                Text(tick.clock, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = onHero)
            }
            // التاريخان
            ui.hijri?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = onHero, fontWeight = FontWeight.Bold) }
            if (ui.gregorian.isNotBlank()) {
                Text(ui.gregorian, style = MaterialTheme.typography.bodySmall, color = onHero.copy(alpha = 0.8f))
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = onHero.copy(alpha = 0.25f))

            // الصلاة القادمة + حلقة تقدّمٍ حيّة تمتلئ باقتراب الوقت
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("الصلاة القادمة", style = MaterialTheme.typography.labelLarge, color = onHero.copy(alpha = 0.85f))
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
                CountdownRing(
                    progress = tick.progress,
                    countdown = tick.countdownText,
                    ringColor = onHero,
                    trackColor = onHero.copy(alpha = 0.22f),
                    labelColor = onHero.copy(alpha = 0.85f),
                )
            }
        }
    }
}

/** حلقةٌ دائريّةٌ تُظهر نسبة انقضاء الوقت حتى الصلاة القادمة، والعدّاد المتبقّي في مركزها. */
@Composable
private fun CountdownRing(
    progress: Float,
    countdown: String,
    ringColor: androidx.compose.ui.graphics.Color,
    trackColor: androidx.compose.ui.graphics.Color,
    labelColor: androidx.compose.ui.graphics.Color,
) {
    // تمريرٌ سلسٌ للحلقة عند تحديث النسبة كلّ ثانية.
    val animated by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = androidx.compose.animation.core.tween(700),
        label = "ringProgress",
    )
    Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val stroke = 11.dp.toPx()
            val inset = stroke / 2f
            val arc = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(
                color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arc,
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
            drawArc(
                color = ringColor, startAngle = -90f, sweepAngle = 360f * animated, useCenter = false,
                topLeft = topLeft, size = arc,
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("المتبقّي", style = MaterialTheme.typography.labelSmall, color = labelColor)
            Text(
                countdown.ifBlank { "—" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ringColor,
            )
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

/** يفتح ورقة مشاركة النظام بنصٍّ (آية/ذكر/حكمة). */
private fun launchShare(context: android.content.Context, text: String) {
    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, text)
    }
    runCatching { context.startActivity(android.content.Intent.createChooser(send, "مشاركة").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** بطاقة محتوى يوميّ (ذكر/حكمة) بنصٍّ بخطّ أميري وأزرار مشاركةٍ وتجديدٍ ونسخ. */
@Composable
private fun DailyCard(
    title: String,
    body: String,
    onRefresh: () -> Unit,
    refreshLabel: String,
    caption: String? = null,
) {
    val clipboard = LocalClipboardManager.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    // نصّ المشاركة: المتن + المصدر (إن وُجد) + توقيع التطبيق.
    val shareText = buildString {
        append(body.trim())
        if (!caption.isNullOrBlank()) append("\n").append(caption)
        append("\n\n— $title عبر تطبيق GT-SALAT")
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { launchShare(ctx, shareText) }, enabled = body.isNotBlank()) {
                        Icon(Icons.Filled.Share, contentDescription = "مشاركة")
                    }
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
