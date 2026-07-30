package io.github.salehgnutux.gtsalat.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
                    Text(
                        "✓ " + listOf(ui.city, ui.country).filter { it.isNotBlank() }.joinToString("، ").ifBlank { "تمّ تحديد الموقع" },
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                OutlinedButton(
                    onClick = {
                        permLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (ui.hasLocation) "إعادة الاكتشاف" else "اكتشف موقعي") }
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
    }
}

/** شارةُ ميزةٍ سريعة في بطاقة الترحيب: رمزٌ فوق تسمية. */
@Composable
private fun FeatureBadge(emoji: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(emoji, fontSize = 24.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
