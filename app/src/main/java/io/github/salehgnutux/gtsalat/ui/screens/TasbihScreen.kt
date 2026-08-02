package io.github.salehgnutux.gtsalat.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.salehgnutux.gtsalat.ui.theme.AmiriQuran

@Composable
fun TasbihScreen(onBack: () -> Unit, vm: TasbihViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val view = LocalView.current
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("التسبيح", onBack)
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // اختيار الوضع: عاديّ (ذكرٌ واحد) أو مختلط (دبر الصلاة بعدد السنّة)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !ui.mixed, onClick = { vm.setMixed(false) }, label = { Text("عاديّ") })
                FilterChip(selected = ui.mixed, onClick = { vm.setMixed(true) }, label = { Text("مختلط (دبر الصلاة)") })
            }

            // في الوضع العاديّ فقط: اختيار الذِّكر
            if (!ui.mixed) {
                LazyChips(
                    options = TasbihUi.DHIKR_LIST,
                    selectedIndex = ui.dhikrIndex,
                    onSelect = { vm.setDhikr(it) },
                )
            } else {
                Text(
                    "الخطوة ${ui.mixedStep + 1} من ${TasbihUi.MIXED.size} · المجموع ${ui.mixedOverall} / ${TasbihUi.MIXED_TOTAL}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                ui.dhikr,
                fontFamily = AmiriQuran,
                fontSize = if (ui.mixed) 24.sp else 30.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            // دائرة العدّ الكبيرة — انقر في أيّ مكانٍ منها للتسبيح
            Box(
                Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable {
                        vm.increment()
                        view.performHapticFeedbackCompat()
                        // بعد الزيادة نقرأ الحالة الجديدة للاهتزاز عند بلوغ الهدف
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${ui.inLap}",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    if (ui.stepTarget > 0) {
                        Text(
                            "${ui.inLap} / ${ui.stepTarget}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            if (ui.mixed && ui.mixedDone) {
                Text("✓ تمّ تسبيح دبر الصلاة (${TasbihUi.MIXED_TOTAL}) — تقبّل الله", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            } else if (!ui.mixed && ui.laps > 0) {
                Text("اكتملت ${ui.laps} لفّة", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }

            // الأهداف (الوضع العاديّ فقط)
            if (!ui.mixed) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TasbihUi.TARGETS.forEach { t ->
                        FilterChip(
                            selected = ui.target == t,
                            onClick = { vm.setTarget(t) },
                            label = { Text(if (t == 0) "بلا حدّ" else "$t") },
                        )
                    }
                }
            }

            FilledTonalButton(onClick = { vm.reset(); view.performHapticFeedbackCompat() }) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Text("  تصفير", style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    // اهتزازٌ واضحٌ مرّةً واحدة عند بلوغ حدٍّ (هدفٍ أو نهاية خطوةٍ مختلطة) — مفتاحه pulse فلا يتكرّر.
    androidx.compose.runtime.LaunchedEffect(ui.pulse) {
        if (ui.pulse > 0) vibrateOnce(context)
    }
}

@Composable
private fun LazyChips(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        itemsIndexed(options) { i, opt ->
            FilterChip(
                selected = selectedIndex == i,
                onClick = { onSelect(i) },
                label = { Text(opt.take(14)) },
            )
        }
    }
}

private fun android.view.View.performHapticFeedbackCompat() {
    performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
}

private fun vibrateOnce(context: Context) {
    val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    runCatching { vib.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)) }
}
