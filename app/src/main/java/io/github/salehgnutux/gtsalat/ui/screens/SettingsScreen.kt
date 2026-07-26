package io.github.salehgnutux.gtsalat.ui.screens

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.salehgnutux.gtsalat.BuildConfig
import io.github.salehgnutux.gtsalat.data.settings.AdhanAlertMode
import io.github.salehgnutux.gtsalat.data.settings.AdhanType
import io.github.salehgnutux.gtsalat.data.settings.ThemeMode
import io.github.salehgnutux.gtsalat.domain.AsrMadhab
import io.github.salehgnutux.gtsalat.domain.CalculationMethods
import io.github.salehgnutux.gtsalat.domain.CalendarKind
import io.github.salehgnutux.gtsalat.domain.MonthScheme

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val months by vm.cachedMonths.collectAsStateWithLifecycle()
    val previewing by vm.previewing.collectAsStateWithLifecycle()
    val settings = s ?: return
    val context = LocalContext.current

    // منتقي ملفّ صوتيّ لاستيراد أذانٍ مخصّص، مع تثبيت إذن القراءة الدائم.
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val name = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0 && c.moveToFirst()) c.getString(i) else null
            } ?: "أذان مخصّص"
            vm.setCustomAdhan(uri.toString(), name)
        }
    }

    // أكورديون: عنوان القسم المفتوح حاليّاً (null = الكلّ مطويّ). فتح قسمٍ يطوي غيره.
    var openSection by remember { mutableStateOf<String?>("الموقع وطريقة الحساب") }
    fun toggle(title: String) { openSection = if (openSection == title) null else title }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard("الموقع وطريقة الحساب", openSection == "الموقع وطريقة الحساب", { toggle("الموقع وطريقة الحساب") }) {
            InfoRow("الموقع الحاليّ", listOf(settings.city, settings.country).filter { it.isNotBlank() }.joinToString("، ").ifBlank { "غير محدّد" })
            ClickRow("إعادة اكتشاف الموقع") { vm.redetectLocation() }
            HorizontalDivider()
            MethodDropdown(settings.methodId) { vm.setMethod(it) }
            LabeledRow("مذهب حساب العصر") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(settings.madhab == AsrMadhab.SHAFI, { vm.setMadhab(AsrMadhab.SHAFI) }, { Text("الجمهور") })
                    FilterChip(settings.madhab == AsrMadhab.HANAFI, { vm.setMadhab(AsrMadhab.HANAFI) }, { Text("الحنفيّ") })
                }
            }
            SwitchRow("تحديث المواقيت عبر الإنترنت (AlAdhan)", settings.useApiTimetables) { vm.setUseApi(it) }
            InfoRow("أشهر مخزَّنة محليّاً", "$months")
        }

        SectionCard("الأذان والتنبيهات", openSection == "الأذان والتنبيهات", { toggle("الأذان والتنبيهات") }) {
            SwitchRow("تنبيه دخول وقت الصلاة", settings.enableSalatNotify) { vm.setEnableSalat(it) }
            SwitchRow("تشغيل صوت الأذان", settings.enableAdhanSound) { vm.setEnableAdhan(it) }
            LabeledRow("نوع الأذان") {
                AdhanTypeRow(
                    "كامل", settings.adhanType == AdhanType.FULL, previewing == AdhanType.FULL,
                    onSelect = { vm.setAdhanType(AdhanType.FULL) },
                    onPreview = { vm.previewAdhan(AdhanType.FULL) },
                )
                AdhanTypeRow(
                    "قصير", settings.adhanType == AdhanType.SHORT, previewing == AdhanType.SHORT,
                    onSelect = { vm.setAdhanType(AdhanType.SHORT) },
                    onPreview = { vm.previewAdhan(AdhanType.SHORT) },
                )
                val hasCustom = settings.customAdhanUri != null
                AdhanTypeRow(
                    "مخصّص", settings.adhanType == AdhanType.CUSTOM, previewing == AdhanType.CUSTOM,
                    onSelect = { if (hasCustom) vm.setAdhanType(AdhanType.CUSTOM) else importLauncher.launch(arrayOf("audio/*")) },
                    onPreview = { vm.previewAdhan(AdhanType.CUSTOM) },
                    previewEnabled = hasCustom,
                    trailing = if (hasCustom) settings.customAdhanName else null,
                )
                FilledTonalButton(
                    onClick = { importLauncher.launch(arrayOf("audio/*")) },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                ) {
                    Text(if (settings.customAdhanUri != null) "تغيير الأذان المخصّص" else "استيراد أذان مخصّص")
                }
            }
            LabeledRow("نمط تنبيه دخول الوقت") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(settings.adhanAlertMode == AdhanAlertMode.FULL, { vm.setAdhanAlertMode(AdhanAlertMode.FULL) }, { Text("أذان كامل") })
                    FilterChip(settings.adhanAlertMode == AdhanAlertMode.TONE, { vm.setAdhanAlertMode(AdhanAlertMode.TONE) }, { Text("رنّة تنبيه") })
                }
            }
            SwitchRow("دعاء بعد الأذان", settings.enableDuaAfterAdhan) { vm.setEnableDua(it) }
            SwitchRow("صوت أذكار بعد الصلاة (بعد ${settings.postDhikrMinutes} دقيقة)", settings.enablePostDhikr) { vm.setEnablePostDhikr(it) }
            SwitchRow("تنبيه الاقتراب قبل الصلاة", settings.enablePreNotify) { vm.setEnablePreNotify(it) }
            if (settings.enablePreNotify) {
                SwitchRow("صوت تنبيه الاقتراب", settings.enablePreNotifySound) { vm.setEnablePreNotifySound(it) }
                MinutesSlider("قبل الصلاة بـ", settings.preNotifyMinutes, 1, 60) { vm.setPreNotify(it) }
            }
            SwitchRow("وضع عدم الإزعاج", settings.doNotDisturb) { vm.setDnd(it) }
            SwitchRow("إشعارٌ دائمٌ بالصلاة القادمة", settings.persistentNotification) { vm.setPersistentNotification(it) }
            SwitchRow("الكاتم التلقائيّ أثناء الصلاة", settings.autoSilence) { vm.setAutoSilence(it) }
            if (settings.autoSilence) {
                SilenceControls(settings.silenceMinutes) { vm.setSilenceMinutes(it) }
            }
        }

        ReliabilityCard(openSection == "موثوقيّة التنبيهات") { toggle("موثوقيّة التنبيهات") }

        SectionCard("التقويم والتواريخ", openSection == "التقويم والتواريخ", { toggle("التقويم والتواريخ") }) {
            LabeledRow("تقويم عرض المواقيت") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(settings.timetableCalendar == CalendarKind.HIJRI, { vm.setTimetableCalendar(CalendarKind.HIJRI) }, { Text("هجريّ") })
                    FilterChip(settings.timetableCalendar == CalendarKind.GREGORIAN, { vm.setTimetableCalendar(CalendarKind.GREGORIAN) }, { Text("ميلاديّ") })
                }
            }
            LabeledRow("أسماء الأشهر الميلاديّة") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(settings.monthScheme == MonthScheme.AUTO, { vm.setMonthScheme(MonthScheme.AUTO) }, { Text("تلقائيّ") })
                        FilterChip(settings.monthScheme == MonthScheme.STANDARD, { vm.setMonthScheme(MonthScheme.STANDARD) }, { Text("قياسيّ") })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(settings.monthScheme == MonthScheme.MAGHREB, { vm.setMonthScheme(MonthScheme.MAGHREB) }, { Text("مغاربيّ (يوليوز/غشت)") })
                        FilterChip(settings.monthScheme == MonthScheme.LEVANT, { vm.setMonthScheme(MonthScheme.LEVANT) }, { Text("شاميّ (تمّوز/آب)") })
                    }
                }
            }
        }

        SectionCard("المظهر", openSection == "المظهر", { toggle("المظهر") }) {
            LabeledRow("السِمة") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(settings.themeMode == ThemeMode.SYSTEM, { vm.setTheme(ThemeMode.SYSTEM) }, { Text("النظام") })
                    FilterChip(settings.themeMode == ThemeMode.LIGHT, { vm.setTheme(ThemeMode.LIGHT) }, { Text("فاتح") })
                    FilterChip(settings.themeMode == ThemeMode.DARK, { vm.setTheme(ThemeMode.DARK) }, { Text("داكن") })
                }
            }
            SwitchRow("الألوان الديناميكيّة (Material You)", settings.dynamicColor) { vm.setDynamicColor(it) }
            if (!settings.dynamicColor) {
                ColorPicker(settings.seedColor) { vm.setSeedColor(it) }
            } else {
                Text(
                    "عطّل الألوان الديناميكيّة لتخصيص لونك.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }

        SectionCard("حول", openSection == "حول", { toggle("حول") }) {
            InfoRow("النسخة", "GT-SALAT ${BuildConfig.VERSION_NAME}")
            InfoRow("الإصدار", if (BuildConfig.USES_GMS) "كاملة (خدمات Google)" else "حرّة (بلا Google)")
        }
    }
}

