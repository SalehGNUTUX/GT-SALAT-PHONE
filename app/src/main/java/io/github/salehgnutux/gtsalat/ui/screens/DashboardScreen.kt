package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.clip
import io.github.salehgnutux.gtsalat.ui.tour.tourTarget
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
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(onOpenSettings: () -> Unit = {}, onOpenShortcut: (String) -> Unit = {}, vm: DashboardViewModel = hiltViewModel()) {
    // الشرح الحركيّ يمرّر الصفحة إلى العنصر الذي يشرحه.
    val tour = io.github.salehgnutux.gtsalat.ui.tour.LocalTour.current
    val shortcutRoute by vm.shortcutRoute.collectAsStateWithLifecycle()
    var pickShortcut by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val tick by vm.tick.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // رسالةُ نتيجة تحديث الموقع (نجاح/تعذّر) كـToast.
    val ctxDash = LocalContext.current
    val locateMsg by vm.locateMsg.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(locateMsg) {
        locateMsg?.let {
            android.widget.Toast.makeText(ctxDash, it, android.widget.Toast.LENGTH_LONG).show()
            vm.clearLocateMsg()
        }
    }

    // دخول الشاشة (أو العودة إليها من قسمٍ آخر) يبدأ من رأس الصفحة — لا من آخر موضعٍ محفوظ.
    androidx.compose.runtime.LaunchedEffect(Unit) { listState.scrollToItem(0) }
    // إعادة النقر على «الرئيسيّة» وهي ظاهرة → التمرير لرأس الصفحة.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        io.github.salehgnutux.gtsalat.ui.UiEvents.scrollHomeToTop.collect {
            listState.animateScrollToItem(0)
        }
    }

    if (pickShortcut) {
        ShortcutPickerSheet(shortcutRoute, onPick = vm::setShortcut, onDismiss = { pickShortcut = false })
    }
    if (tour != null && tour.active) {
        androidx.compose.runtime.LaunchedEffect(tour.stepIndex) {
            val item = io.github.salehgnutux.gtsalat.ui.tour.TourSteps[tour.stepIndex].listItem
            listState.animateScrollToItem(item)
        }
    }

    // الشريط العلويّ ثابتٌ خارج القائمة، ويمرّ المحتوى تحته. (عناصر القائمة وفهارسها كما هي: I_DHIKR…)
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                // فوق الصورة: خلفيّةٌ بلون البطاقات تحفظ وضوح الاسم والأزرار.
                val headerShape = RoundedCornerShape(20.dp)
                val headerBg = if (homeElementsNeedFill()) {
                    Modifier.frostedGlass(headerShape).background(homeCardColor(), headerShape)
                } else Modifier
                Row(Modifier.fillMaxWidth().then(headerBg), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onOpenSettings, modifier = Modifier.tourTarget(io.github.salehgnutux.gtsalat.ui.tour.TourKeys.SETTINGS)) {
                            Icon(Icons.Outlined.Settings, contentDescription = "الإعدادات", tint = MaterialTheme.colorScheme.primary)
                        }
                        // نقرةٌ تفتح القسم المختار، وضغطةٌ مطوّلةٌ تعرض بقيّة الأقسام.
                        val shortcut = AppFeatures.byRoute(shortcutRoute)
                        val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
                        Box(
                            Modifier
                                .tourTarget(io.github.salehgnutux.gtsalat.ui.tour.TourKeys.SHORTCUT)
                                .size(48.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .combinedClickable(
                                    onClickLabel = "فتح ${shortcut.label}",
                                    onLongClickLabel = "تغيير الاختصار",
                                    onLongClick = {
                                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        pickShortcut = true
                                    },
                                    onClick = { onOpenShortcut(shortcut.route) },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(shortcut.icon, contentDescription = shortcut.label, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text("GT-SALAT", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ClockFormatToggleButton()
                        Box(Modifier.tourTarget(io.github.salehgnutux.gtsalat.ui.tour.TourKeys.THEME)) { ThemeToggleButton() }
                    }
                }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 0 — صفٌّ علويٌّ (اختصار الإعدادات + تبديل السِمة) ثمّ البطاقة الرئيسيّة (بلا تغيير المؤشّرات)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdhanStopBanner()
                    NotificationPermissionBanner()
                    UpdateBanner()
                    val locating by vm.locating.collectAsStateWithLifecycle()
                    val locationGate = io.github.salehgnutux.gtsalat.ui.location.rememberLocationGate()
                    HeroCard(ui, tick, locating = locating, onRefreshLocation = { locationGate { vm.redetectLocation() } })
                    RamadanCard(ui, tick)
                }
            }

            // 1 — أزرار ذكر/حكمة/آية اليوم (تنزل للمحتوى أسفل الصفحة) — متّسقةٌ بسطرٍ واحد
            item {
                Row(Modifier.fillMaxWidth().tourTarget(io.github.salehgnutux.gtsalat.ui.tour.TourKeys.DAILY), Arrangement.spacedBy(10.dp)) {
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
                    modifier = Modifier.fillMaxWidth().then(
                        if (homeElementsNeedFill()) {
                            Modifier.frostedGlass(RoundedCornerShape(12.dp)).background(homeCardColor(), RoundedCornerShape(12.dp)).padding(8.dp)
                        } else Modifier,
                    ),
                )
            }
        }
    }
}

