package io.github.salehgnutux.gtsalat.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Card
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.salehgnutux.gtsalat.domain.AsrMadhab
import io.github.salehgnutux.gtsalat.domain.CalculationMethods

@Composable
fun SetupScreen(vm: SetupViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { vm.detectLocation() }

    val ctx = androidx.compose.ui.platform.LocalContext.current
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) vm.importBackup(uri) { } }

    // حقلا الإدخال اليدويّ للإحداثيّات (يعمل دون إنترنت).
    var latText by remember { mutableStateOf("") }
    var lonText by remember { mutableStateOf("") }
    // قائمة المواقع المُضمَّنة (دون GPS/إنترنت).
    val places by vm.places.collectAsStateWithLifecycle()
    var showPlaces by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // بطاقة ترحيبٍ متدرّجة: اسم التطبيق + نبذة + ميزاتٌ سريعة
        val cs = MaterialTheme.colorScheme
        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
            Column(
                Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(lerp(cs.primaryContainer, cs.primary, 0.18f), cs.primaryContainer)))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("🕌", fontSize = 60.sp)
                Text("أهلاً بك في GT-SALAT", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = cs.onPrimaryContainer, textAlign = TextAlign.Center)
                Text(
                    "رفيقك الإسلاميّ الشامل — مواقيت وأذان وقرآن وأذكار وقبلة، تعمل كلّها دون إنترنت.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onPrimaryContainer.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                )
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    FeatureBadge("🕐", "مواقيت")
                    FeatureBadge("📖", "قرآن")
                    FeatureBadge("📿", "أذكار")
                    FeatureBadge("🧭", "قبلة")
                }
            }
        }

        Text(
            "لنبدأ بخطوتين بسيطتين:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("📍 الخطوة 1 — الموقع", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (ui.detecting) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(Modifier.padding(2.dp))
                        Text("يجري اكتشاف الموقع…")
                    }
                } else if (ui.hasLocation) {
                    val place = listOf(ui.city, ui.country).filter { it.isNotBlank() }.joinToString("، ")
                    val coords = if (ui.lat != null && ui.lon != null) String.format(java.util.Locale.US, "%.4f، %.4f", ui.lat, ui.lon) else ""
                    Text("✓ " + place.ifBlank { coords.ifBlank { "تمّ تحديد الموقع" } }, color = MaterialTheme.colorScheme.primary)
                }
                ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                ui.info?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) }
                Button(
                    onClick = {
                        permLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (ui.hasLocation) "إعادة الاكتشاف" else "اكتشف موقعي تلقائيّاً (GPS)") }
                // إن كانت خدمة الموقع مطفأة، وجّه المستخدم لتفعيلها (GPS يعمل دون إنترنت).
                if (!locationEnabled(ctx)) {
                    OutlinedButton(
                        onClick = { runCatching { ctx.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("⚙️ فعّل خدمة الموقع (GPS) أولاً") }
                }

                HorizontalDivider()
                // الأسهل دون إنترنت: اختيارٌ من قائمة بلدانٍ ومدنٍ مُضمَّنة.
                FilledTonalButton(onClick = { showPlaces = true }, modifier = Modifier.fillMaxWidth(), enabled = places.isNotEmpty()) {
                    Text("🌍 اختر بلدك ومدينتك من القائمة (دون إنترنت)")
                }

                HorizontalDivider()
                Text("أو أدخِل الإحداثيّات يدويّاً (يعمل دون إنترنت):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latText, onValueChange = { latText = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                        label = { Text("خط العرض") }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        value = lonText, onValueChange = { lonText = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                        label = { Text("خط الطول") }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                OutlinedButton(
                    onClick = {
                        val la = latText.toDoubleOrNull(); val lo = lonText.toDoubleOrNull()
                        if (la != null && lo != null && la in -90.0..90.0 && lo in -180.0..180.0) vm.setManualLocation(la, lo)
                        else android.widget.Toast.makeText(ctx, "أدخِل إحداثيّاتٍ صحيحة (العرض −90..90، الطول −180..180)", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    enabled = latText.isNotBlank() && lonText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("حفظ الإحداثيّات") }

                HorizontalDivider()
                FilledTonalButton(
                    onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("📥 استيراد نسخةٍ احتياطيّة (فيها موقعك وإعداداتك)") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("🧮 الخطوة 2 — طريقة الحساب", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                var expanded by remember { mutableStateOf(false) }
                val info = CalculationMethods.infoOf(ui.methodId)
                Text(
                    info.nameAr,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().clickable { expanded = true }.padding(vertical = 4.dp),
                )
                DropdownMenu(expanded, { expanded = false }) {
                    CalculationMethods.ALL.forEach { m ->
                        DropdownMenuItem(text = { Text(m.nameAr) }, onClick = { vm.setMethod(m.id); expanded = false })
                    }
                }
                Text("مذهب حساب العصر", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(ui.madhab == AsrMadhab.SHAFI, { vm.setMadhab(AsrMadhab.SHAFI) }, { Text("الجمهور") })
                    FilterChip(ui.madhab == AsrMadhab.HANAFI, { vm.setMadhab(AsrMadhab.HANAFI) }, { Text("الحنفيّ") })
                }
            }
        }

        Button(
            onClick = { vm.finish() },
            enabled = ui.hasLocation,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (ui.hasLocation) "ابدأ الآن" else "حدّد موقعك أولاً") }
        OutlinedButton(onClick = { vm.skip() }, modifier = Modifier.fillMaxWidth()) {
            Text("تخطّي الآن (يمكنك تحديد الموقع لاحقاً)")
        }
    }

    if (showPlaces) {
        PlacePickerDialog(places = places, onDismiss = { showPlaces = false }) { p ->
            vm.pickPlace(p); showPlaces = false
        }
    }
}

/** هل خدمة الموقع (GPS/الشبكة) مفعّلةٌ في النظام؟ */
private fun locationEnabled(ctx: android.content.Context): Boolean = runCatching {
    val lm = ctx.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
    lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
        lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
}.getOrDefault(true)

/** شارةُ ميزةٍ سريعة في بطاقة الترحيب: رمزٌ فوق تسمية. */
@Composable
private fun FeatureBadge(emoji: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(emoji, fontSize = 24.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

/** حوارُ اختيار موقعٍ من القائمة المُضمَّنة، ببحثٍ بالبلد أو المدينة. */
@Composable
private fun PlacePickerDialog(
    places: List<io.github.salehgnutux.gtsalat.domain.Place>,
    onDismiss: () -> Unit,
    onPick: (io.github.salehgnutux.gtsalat.domain.Place) -> Unit,
) {
    var q by remember { mutableStateOf("") }
    val nq = io.github.salehgnutux.gtsalat.domain.Quran.normalize(q)
    val shown = remember(nq, places) {
        if (nq.isBlank()) places
        else places.filter {
            io.github.salehgnutux.gtsalat.domain.Quran.normalize("${it.country} ${it.city}").contains(nq)
        }
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختر موقعك") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = q, onValueChange = { q = it }, singleLine = true,
                    label = { Text("ابحث ببلدٍ أو مدينة") }, modifier = Modifier.fillMaxWidth(),
                )
                androidx.compose.foundation.lazy.LazyColumn(
                    Modifier.fillMaxWidth().heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(shown) { p ->
                        Column(
                            Modifier.fillMaxWidth().clickable { onPick(p) }.padding(vertical = 8.dp),
                        ) {
                            Text(p.city, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text(p.country, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
    )
}