/** قسم إعداداتٍ قابلٌ للطيّ ضمن أكورديون: فتح قسمٍ يطوي الباقي تلقائيّاً. */
@Composable
private fun SectionCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { onToggle() },
                Arrangement.SpaceBetween,
                Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "طيّ" else "فتح",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            androidx.compose.animation.AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
            }
        }
    }
}

/** بطاقة موثوقيّة التنبيهات: الإنذار الدقيق وإعفاء البطاريّة — أهمّ ما يضمن وصول الأذان. */
@Composable
private fun ReliabilityCard(expanded: Boolean, onToggle: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // مفتاحٌ يتغيّر عند العودة من إعدادات النظام لإعادة قراءة الحالة.
    var refreshKey by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val exactOk = remember(refreshKey) { canScheduleExact(context) }
    val batteryOk = remember(refreshKey) { isBatteryUnrestricted(context) }
    if (exactOk && batteryOk) return // كلّ شيءٍ على ما يرام، لا نُزعِج المستخدم

    SectionCard("موثوقيّة التنبيهات", expanded, onToggle) {
        Text(
            "لضمان وصول الأذان في وقته حتى والتطبيق مغلق، فعّل ما يلي:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        if (!exactOk) {
            ReliabilityRow(
                title = "الإنذارات الدقيقة",
                desc = "يسمح بإطلاق الأذان في لحظته بالضبط.",
                onFix = { openExactAlarmSettings(context) },
            )
        }
        if (!batteryOk) {
            ReliabilityRow(
                title = "إعفاء من تحسين البطاريّة",
                desc = "يمنع النظام من تعطيل التطبيق في الخلفيّة (مهمّ على أجهزة Xiaomi وHuawei وغيرها).",
                onFix = { openBatteryExemption(context) },
            )
        }
    }
}