/** زرُّ تنقّلٍ يوميّ متّسق: أيقونةٌ فوق نصٍّ بسطرٍ واحد (فلا يلتفّ ولا يصير دائرة). */
@Composable
private fun DailyNavButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val glass = io.github.salehgnutux.gtsalat.ui.theme.LocalGlassTheme.current
    val shape = RoundedCornerShape(18.dp)
    FilledTonalButton(
        onClick = onClick,
        modifier = if (glass) modifier.frostedGlass(shape) else modifier,
        shape = if (glass) shape else androidx.compose.material3.ButtonDefaults.filledTonalShape,
        colors = if (glass) {
            androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        } else androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp, horizontal = 4.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(icon, contentDescription = null)
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
private fun HeroCard(ui: DashboardUi, tick: DashboardTick, locating: Boolean = false, onRefreshLocation: () -> Unit = {}) {
    val cs = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    val heroAlpha = homeHeroAlpha()
    val glass = io.github.salehgnutux.gtsalat.ui.theme.LocalGlassTheme.current
    val strong = glassStrongFill()
    val brush = androidx.compose.runtime.remember(cs.primaryContainer, cs.primary, dark, heroAlpha, glass, strong) {
        if (glass) androidx.compose.ui.graphics.SolidColor(strong) else Brush.verticalGradient(
            listOf(
                lerp(cs.primaryContainer, cs.primary, if (dark) 0.35f else 0.14f).copy(alpha = heroAlpha),
                cs.primaryContainer.copy(alpha = heroAlpha),
            ),
        )
    }
    val onHero = if (glass) cs.onSurface else cs.onPrimaryContainer
    val heroShape = RoundedCornerShape(if (glass) 24.dp else 22.dp)

    Card(
        Modifier.fillMaxWidth().frostedGlass(heroShape),
        shape = heroShape,
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (glass || heroAlpha < 1f) androidx.compose.ui.graphics.Color.Transparent else androidx.compose.material3.CardDefaults.cardColors().containerColor,
        ),
    ) {
        Column(
            Modifier.fillMaxWidth().background(brush).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // الموقع + الساعة الحيّة — الاسمُ الطويل يُختصَر بنقاطٍ ولا يضغط الساعةَ فتلتفّ.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).tourTarget(io.github.salehgnutux.gtsalat.ui.tour.TourKeys.LOCATION), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = onHero, modifier = Modifier.height(18.dp))
                    Text(
                        if (ui.city.isNotBlank()) ui.city else "موقعك",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onHero,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).padding(horizontal = 2.dp),
                    )
                    // زرّ سهمين دائريّين لتحديث الموقع يدويّاً دون الذهاب للإعدادات.
                    IconButton(onClick = onRefreshLocation, enabled = !locating, modifier = Modifier.size(30.dp)) {
                        if (locating) {
                            androidx.compose.material3.CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = onHero)
                        } else {
                            Icon(Icons.Filled.Sync, contentDescription = "تحديث الموقع", tint = onHero, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Text(
                    tick.clock,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = onHero,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            // التاريخان
            ui.hijri?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = onHero, fontWeight = FontWeight.Bold) }
            if (ui.gregorian.isNotBlank()) {
                Text(ui.gregorian, style = MaterialTheme.typography.bodySmall, color = onHero.copy(alpha = 0.8f))
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = onHero.copy(alpha = 0.25f))

            // الصلاة القادمة + حلقة تقدّمٍ حيّة تمتلئ باقتراب الوقت
            Row(Modifier.fillMaxWidth().tourTarget(io.github.salehgnutux.gtsalat.ui.tour.TourKeys.NEXT), Arrangement.SpaceBetween, Alignment.CenterVertically) {
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
                        color = if (glass) cs.tertiary else onHero,
                    )
                }
                CountdownRing(
                    progress = tick.progress,
                    countdown = tick.countdownText,
                    ringColor = if (glass) cs.primary else onHero,
                    trackColor = onHero.copy(alpha = 0.22f),
                    labelColor = onHero.copy(alpha = 0.85f),
                )
            }
        }
    }
}

