package io.github.salehgnutux.gtsalat.ui.screens

import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.salehgnutux.gtsalat.BuildConfig
import io.github.salehgnutux.gtsalat.data.settings.AdhanType
import io.github.salehgnutux.gtsalat.data.settings.ThemeMode
import io.github.salehgnutux.gtsalat.domain.AsrMadhab
import io.github.salehgnutux.gtsalat.domain.CalculationMethods

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

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard("الموقع وطريقة الحساب") {
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

        SectionCard("الأذان والتنبيهات") {
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
            SwitchRow("دعاء بعد الأذان", settings.enableDuaAfterAdhan) { vm.setEnableDua(it) }
            SwitchRow("تنبيه الاقتراب قبل الصلاة", settings.enablePreNotify) { vm.setEnablePreNotify(it) }
            if (settings.enablePreNotify) {
                PreNotifySlider(settings.preNotifyMinutes) { vm.setPreNotify(it) }
            }
            SwitchRow("وضع عدم الإزعاج", settings.doNotDisturb) { vm.setDnd(it) }
        }

        SectionCard("المظهر") {
            LabeledRow("السِمة") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(settings.themeMode == ThemeMode.SYSTEM, { vm.setTheme(ThemeMode.SYSTEM) }, { Text("النظام") })
                    FilterChip(settings.themeMode == ThemeMode.LIGHT, { vm.setTheme(ThemeMode.LIGHT) }, { Text("فاتح") })
                    FilterChip(settings.themeMode == ThemeMode.DARK, { vm.setTheme(ThemeMode.DARK) }, { Text("داكن") })
                }
            }
            SwitchRow("الألوان الديناميكيّة (Material You)", settings.dynamicColor) { vm.setDynamicColor(it) }
        }

        SectionCard("حول") {
            InfoRow("النسخة", "GT-SALAT ${BuildConfig.VERSION_NAME}")
            InfoRow("الإصدار", if (BuildConfig.USES_GMS) "كاملة (خدمات Google)" else "حرّة (بلا Google)")
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            content()
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
private fun PreNotifySlider(minutes: Int, onChange: (Int) -> Unit) {
    var v by remember(minutes) { mutableStateOf(minutes.toFloat()) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // القيمة الحيّة تظهر أثناء السحب لا بعد تركه فقط.
        Text("قبل الصلاة بـ ${v.toInt()} دقيقة", style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = v,
            onValueChange = { v = it },
            valueRange = 1f..60f,
            onValueChangeFinished = { onChange(v.toInt()) },
            modifier = Modifier.fillMaxWidth(),
        )
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