@Composable
private fun ReliabilityRow(title: String, desc: String, onFix: () -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        FilledTonalButton(onClick = onFix) { Text("تفعيل") }
    }
}

private fun canScheduleExact(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return am.canScheduleExactAlarms()
}

private fun isBatteryUnrestricted(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }.onFailure { openAppDetails(context) }
}

private fun openBatteryExemption(context: Context) {
    runCatching {
        @Suppress("BatteryLife")
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }.onFailure {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure { openAppDetails(context) }
    }
}

private fun openAppDetails(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}

/** أداة تخصيص لون السِمة: سواترُ جاهزة + منزلق تدرّجٍ حرّ (Hue). يؤثّر في الوضعين والتدرّج. */
@Composable
private fun ColorPicker(seed: Int, onPick: (Int) -> Unit) {
    val presets = listOf(
        0 to "افتراضيّ",
        0xFF1B6B4C.toInt() to "أخضر",
        0xFF00796B.toInt() to "فيروزيّ",
        0xFF1565C0.toInt() to "أزرق",
        0xFF6A1B9A.toInt() to "أرجوانيّ",
        0xFFC9A227.toInt() to "ذهبيّ",
        0xFFB5651D.toInt() to "بنّيّ",
        0xFFAD1457.toInt() to "توتيّ",
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("لون السِمة والتدرّج", style = MaterialTheme.typography.bodyLarge)
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(presets) { (argb, _) ->
                val selected = seed == argb
                val swatch = if (argb == 0) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color(argb)
                androidx.compose.foundation.layout.Box(
                    Modifier.size(38.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(swatch)
                        .then(if (argb == 0) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, androidx.compose.foundation.shape.CircleShape) else Modifier)
                        .clickable { onPick(argb) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
                    else if (argb == 0) Text("×", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        // منزلق تدرّجٍ حرّ: يولّد لوناً من زاوية الطيف.
        var hue by remember(seed) {
            val hsv = FloatArray(3)
            if (seed != 0) android.graphics.Color.colorToHSV(seed, hsv) else hsv[0] = 150f
            mutableStateOf(hsv[0])
        }
        Text("تدرّجٌ حرّ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Slider(
            value = hue,
            onValueChange = { hue = it },
            valueRange = 0f..360f,
            onValueChangeFinished = { onPick(androidx.compose.ui.graphics.Color.hsv(hue, 0.6f, 0.55f).toArgb()) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AdhanTypeRow(
    label: String,
    selected: Boolean,
    previewing: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
    previewEnabled: Boolean = true,
    trailing: String? = null,
) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            FilterChip(selected, { onSelect() }, { Text(label) })
            if (!trailing.isNullOrBlank()) {
                Text(
                    trailing,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                )
            }
        }
        IconButton(onClick = onPreview, enabled = previewEnabled) {
            Icon(
                if (previewing) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = if (previewing) "إيقاف التجربة" else "تجربة",
                tint = if (previewEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun MinutesSlider(prefix: String, minutes: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    var v by remember(minutes) { mutableStateOf(minutes.toFloat()) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // القيمة الحيّة تظهر أثناء السحب لا بعد تركه فقط.
        Text("$prefix ${v.toInt()} دقيقة", style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = v,
            onValueChange = { v = it },
            valueRange = min.toFloat()..max.toFloat(),
            onValueChangeFinished = { onChange(v.toInt()) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** ضوابط الكاتم: تنبيهُ منح إذن «عدم الإزعاج» إن لزم، ومدّة الكتم. */
@Composable
private fun SilenceControls(minutes: Int, onChange: (Int) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshKey by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    val hasAccess = remember(refreshKey) { hasPolicyAccess(context) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!hasAccess) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(
                    "يلزم منح إذن «عدم الإزعاج» ليعمل الكتم.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                FilledTonalButton(onClick = { openPolicyAccess(context) }) { Text("منح") }
            }
        }
        MinutesSlider("يُكتم لمدّة", minutes, 5, 60, onChange)
    }
}

private fun hasPolicyAccess(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    return nm.isNotificationPolicyAccessGranted
}

private fun openPolicyAccess(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.onFailure { openAppDetails(context) }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked, onChange)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun ClickRow(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
    )
}

@Composable
private fun LabeledRow(label: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        content()
    }
}

@Composable
private fun MethodDropdown(methodId: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val info = CalculationMethods.infoOf(methodId)
    Column {
        Text("طريقة الحساب", style = MaterialTheme.typography.bodyLarge)
        Text(
            info.nameAr,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().clickable { expanded = true }.padding(vertical = 6.dp),
        )
        DropdownMenu(expanded, { expanded = false }) {
            CalculationMethods.ALL.forEach { m ->
                DropdownMenuItem(
                    text = { Text(m.nameAr) },
                    onClick = { onSelect(m.id); expanded = false },
                )
            }
        }
    }
}