/** بطاقةٌ تظهر ما دامت إشعارات التطبيق مُعطَّلة (بعد أوّل تثبيت غالباً)، بزرٍّ يفتح إعدادات الإشعارات. */
@Composable
private fun NotificationPermissionBanner() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var enabled by remember { mutableStateOf(true) }
    // نُعيد الفحص كلّما عادت الشاشة للمقدّمة (فتختفي البطاقة فور تفعيل المستخدم للإشعارات).
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                enabled = androidx.core.app.NotificationManagerCompat.from(ctx).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    if (enabled) return
    Card(
        Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("🔔 الإشعارات معطّلة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(
                "لن يصلك الأذان ولا التنبيهات ما لم تُفعّل إشعارات التطبيق. اضغط الزرّ ثمّ فعّل «السماح بالإشعارات».",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            androidx.compose.material3.Button(
                onClick = {
                    val i = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { ctx.startActivity(i) }.onFailure {
                        runCatching { ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${ctx.packageName}")).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }
                },
            ) { Text("فتح إعدادات الإشعارات") }
        }
    }
}

/** بطاقة رمضان (تظهر في الشهر فقط): عدٌّ تنازليٌّ للإفطار أو السحور/الإمساك + وقتاهما. */
@Composable
private fun RamadanCard(ui: DashboardUi, tick: DashboardTick) {
    if (!io.github.salehgnutux.gtsalat.domain.Ramadan.isRamadan()) return
    val today = ui.today ?: return
    val fajr = today.time(io.github.salehgnutux.gtsalat.domain.PrayerId.FAJR)?.epochMillis ?: return
    val maghrib = today.time(io.github.salehgnutux.gtsalat.domain.PrayerId.MAGHRIB)?.epochMillis ?: return
    // قراءة tick تُجبر إعادة التركيب كلّ ثانية (عدّادٌ حيّ).
    @Suppress("UNUSED_EXPRESSION") tick.clock
    val now = System.currentTimeMillis()
    val imsak = fajr - io.github.salehgnutux.gtsalat.domain.Ramadan.IMSAK_BEFORE_FAJR_MIN * 60_000L

    // تحديد الحدث القادم: قبل الإمساك → سحور، بين الفجر والمغرب → إفطار، بعد المغرب → سحور الغد.
    val (label, target, isIftar) = when {
        now < imsak -> Triple("السحور والإمساك", imsak, false)
        now < maghrib -> Triple("الإفطار", maghrib, true)
        else -> Triple("السحور والإمساك", (ui.tomorrowFajrMillis ?: (fajr + 86_400_000L)) - io.github.salehgnutux.gtsalat.domain.Ramadan.IMSAK_BEFORE_FAJR_MIN * 60_000L, false)
    }
    val remaining = (target - now).coerceAtLeast(0L)
    val day = io.github.salehgnutux.gtsalat.domain.Ramadan.dayOfRamadan()

    val ramadanAlpha = homeHeroAlpha()
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (ramadanAlpha < 1f) androidx.compose.ui.graphics.Color.Transparent else androidx.compose.material3.CardDefaults.cardColors().containerColor,
        ),
    ) {
        Column(
            Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = ramadanAlpha), MaterialTheme.colorScheme.secondaryContainer.copy(alpha = ramadanAlpha))))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val onC = MaterialTheme.colorScheme.onTertiaryContainer
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("🌙 رمضان", fontWeight = FontWeight.Bold, color = onC, style = MaterialTheme.typography.titleMedium)
                if (day in 1..30) Text("اليوم $day", color = onC, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("المتبقّي على ${if (isIftar) "الإفطار" else "الإمساك"}", color = onC.copy(alpha = 0.9f), style = MaterialTheme.typography.labelLarge)
                Text(Format.countdown(remaining), style = CountdownStyle, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = onC)
            }
            HorizontalDivider(color = onC.copy(alpha = 0.25f))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                RamadanTime("الإمساك", Format.clock(imsak), onC)
                RamadanTime("الفجر", Format.clock(fajr), onC)
                RamadanTime("الإفطار", Format.clock(maghrib), onC)
            }
        }
    }
}

