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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import android.widget.Toast
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
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
import io.github.salehgnutux.gtsalat.data.settings.AppSettings
import io.github.salehgnutux.gtsalat.data.settings.ThemeMode
import io.github.salehgnutux.gtsalat.domain.AsrMadhab
import io.github.salehgnutux.gtsalat.domain.CalculationMethods
import io.github.salehgnutux.gtsalat.domain.CalendarKind
import io.github.salehgnutux.gtsalat.domain.MonthScheme

@Composable
fun SettingsScreen(onShowTour: () -> Unit = {}, vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val months by vm.cachedMonths.collectAsStateWithLifecycle()
    val previewing by vm.previewing.collectAsStateWithLifecycle()
    val previewKey by vm.previewKey.collectAsStateWithLifecycle()
    val settings = s ?: return
    val context = LocalContext.current
    val shareScope = rememberCoroutineScope()
    var showWhatsNew by remember { mutableStateOf(false) }

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

    // ----- النسخ الاحتياطيّ الانتقائيّ (تصدير/مشاركة/استيراد) -----
    var backupSizes by remember { mutableStateOf<io.github.salehgnutux.gtsalat.data.BackupSizes?>(null) }
    var exportMode by remember { mutableStateOf<String?>(null) }      // "export" أو "share" أو null
    var pendingOpts by remember { mutableStateOf(io.github.salehgnutux.gtsalat.data.BackupOptions()) }
    var importUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var importContents by remember { mutableStateOf<io.github.salehgnutux.gtsalat.data.BackupContents?>(null) }
    var busy by remember { mutableStateOf<String?>(null) }   // نصّ رسالة الانتظار أو null

    fun toast(msg: String) = android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()

    // إنشاء ملفّ الحزمة (SAF) بعد اختيار المحتوى.
    val exportZipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri != null) {
            busy = "جارٍ تجهيز الحزمة…"
            vm.exportBundle(uri, pendingOpts) { ok -> busy = null; toast(if (ok) "تمّ حفظ النسخة الاحتياطيّة" else "تعذّر التصدير") }
        }
    }
    // اختيار حزمةٍ لاستيرادها ← فحصٌ ثمّ اختيار ما يُستعاد.
    val importZipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            busy = "جارٍ فحص الحزمة…"
            vm.inspectBackup(uri) { c ->
                busy = null
                if (!c.anything) toast("الحزمة فارغةٌ أو غير صالحة") else { importUri = uri; importContents = c }
            }
        }
    }

    // أكورديون: القسم المفتوح مُخزَّنٌ في الإعدادات (يُتذكَّر عبر الجلسات؛ "" = الكلّ مطويّ).
    val openSection: String? = settings.settingsOpenSection.ifBlank { null }
    fun toggle(title: String) { vm.setSettingsSection(if (openSection == title) "" else title) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard("الموقع وطريقة الحساب", openSection == "الموقع وطريقة الحساب", { toggle("الموقع وطريقة الحساب") }) {
            InfoRow("المنطقة الزمنيّة للهاتف", "\u2066" + io.github.salehgnutux.gtsalat.domain.TimeZoneCheck.label(java.time.ZoneId.systemDefault().rules.getOffset(java.time.Instant.now())) + "\u2069")
            InfoRow("الموقع الحاليّ", listOf(settings.city, settings.country).filter { it.isNotBlank() }.joinToString("، ").ifBlank { "غير محدّد" })
            val locationGate = io.github.salehgnutux.gtsalat.ui.location.rememberLocationGate()
            ClickRow("إعادة اكتشاف الموقع") { locationGate { vm.redetectLocation() } }
            val locStatus by vm.locationStatus.collectAsStateWithLifecycle()
            locStatus?.let { st ->
                if (st.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        androidx.compose.material3.CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("يجري تحديد موقعك… قد يستغرق لحظات.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text(st, style = MaterialTheme.typography.bodySmall, color = if (st.startsWith("✓")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }
            }
            // تحديدٌ يدويٌّ للموقع — خيارٌ إضافيٌّ عند تعذّر الكشف التلقائيّ أو لتغيير الموقع دون إنترنت.
            val places by vm.places.collectAsStateWithLifecycle()
            var showPlaces by remember { mutableStateOf(false) }
            var showManual by remember { mutableStateOf(false) }
            var latText by remember { mutableStateOf("") }
            var lonText by remember { mutableStateOf("") }
            FilledTonalButton(onClick = { showPlaces = true }, enabled = places.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text("🌍 اختر البلد والمدينة يدويّاً (دون إنترنت)")
            }
            androidx.compose.material3.TextButton(onClick = { showManual = !showManual }) {
                Text(if (showManual) "إخفاء الإدخال اليدويّ للإحداثيّات" else "أو أدخِل الإحداثيّات يدويّاً")
            }
            if (showManual) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = latText, onValueChange = { latText = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                        label = { Text("خط العرض") }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = lonText, onValueChange = { lonText = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                        label = { Text("خط الطول") }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    )
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        val la = latText.toDoubleOrNull(); val lo = lonText.toDoubleOrNull()
                        if (la != null && lo != null && la in -90.0..90.0 && lo in -180.0..180.0) { vm.setManualLocation(la, lo); showManual = false }
                        else android.widget.Toast.makeText(context, "أدخِل إحداثيّاتٍ صحيحة (العرض −90..90، الطول −180..180)", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    enabled = latText.isNotBlank() && lonText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("حفظ الإحداثيّات") }
            }
            if (showPlaces) {
                PlacePickerDialog(places = places, onDismiss = { showPlaces = false }) { p -> vm.pickPlace(p); showPlaces = false }
            }
            SwitchRow("تحديث الموقع تلقائيّاً", settings.autoUpdateLocation) { vm.setAutoUpdateLocation(it) }
            Text(
                "يُحدَّث موقعك عند فتح التطبيق ودوريّاً من GPS أو الشبكة إن كانت خدمة الموقع مفعَّلة، فتتبعك المواقيت في تنقّلك. واختيار مدينةٍ يدويّاً يطفئه.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            SwitchRow("ترجمة أسماء المواقع الأجنبيّة للعربيّة", settings.translateLocationNames) { vm.setTranslateLocationNames(it) }
            Text(
                "افتراضيّاً يظهر اسم الموقع بلغته (مثلاً «Jerez»). عند التفعيل يُترجَم للعربيّة إن توفّرت الترجمة (مثلاً «شريش») — يسري عند تحديث الموقع التالي.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            // سجلّ آخر المواقع المكتشَفة — للعودة إليها إن تعذّر الكشف التلقائيّ.
            if (settings.locationHistory.isNotEmpty()) {
                Text("مواقعُ سابقة (اضغط للعودة إليها):", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                settings.locationHistory.forEach { pl ->
                    val name = listOf(pl.city, pl.country).filter { it.isNotBlank() }.joinToString("، ")
                        .ifBlank { String.format(java.util.Locale.US, "%.3f، %.3f", pl.lat, pl.lon) }
                    ClickRow(name) { vm.restoreLocation(pl) }
                }
            }
            HorizontalDivider()
            MethodDropdown(settings.methodId) { vm.setMethod(it) }
            LabeledRow("مذهب حساب العصر") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(settings.madhab == AsrMadhab.SHAFI, { vm.setMadhab(AsrMadhab.SHAFI) }, { Text("الجمهور") })
                    FilterChip(settings.madhab == AsrMadhab.HANAFI, { vm.setMadhab(AsrMadhab.HANAFI) }, { Text("الحنفيّ") })
                }
            }
            SwitchRow("تحديث المواقيت عبر الإنترنت (AlAdhan)", settings.useApiTimetables) { vm.setUseApi(it) }
            // الأشهر المخزّنة تُنظَّف تلقائيّاً عند التخزين المسبق، وهنا حذفٌ يدويٌّ للمنصرم.
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("أشهر مخزَّنة محليّاً", style = MaterialTheme.typography.bodyLarge)
                    Text("$months شهراً — تُنظَّف المنصرمة تلقائيّاً", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                FilledTonalButton(onClick = { vm.pruneOldMonths() }) { Text("حذف المنصرم") }
            }
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
                AdhanTypeRow(
                    "بالصيغة المغربيّة — كامل", settings.adhanType == AdhanType.MAGHRIBI_FULL, previewing == AdhanType.MAGHRIBI_FULL,
                    onSelect = { vm.setAdhanType(AdhanType.MAGHRIBI_FULL) },
                    onPreview = { vm.previewAdhan(AdhanType.MAGHRIBI_FULL) },
                )
                AdhanTypeRow(
                    "بالصيغة المغربيّة — قصير", settings.adhanType == AdhanType.MAGHRIBI_SHORT, previewing == AdhanType.MAGHRIBI_SHORT,
                    onSelect = { vm.setAdhanType(AdhanType.MAGHRIBI_SHORT) },
                    onPreview = { vm.previewAdhan(AdhanType.MAGHRIBI_SHORT) },
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
            MinutesSlider("مستوى صوت الأذان:", settings.adhanVolume, 0, 100, suffix = "٪") { vm.setAdhanVolume(it) }
            Text(
                "يتحكّم هذا المستوى بصوت الأذان ودعاء ما بعده، وتنبيه الاقتراب، وأذكار ما بعد الصلاة.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            // زرّا اختبار الصوت بجانب المنزلق مباشرةً — يشغّلان بالمستوى المضبوط للتحقّق منه.
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { vm.testFullScreen(isDhikr = false) }, modifier = Modifier.weight(1f)) { Text("اختبار الأذان") }
                FilledTonalButton(onClick = { vm.testFullScreen(isDhikr = true) }, modifier = Modifier.weight(1f)) { Text("اختبار الأذكار") }
            }
            SwitchRow("تنبيهٌ مخصّصٌ لكلّ صلاة", settings.perPrayerAlerts) { vm.setPerPrayerAlerts(it) }
            if (settings.perPrayerAlerts) {
                io.github.salehgnutux.gtsalat.data.settings.AppSettings.ALERT_PRAYERS.forEachIndexed { i, pid ->
                    LabeledRow(pid.arabic) {
                        AlertModeChips(settings.prayerAlerts.getOrElse(i) { AdhanAlertMode.FULL }) { vm.setPrayerAlert(i, it) }
                    }
                }
            } else {
                LabeledRow("نمط تنبيه دخول الوقت") {
                    AlertModeChips(settings.adhanAlertMode) { vm.setAdhanAlertMode(it) }
                }
            }
            PreviewRow("معاينة رنّة التنبيه", previewKey == "tone") { vm.previewTone() }
            SwitchRowPreview("دعاء بعد الأذان", settings.enableDuaAfterAdhan, previewKey == "dua", { vm.previewDua() }) { vm.setEnableDua(it) }
            SwitchRowPreview("صوت أذكار بعد الصلاة (بعد ${settings.postDhikrMinutes} دقيقة)", settings.enablePostDhikr, previewKey == "dhikr", { vm.previewPostDhikr() }) { vm.setEnablePostDhikr(it) }
            SwitchRow("تنبيه الاقتراب قبل الصلاة", settings.enablePreNotify) { vm.setEnablePreNotify(it) }
            if (settings.enablePreNotify) {
                SwitchRowPreview("صوت تنبيه الاقتراب", settings.enablePreNotifySound, previewKey == "prenotify", { vm.previewPreNotifySound() }) { vm.setEnablePreNotifySound(it) }
                MinutesSlider("قبل الصلاة بـ", settings.preNotifyMinutes, 1, 60) { vm.setPreNotify(it) }
            }
            SwitchRow("وضع عدم الإزعاج", settings.doNotDisturb) { vm.setDnd(it) }
            SwitchRow("إشعارٌ دائمٌ بالصلاة القادمة", settings.persistentNotification) { vm.setPersistentNotification(it) }
            SwitchRow("الكاتم التلقائيّ أثناء الصلاة", settings.autoSilence) { vm.setAutoSilence(it) }
            if (settings.autoSilence) {
                SilenceControls(settings.silenceMinutes) { vm.setSilenceMinutes(it) }
            }
            SwitchRow("نافذة أذانٍ ملء الشاشة فوق القفل", settings.fullScreenAdhan) { vm.setFullScreenAdhan(it) }
            if (settings.fullScreenAdhan) {
                SwitchRow("إبقاء النافذة حتى أغلقها يدويّاً (بعد انتهاء الصوت)", settings.keepAdhanScreen) { vm.setKeepAdhanScreen(it) }
            }
        }

        SectionCard("التذكيرات اليوميّة", openSection == "التذكيرات اليوميّة", { toggle("التذكيرات اليوميّة") }) {
            SwitchRow("بطاقة آية اليوم في الرئيسيّة", settings.enableDailyAyah) { vm.setEnableDailyAyah(it) }
            SwitchRow("تذكير وِرد التلاوة", settings.enableRecitationReminder) { vm.setEnableRecitationReminder(it) }
            SwitchRow("بطاقة وِرد التلاوة في القرآن (هدفٌ وسلسلة أيّام)", settings.enableWird) { vm.setEnableWird(it) }
            SwitchRow("تذكير الأيّام البيض (13/14/15 هجريّ)", settings.enableWhiteDaysReminder) { vm.setEnableWhiteDaysReminder(it) }
            SwitchRow("تذكير السُّنن (اثنين/خميس، جمعة، عاشوراء، عرفة…)", settings.enableSunnahReminder) { vm.setEnableSunnahReminder(it) }
            MinutesSlider("ساعة التذكير:", settings.reminderHour, 0, 23, suffix = "") { vm.setReminderHour(it) }
            HorizontalDivider()
            SwitchRow("تذكير أذكار الصباح", settings.enableMorningAdhkar) { vm.setEnableMorningAdhkar(it) }
            if (settings.enableMorningAdhkar) MinutesSlider("ساعة تذكير الصباح:", settings.morningAdhkarHour, 0, 23, suffix = "") { vm.setMorningAdhkarHour(it) }
            SwitchRow("تذكير أذكار المساء", settings.enableEveningAdhkar) { vm.setEnableEveningAdhkar(it) }
            if (settings.enableEveningAdhkar) MinutesSlider("ساعة تذكير المساء:", settings.eveningAdhkarHour, 0, 23, suffix = "") { vm.setEveningAdhkarHour(it) }
        }

        ReliabilityCard(openSection == "موثوقيّة التنبيهات", { toggle("موثوقيّة التنبيهات") }, { vm.testNotification() })

        SectionCard("التقويم والتواريخ", openSection == "التقويم والتواريخ", { toggle("التقويم والتواريخ") }) {
            LabeledRow("نظام عرض الوقت") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(settings.clock24h, { vm.setClock24h(true) }, { Text("24 ساعة") })
                    FilterChip(!settings.clock24h, { vm.setClock24h(false) }, { Text("12 ساعة (ص/م)") })
                }
            }
            LabeledRow("تقويم عرض المواقيت") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(settings.timetableCalendar == CalendarKind.HIJRI, { vm.setTimetableCalendar(CalendarKind.HIJRI) }, { Text("هجريّ") })
                    FilterChip(settings.timetableCalendar == CalendarKind.GREGORIAN, { vm.setTimetableCalendar(CalendarKind.GREGORIAN) }, { Text("ميلاديّ") })
                }
            }
            // تعديل التاريخ الهجريّ بالأيّام (لتصحيح فرق التقويم حسب المنطقة/الرؤية).
            LabeledRow("تعديل التاريخ الهجريّ") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = { vm.setHijriOffset(settings.hijriOffset - 1) }, enabled = settings.hijriOffset > -3) { Text("−", fontWeight = FontWeight.Bold) }
                    val off = settings.hijriOffset
                    Text(
                        when { off == 0 -> "بدون" ; off > 0 -> "+$off يوم" ; else -> "$off يوم" },
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    FilledTonalButton(onClick = { vm.setHijriOffset(settings.hijriOffset + 1) }, enabled = settings.hijriOffset < 3) { Text("+", fontWeight = FontWeight.Bold) }
                }
            }
            if (settings.hijriOffset != 0) {
                Text(
                    "التاريخ الهجريّ محسوبٌ محلّيّاً (أمّ القرى) بالإزاحة. أعِده إلى «بدون» للاعتماد على تاريخ الإنترنت.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
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

        val customBgAvailable by vm.customBackgroundAvailable.collectAsStateWithLifecycle()
        val importingBg by vm.importingBackground.collectAsStateWithLifecycle()
        val bgMessage by vm.backgroundMessage.collectAsStateWithLifecycle()
        // منتقي الصور في النظام: لا يحتاج إذناً، ويعطي الصورة المختارة وحدها.
        val pickBackground = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) vm.importCustomBackground(uri)
        }
        SectionCard("خلفيّة الرئيسيّة", openSection == "خلفيّة الرئيسيّة", { toggle("خلفيّة الرئيسيّة") }) {
            HomeBackgroundSettings(
                enabled = settings.homeBackgroundEnabled,
                selectedId = settings.homeBackgroundId,
                level = settings.homeBackgroundLevel,
                onEnabled = vm::setHomeBackgroundEnabled,
                onSelect = vm::selectHomeBackground,
                onLevel = vm::setHomeBackgroundLevel,
                blur = settings.glassBlur,
                customAvailable = customBgAvailable,
                importing = importingBg,
                onPickCustom = {
                    pickBackground.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                blurSlider = {
                    MinutesSlider("شدّة التغبيش:", settings.glassBlur, 0, 100, suffix = "من 100") { vm.setGlassBlur(it) }
                },
                switchRow = { label, checked, onChange -> SwitchRow(label, checked, onChange) },
            )
            bgMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
        }

        SectionCard("المظهر", openSection == "المظهر", { toggle("المظهر") }) {
            SwitchRow("السمة الزجاجيّة", settings.glassTheme) { vm.setGlassTheme(it) }
            // اختصار أعلى الرئيسيّة (يُغيَّر أيضاً بضغطةٍ مطوّلةٍ على أيقونته هناك).
            var pickShortcut by remember { mutableStateOf(false) }
            val shortcut = AppFeatures.byRoute(settings.homeShortcutRoute)
            LabeledRow("اختصار أعلى الرئيسيّة") {
                FilledTonalButton(onClick = { pickShortcut = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(shortcut.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(shortcut.label, modifier = Modifier.padding(start = 8.dp))
                }
            }
            if (pickShortcut) {
                ShortcutPickerSheet(settings.homeShortcutRoute, onPick = vm::setHomeShortcut, onDismiss = { pickShortcut = false })
            }
            LabeledRow("السِمة") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(settings.themeMode == ThemeMode.SYSTEM, { vm.setTheme(ThemeMode.SYSTEM) }, { Text("النظام") })
                    FilterChip(settings.themeMode == ThemeMode.LIGHT, { vm.setTheme(ThemeMode.LIGHT) }, { Text("فاتح") })
                    FilterChip(settings.themeMode == ThemeMode.DARK, { vm.setTheme(ThemeMode.DARK) }, { Text("داكن") })
                }
            }
            SwitchRow("الألوان الديناميكيّة (Material You)", settings.dynamicColor) { vm.setDynamicColor(it) }
            if (settings.glassTheme && settings.dynamicColor) {
                Text(
                    "السمة الزجاجيّة تستعمل ألوانها الخاصّة؛ أطفئها لتطبيق الألوان الديناميكيّة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            if (!settings.dynamicColor) {
                ColorTool(settings, vm)
            } else {
                Text(
                    "عطّل الألوان الديناميكيّة لتخصيص ألوانك.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            LabeledRow("نمط ودجت الساعة والتقدّم") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(settings.widgetProgressStyle == "classic", { vm.setWidgetProgressStyle("classic") }, { Text("كلاسيكيّ") })
                    FilterChip(settings.widgetProgressStyle == "center", { vm.setWidgetProgressStyle("center") }, { Text("مركزيّ") })
                    FilterChip(settings.widgetProgressStyle == "day", { vm.setWidgetProgressStyle("day") }, { Text("يوميّ") })
                }
            }
        }

        SectionCard("عامّ", openSection == "عامّ", { toggle("عامّ") }) {
            LabeledRow("طريقة الخروج من التطبيق") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(settings.exitMode != "double", { vm.setExitMode("dialog") }, { Text("نافذة تأكيد") })
                    FilterChip(settings.exitMode == "double", { vm.setExitMode("double") }, { Text("نقرتان للرجوع") })
                }
            }
            Text(
                "عند ضغط «رجوع» من الرئيسيّة: إمّا نافذةُ تأكيدٍ للخروج، أو الضغط مرّتين متتاليتين للخروج.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        SectionCard("تنزيل المحتوى", openSection == "تنزيل المحتوى", { toggle("تنزيل المحتوى") }) {
            DownloadsSectionContent()
        }

        SectionCard("النسخ الاحتياطيّ والمشاركة", openSection == "النسخ الاحتياطيّ والمشاركة", { toggle("النسخ الاحتياطيّ والمشاركة") }) {
            Text(
                "صدّر إعداداتك ومواقيتك المخزّنة وما نزّلته من القرآن (صوتٌ/مصحف) في حزمةٍ واحدة، أو شاركها. عند الاستيراد تختار ما تستعيده.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { vm.loadBackupSizes { backupSizes = it; exportMode = "export" } }, modifier = Modifier.weight(1f)) { Text("تصدير") }
                FilledTonalButton(onClick = { vm.loadBackupSizes { backupSizes = it; exportMode = "share" } }, modifier = Modifier.weight(1f)) { Text("مشاركة") }
                FilledTonalButton(onClick = { importZipLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }, modifier = Modifier.weight(1f)) { Text("استيراد") }
            }
        }

        SectionCard("مشاركة التطبيق", openSection == "مشاركة التطبيق", { toggle("مشاركة التطبيق") }) {
            Text(
                "الرابط يُرسل نصًّا، وملفّ التثبيت يُرسل نسخةَ التطبيق نفسها — تنفع لمن لا إنترنت عنده.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            FilledTonalButton(
                onClick = { io.github.salehgnutux.gtsalat.util.Share.shareLink(context) },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) { Text("🔗 مشاركة الرابط") }
            FilledTonalButton(
                onClick = { shareScope.launch { io.github.salehgnutux.gtsalat.util.Share.shareApk(context) } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("📦 مشاركة ملفّ التثبيت") }
        }

        SectionCard("حول", openSection == "حول", { toggle("حول") }) {
            InfoRow("النسخة", "GT-SALAT ${BuildConfig.VERSION_NAME}")
            InfoRow("الإصدار", if (BuildConfig.USES_GMS) "كاملة (خدمات Google)" else "حرّة (بلا Google)")
            FilledTonalButton(onClick = { showWhatsNew = true }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text("🎉 ما الجديد في هذا الإصدار")
            }
            FilledTonalButton(
                onClick = { vm.replayTour(); onShowTour() },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) { Text("🎬 عرض الشرح المرئيّ للواجهة") }
            // خيار التحديث الذاتيّ يظهر في نكهة foss فقط (تنزيلٌ مباشر + F-Droid)؛ يُخفى في full (بلاي).
            if (!io.github.salehgnutux.gtsalat.ui.update.SELF_UPDATE_AVAILABLE) {
                // بلا هذا يبدو غياب خيارات التحديث خللاً.
                Text(
                    "إشعارات التحديث والتحديث من داخل التطبيق في النسخة الحرّة (foss). أمّا هذه النسخة فمخصّصةٌ لغوغل بلاي، وتتحدّث من المتجر.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            if (io.github.salehgnutux.gtsalat.ui.update.SELF_UPDATE_AVAILABLE) {
                SwitchRow("إشعارٌ عند توفّر نسخةٍ جديدة", settings.checkUpdates) { vm.setCheckUpdates(it) }
                if (settings.checkUpdates) {
                    SwitchRow("إشعارٌ عند توفّر تحديثاتٍ اختباريّة", settings.allowPrereleaseUpdates) { vm.setAllowPrereleaseUpdates(it) }
                    Text(
                        "نسخٌ ألفا وبيتا لتجربة الجديد قبل صدوره، وقد تحوي أخطاء.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                // فحصٌ يدويّ يعمل ولو أُطفئت الإشعارات، ويتبع خيار التحديثات الاختباريّة.
                val updateStatus by vm.updateCheckStatus.collectAsStateWithLifecycle()
                FilledTonalButton(
                    onClick = { vm.checkUpdatesNow() },
                    enabled = updateStatus != "",
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                ) { Text(if (updateStatus == "") "جارٍ الفحص…" else "🔄 فحص التحديثات الآن") }
                updateStatus?.takeIf { it.isNotEmpty() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            HorizontalDivider()
            LinkRow("المطوّر", io.github.salehgnutux.gtsalat.domain.Credits.DEVELOPER) { openUrl(context, io.github.salehgnutux.gtsalat.domain.Credits.GITHUB) }
            LinkRow("المستودع (GitHub)", "الشيفرة المصدريّة") { openUrl(context, io.github.salehgnutux.gtsalat.domain.Credits.REPO) }
            LinkRow("موقع المشروع", "الصفحة التعريفيّة") { openUrl(context, io.github.salehgnutux.gtsalat.domain.Credits.WEBSITE) }
            LinkRow("مشاريع GNUTUX", "بقيّة مشاريع المطوّر") { openUrl(context, io.github.salehgnutux.gtsalat.domain.Credits.PROJECTS) }
        }

        SectionCard("المصادر المعتمَدة", openSection == "المصادر المعتمَدة", { toggle("المصادر المعتمَدة") }) {
            Text(
                "أُثري التطبيق بمحتوىً وبياناتٍ من مشاريعَ حرّةٍ ومفتوحةِ المصدر، بالشكر والتقدير:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            io.github.salehgnutux.gtsalat.domain.Credits.SOURCES.forEach { src ->
                LinkRow(src.name, src.note) { openUrl(context, src.url) }
            }
        }
    }

    if (showWhatsNew) WhatsNewDialog(onDismiss = { showWhatsNew = false })

    // حوار اختيار محتوى التصدير/المشاركة.
    val sizes = backupSizes
    if (exportMode != null && sizes != null) {
        val items = listOf(
            BackupPickItem("settings", "الإعدادات وحافظة القرآن", "تفضيلاتك، وموضعا القراءة والاستماع، والإشارات", available = true),
            BackupPickItem("prayers", "مواقيت الصلاة المخزّنة", "${sizes.prayersCount} يوماً", available = sizes.prayersCount > 0),
            BackupPickItem("audio", "القرآن الصوتيّ المُنزَّل", formatBytes(sizes.audioBytes), available = sizes.audioBytes > 0),
            BackupPickItem("mushaf", "المصحف المصوَّر المُنزَّل", formatBytes(sizes.mushafBytes), available = sizes.mushafBytes > 0),
        )
        val share = exportMode == "share"
        BackupSelectionDialog(
            title = if (share) "مشاركة نسخة" else "تصدير نسخة",
            confirmLabel = if (share) "مشاركة" else "تصدير",
            items = items,
            onDismiss = { exportMode = null },
            onConfirm = { opts ->
                val chosen = exportMode
                exportMode = null
                pendingOpts = opts
                if (chosen == "share") {
                    busy = "جارٍ تجهيز الحزمة للمشاركة…"
                    vm.shareBundle(opts) { uri ->
                        busy = null
                        if (uri == null) toast("تعذّر تجهيز الحزمة") else {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "application/zip"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            runCatching { context.startActivity(Intent.createChooser(send, "مشاركة النسخة").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        }
                    }
                } else {
                    exportZipLauncher.launch("GT-SALAT-backup.zip")
                }
            },
        )
    }

    // حوار اختيار ما يُستعاد من الحزمة (يعرض المتاح فيها فقط).
    val contents = importContents
    val uri = importUri
    if (contents != null && uri != null) {
        val items = listOf(
            BackupPickItem("settings", "الإعدادات وحافظة القرآن", "تفضيلاتك، وموضعا القراءة والاستماع، والإشارات", available = contents.hasSettings),
            BackupPickItem("prayers", "مواقيت الصلاة المخزّنة", "${contents.prayersCount} يوماً", available = contents.hasPrayers),
            BackupPickItem("audio", "القرآن الصوتيّ", formatBytes(contents.audioBytes), available = contents.hasAudio),
            BackupPickItem("mushaf", "المصحف المصوَّر", formatBytes(contents.mushafBytes), available = contents.hasMushaf),
        )
        BackupSelectionDialog(
            title = "استيراد نسخة",
            confirmLabel = "استيراد",
            items = items,
            onDismiss = { importContents = null; importUri = null },
            onConfirm = { opts ->
                importContents = null; importUri = null
                busy = "جارٍ الاستيراد…"
                vm.importBundle(uri, opts) { res ->
                    busy = null
                    toast(if (res.ok) "تمّ الاستيراد (${res.prayers} يوماً · ${res.files} ملفّاً)" else "تعذّر الاستيراد")
                }
            },
        )
    }

    // حوار انتظارٍ غير قابلٍ للإغلاق أثناء تجهيز/فحص/استيراد الحزمة (قد تطول مع الملفّات الكبيرة).
    busy?.let { msg ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { },
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text("يرجى الانتظار") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    androidx.compose.material3.CircularProgressIndicator()
                    Text(msg)
                }
            },
            confirmButton = { },
        )
    }
}

/** عنصرٌ في حوار اختيار محتوى النسخة. */
private data class BackupPickItem(val key: String, val label: String, val subtitle: String, val available: Boolean)

/** حوارٌ بقائمة اختياراتٍ (خانات) لمحتوى التصدير/المشاركة/الاستيراد. المتعذّر معطَّل. */
@Composable
private fun BackupSelectionDialog(
    title: String,
    confirmLabel: String,
    items: List<BackupPickItem>,
    onDismiss: () -> Unit,
    onConfirm: (io.github.salehgnutux.gtsalat.data.BackupOptions) -> Unit,
) {
    val checked = remember { mutableStateMapOf<String, Boolean>().apply { items.forEach { put(it.key, it.available) } } }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().clickable(enabled = item.available) { checked[item.key] = !(checked[item.key] ?: false) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = item.available && (checked[item.key] ?: false),
                            onCheckedChange = { if (item.available) checked[item.key] = it },
                            enabled = item.available,
                        )
                        Column(Modifier.weight(1f)) {
                            Text(item.label, color = if (item.available) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
                            if (item.subtitle.isNotBlank()) Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            else if (!item.available) Text("غير متوفّر", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        },
        confirmButton = {
            val opts = io.github.salehgnutux.gtsalat.data.BackupOptions(
                settings = checked["settings"] == true,
                prayers = checked["prayers"] == true,
                audio = checked["audio"] == true,
                mushaf = checked["mushaf"] == true,
            )
            androidx.compose.material3.TextButton(onClick = { onConfirm(opts) }, enabled = opts.any) { Text(confirmLabel) }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

/** يصوغ حجماً بالبايت إلى نصٍّ مقروء (بأرقامٍ لاتينيّة 0-9). */
private fun formatBytes(bytes: Long): String = when {
    bytes <= 0 -> "لا شيء"
    bytes < 1024 * 1024 -> String.format(java.util.Locale.US, "%.0f كيلوبايت", bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f ميغابايت", bytes / (1024.0 * 1024))
    else -> String.format(java.util.Locale.US, "%.2f غيغابايت", bytes / (1024.0 * 1024 * 1024))
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/** صفٌّ قابلٌ للنقر يفتح رابطاً: عنوانٌ ووصفٌ وأيقونة فتح. */
@Composable
private fun LinkRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        Arrangement.SpaceBetween,
        Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "فتح", tint = MaterialTheme.colorScheme.outline)
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
private fun ReliabilityCard(expanded: Boolean, onToggle: () -> Unit, onTest: () -> Unit) {
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

    val notifOk = remember(refreshKey) { notificationsOk(context) }
    val exactOk = remember(refreshKey) { canScheduleExact(context) }
    val batteryOk = remember(refreshKey) { isBatteryUnrestricted(context) }
    val fullScreenOk = remember(refreshKey) { canUseFullScreenIntent(context) }
    val allOk = notifOk && exactOk && batteryOk && fullScreenOk

    SectionCard(if (allOk) "موثوقيّة التنبيهات ✓" else "موثوقيّة التنبيهات", expanded, onToggle) {
        Text(
            "الصلاحيات للعمل في الخلفيّة — تضمن وصول الأذان والأذكار في وقتها حتى والتطبيق مغلق:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        // كلّ صلاحيّةٍ تظهر دائماً بشارة حالةٍ («مفوّض»/«ينقص الإذن»)، وزرُّ التفعيل عند نقصها فقط.
        PermStatusRow(
            title = "إشعارات النظام",
            desc = "بدونها لا تظهر التنبيهات أصلاً.",
            granted = notifOk,
            onFix = { openNotificationSettings(context) },
        )
        PermStatusRow(
            title = "المنبّهات الدقيقة",
            desc = "لإطلاق الأذان في لحظته بالضبط (أندرويد 12 فأحدث).",
            granted = exactOk,
            onFix = { openExactAlarmSettings(context) },
        )
        PermStatusRow(
            title = "إعفاء من تحسين البطاريّة",
            desc = "يمنع النظام من تعطيل التطبيق في الخلفيّة (مهمّ على أجهزة Xiaomi وOppo وHuawei وغيرها).",
            granted = batteryOk,
            onFix = { openBatteryExemption(context) },
        )
        PermStatusRow(
            title = "نافذة الأذان فوق القفل",
            desc = "لإيقاظ الشاشة وإظهار نافذة الأذان فورَ دخول الوقت (أندرويد 14 فأحدث).",
            granted = fullScreenOk,
            onFix = { openFullScreenIntentSettings(context) },
        )
        // التشغيل التلقائيّ (autostart) — إعدادٌ خاصٌّ ببعض الأنظمة لا يُقرأ برمجيّاً؛ إرشادٌ دائمٌ حسب المُصنّع.
        PermStatusRow(
            title = "التشغيل التلقائيّ وتقييد الخلفيّة",
            desc = oemAutostartDesc(),
            granted = null,
            onFix = { openAutoStartSettings(context) },
        )
        HorizontalDivider()
        // زرّ اختبارٍ عمليّ: يجدول تنبيهاً بعد دقيقة ليتحقّق المستخدم والشاشة مغلقة.
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("اختبار التنبيه", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("يصلك تنبيهٌ بعد دقيقة. اقفل الشاشة الآن للتحقّق من وصوله في الخلفيّة.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            FilledTonalButton(onClick = {
                onTest()
                Toast.makeText(context, "سيصلك تنبيهٌ اختباريٌّ بعد دقيقة — اقفل الشاشة", Toast.LENGTH_LONG).show()
            }) { Text("جرّب") }
        }
        HorizontalDivider()
        // سجلّ وصول التنبيهات: يبيّن هل حبسها النظام أم تعطّل شيءٌ داخل التطبيق.
        var showLog by remember { mutableStateOf(false) }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("سجلّ التنبيهات", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("موعد كلّ تنبيهٍ ووقت وصوله فعلاً. يبقى على هاتفك ولا يُرسَل.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            FilledTonalButton(onClick = { showLog = true }) { Text("عرض") }
        }
        if (showLog) AlarmLogDialog { showLog = false }
    }
}

@Composable
private fun AlarmLogDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var log by remember { mutableStateOf(io.github.salehgnutux.gtsalat.alarm.AlarmLog.read(context)) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("سجلّ التنبيهات") },
        text = {
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(
                    log.ifBlank { "لا تنبيهات مسجَّلة بعد." },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(enabled = log.isNotBlank(), onClick = {
                val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("GT-SALAT", log))
                Toast.makeText(context, "نُسخ السجلّ", Toast.LENGTH_SHORT).show()
            }) { Text("نسخ") }
        },
        dismissButton = {
            Row {
                androidx.compose.material3.TextButton(enabled = log.isNotBlank(), onClick = {
                    io.github.salehgnutux.gtsalat.alarm.AlarmLog.clear(context)
                    log = ""
                }) { Text("مسح") }
                androidx.compose.material3.TextButton(onClick = onDismiss) { Text("إغلاق") }
            }
        },
    )
}

/**
 * صفُّ صلاحيّةٍ دائم الظهور بشارة حالة: [granted] = true «مفوّض» · false «ينقص الإذن» ·
 * null لا يُقرأ برمجيّاً (كالتشغيل التلقائيّ) فبلا شارة. زرُّ «تفعيل» يظهر ما لم تكن مفوّضة.
 */
@Composable
private fun PermStatusRow(title: String, desc: String, granted: Boolean?, onFix: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                when (granted) {
                    true -> StatusChip("مفوّض", MaterialTheme.colorScheme.primary)
                    false -> StatusChip("ينقص الإذن", Color(0xFFCC8A00))
                    null -> {}
                }
            }
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        if (granted != true) {
            FilledTonalButton(onClick = onFix) { Text("تفعيل") }
        }
    }
}

/** شارةُ حالةٍ صغيرةٌ ملوّنة (كبسولة). */
@Composable
private fun StatusChip(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

private fun notificationsOk(context: Context): Boolean =
    androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun openNotificationSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }.onFailure { openAppDetails(context) }
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

/** أندرويد 14+ قد يمنع نافذة ملء الشاشة (full-screen intent) افتراضيّاً؛ نتحقّق ونوجّه لتفعيلها. */
private fun canUseFullScreenIntent(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    return runCatching { nm.canUseFullScreenIntent() }.getOrDefault(true)
}

private fun openFullScreenIntentSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { openAppDetails(context); return }
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }.onFailure { openAppDetails(context) }
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

/** إرشادُ التشغيل التلقائيّ حسب المُصنّع (يظهر نصّاً في البطاقة). */
private fun oemAutostartDesc(): String {
    val m = Build.MANUFACTURER.lowercase(java.util.Locale.US)
    return when {
        m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") ->
            "على أجهزة Xiaomi/‏MIUI: «الأمان» ← «الأذونات» ← «التشغيل التلقائيّ» وفعّله لـGT-SALAT، واجعل توفير البطاريّة «بلا قيود»."
        m.contains("oppo") || m.contains("realme") ->
            "على أجهزة Oppo/‏Realme: الإعدادات ← «إدارة التطبيقات» ← فعّل «التشغيل التلقائيّ» و«العمل في الخلفيّة» لـGT-SALAT."
        m.contains("vivo") || m.contains("iqoo") ->
            "على أجهزة Vivo/‏iQOO: الإعدادات ← «البطاريّة» ← «الاستهلاك العالي في الخلفيّة»، وفعّل «التشغيل التلقائيّ» لـGT-SALAT."
        m.contains("huawei") || m.contains("honor") ->
            "على أجهزة Huawei/‏Honor: «مدير الهاتف» ← «التشغيل» ← اجعله يدويّاً وفعّل (التشغيل التلقائيّ + الثانويّ + العمل في الخلفيّة)."
        m.contains("samsung") ->
            "على أجهزة Samsung: الإعدادات ← «البطاريّة» ← «حدود الاستخدام في الخلفيّة» ← أضِف GT-SALAT إلى «التطبيقات التي لن تنام أبداً»."
        m.contains("infinix") || m.contains("tecno") || m.contains("itel") || m.contains("transsion") ->
            "على أجهزة Infinix/‏Tecno/‏itel: فعّل لـGT-SALAT «التشغيل التلقائيّ» (Auto-launch)، و«العرض فوق التطبيقات»، و«الإظهار على شاشة القفل»، واجعل البطاريّة «بلا قيود» — وإلّا لم تظهر نافذة الأذان فوق القفل رغم صدور الصوت."
        else ->
            "بعض الأنظمة توقف التطبيق رغم إعفاء البطاريّة. فعّل «التشغيل التلقائيّ» واجعل البطاريّة «بلا قيود» لهذا التطبيق."
    }
}

/** فتحُ شاشة «التشغيل التلقائيّ» الخاصّة بالمُصنّع مباشرةً إن أمكن، وإلّا تفاصيل التطبيق. */
private fun openAutoStartSettings(context: Context) {
    val candidates = listOf(
        "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",
        "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
        "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
        "com.oppo.safe" to "com.oppo.safe.permission.startup.StartupAppListActivity",
        "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
        "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
        "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
        "com.huawei.systemmanager" to "com.huawei.systemmanager.optimize.process.ProtectActivity",
        // Transsion: Infinix (XOS) · Tecno (HiOS) · itel — «مدير الهاتف»/التشغيل التلقائيّ
        "com.transsion.phonemanager" to "com.itel.autobootmanager.activity.AutoBootMgrActivity",
        "com.transsion.phonemanager" to "com.cyin.himgr.autostart.AutoStartActivity",
    )
    for ((pkg, cls) in candidates) {
        val intent = Intent().apply {
            component = android.content.ComponentName(pkg, cls)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (context.packageManager.resolveActivity(intent, 0) != null) {
            if (runCatching { context.startActivity(intent) }.isSuccess) return
        }
    }
    openAppDetails(context)   // لا شاشة معروفة لهذا المُصنّع → تفاصيل التطبيق.
}

/** أداة الألوان الكاملة: لون السِمة (HSV) + سواتر + تخصيص تدرّج الخلفيّة للوضع الحاليّ. */
@Composable
private fun ColorTool(settings: AppSettings, vm: SettingsViewModel) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val presets = listOf(
        0, 0xFF1B6B4C.toInt(), 0xFF00796B.toInt(), 0xFF1565C0.toInt(),
        0xFF6A1B9A.toInt(), 0xFFC9A227.toInt(), 0xFFB5651D.toInt(), 0xFFAD1457.toInt(),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("لون السِمة", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(presets) { argb ->
                val selected = settings.seedColor == argb
                val swatch = if (argb == 0) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color(argb)
                androidx.compose.foundation.layout.Box(
                    Modifier.size(38.dp).clip(CircleShape).background(swatch)
                        .then(if (argb == 0) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape) else Modifier)
                        .clickable { vm.setSeedColor(argb) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
                    else if (argb == 0) Text("×", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        HsvPicker(if (settings.seedColor != 0) settings.seedColor else MaterialTheme.colorScheme.primary.toArgb()) { vm.setSeedColor(it) }

        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("تدرّج الخلفيّة (${if (dark) "داكن" else "فاتح"})", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            androidx.compose.material3.TextButton(onClick = { vm.resetGradient(dark) }) { Text("تلقائيّ") }
        }
        val gTop = if (dark) settings.gradTopDark else settings.gradTopLight
        val gBot = if (dark) settings.gradBotDark else settings.gradBotLight
        Text("أعلى التدرّج", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        HsvPicker(if (gTop != 0) gTop else MaterialTheme.colorScheme.background.toArgb()) { vm.setGradient(dark, true, it) }
        Text("أسفل التدرّج", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        HsvPicker(if (gBot != 0) gBot else MaterialTheme.colorScheme.primary.toArgb()) { vm.setGradient(dark, false, it) }
    }
}

/** منتقي لونٍ كامل: لون (Hue) + إشباع + إضاءة، مع معاينةٍ حيّة. يبثّ عند ترك المنزلق. */
@Composable
private fun HsvPicker(argb: Int, onChange: (Int) -> Unit) {
    val init = remember(argb) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(argb, it) }
    }
    var h by remember(argb) { mutableStateOf(init[0]) }
    var s by remember(argb) { mutableStateOf(init[1]) }
    var v by remember(argb) { mutableStateOf(init[2]) }
    fun emit() = onChange(androidx.compose.ui.graphics.Color.hsv(h, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f)).toArgb())

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        androidx.compose.foundation.layout.Box(
            Modifier.size(44.dp).clip(CircleShape)
                .background(androidx.compose.ui.graphics.Color.hsv(h, s, v))
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
        )
        Column(Modifier.weight(1f)) {
            Slider(value = h, onValueChange = { h = it }, valueRange = 0f..360f, onValueChangeFinished = { emit() })
            Slider(value = s, onValueChange = { s = it }, valueRange = 0f..1f, onValueChangeFinished = { emit() })
            Slider(value = v, onValueChange = { v = it }, valueRange = 0f..1f, onValueChangeFinished = { emit() })
        }
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
private fun AlertModeChips(mode: AdhanAlertMode, onPick: (AdhanAlertMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(mode == AdhanAlertMode.FULL, { onPick(AdhanAlertMode.FULL) }, { Text("أذان") })
        FilterChip(mode == AdhanAlertMode.TONE, { onPick(AdhanAlertMode.TONE) }, { Text("رنّة") })
        FilterChip(mode == AdhanAlertMode.SILENT, { onPick(AdhanAlertMode.SILENT) }, { Text("صامت") })
    }
}

@Composable
private fun MinutesSlider(prefix: String, minutes: Int, min: Int, max: Int, suffix: String = "دقيقة", onChange: (Int) -> Unit) {
    var v by remember(minutes) { mutableStateOf(minutes.toFloat()) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // القيمة الحيّة تظهر أثناء السحب لا بعد تركه فقط.
        Text("$prefix ${v.toInt()} $suffix", style = MaterialTheme.typography.bodyLarge)
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
        MinutesSlider("يُكتم لمدّة", minutes, 5, 60, onChange = onChange)
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

/** صفّ مفتاحٍ بزرّ معاينةٍ صوتيّة (يبدّل تشغيل/إيقاف حسب [playing]). */
@Composable
private fun SwitchRowPreview(label: String, checked: Boolean, playing: Boolean, onPreview: () -> Unit, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPreview) {
                Icon(if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = if (playing) "إيقاف" else "معاينة", tint = MaterialTheme.colorScheme.primary)
            }
            Switch(checked, onChange)
        }
    }
}

/** صفّ معاينةٍ صوتيّة (زرّ تشغيل/إيقاف حسب [playing]). */
@Composable
private fun PreviewRow(label: String, playing: Boolean, onPreview: () -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        IconButton(onClick = onPreview) {
            Icon(if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = if (playing) "إيقاف" else "تشغيل", tint = MaterialTheme.colorScheme.primary)
        }
    }
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