@Composable
private fun RamadanTime(label: String, time: String, onColor: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = onColor.copy(alpha = 0.85f))
        Text(time, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = onColor)
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
    Card(Modifier.fillMaxWidth().tourTarget(io.github.salehgnutux.gtsalat.ui.tour.TourKeys.PRAYERS).frostedGlass(MaterialTheme.shapes.medium), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = homeCardColor())) {
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
    Card(Modifier.fillMaxWidth().frostedGlass(MaterialTheme.shapes.medium), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = homeCardColor())) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { io.github.salehgnutux.gtsalat.util.Share.send(ctx, body, caption) }, enabled = body.isNotBlank()) {
                        Icon(Icons.Filled.Share, contentDescription = "مشاركة")
                    }
                    TextButton(onClick = { clipboard.setText(AnnotatedString(io.github.salehgnutux.gtsalat.util.Share.decorate(body, caption))) }) {
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

/** بطاقةٌ في الرئيسيّة تظهر أثناء تشغيل صوت الأذان/التنبيه/ذكر ما بعد الصلاة، بزرّ إيقافٍ فوريّ. */
@Composable
private fun AdhanStopBanner() {
    val active by io.github.salehgnutux.gtsalat.audio.AdhanPlayback.active.collectAsStateWithLifecycle()
    val kind by io.github.salehgnutux.gtsalat.audio.AdhanPlayback.kind.collectAsStateWithLifecycle()
    if (!active) return
    val ctx = LocalContext.current
    // أيقونةٌ ورسالةٌ حسب نوع الصوت الجاري.
    val (icon, label) = when (kind) {
        "prenotify" -> Icons.Filled.NotificationsActive to "يُشغَّل صوت تنبيه الاقتراب"
        "dhikr" -> Icons.Filled.Favorite to "يُشغَّل صوت الذكر الآن"
        else -> Icons.Filled.VolumeUp to "يُشغَّل صوت الأذان الآن"
    }
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Text(
                label,
                Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            FilledTonalButton(onClick = { io.github.salehgnutux.gtsalat.audio.AdhanService.stop(ctx) }) {
                Icon(Icons.Filled.Stop, null, Modifier.size(18.dp))
                Text("  إيقاف")
            }
        }
    }
}
