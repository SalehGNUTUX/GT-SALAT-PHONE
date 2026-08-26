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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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

    // الوضع الصوتيّ: كلّ نطقةٍ = تسبيحة (يعمل دون إنترنت). الحالة مرفوعةٌ لتُستعمَل في الترويسة والجسم.
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val voice = androidx.compose.runtime.remember { io.github.salehgnutux.gtsalat.audio.VoiceCounter() }
    var voiceOn by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var sensitivity by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(60) }
    fun startVoice() { voice.stop(); voice.start(scope, sensitivity) { vm.increment(); view.performHapticFeedbackCompat() } }
    val micLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) { voiceOn = true; startVoice() }
        else android.widget.Toast.makeText(context, "يلزم إذن الميكروفون لتشغيل الوضع الصوتيّ", android.widget.Toast.LENGTH_SHORT).show()
    }
    fun toggleVoice() {
        if (voiceOn) { voiceOn = false; voice.stop() } else {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (granted) { voiceOn = true; startVoice() } else micLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { voice.stop() } }

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("التسبيح", onBack, actions = {
            IconButton(onClick = { toggleVoice() }) {
                Icon(
                    if (voiceOn) Icons.Filled.Mic else Icons.Filled.MicNone,
                    contentDescription = "الوضع الصوتيّ",
                    tint = if (voiceOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // اختيار الوضع: عاديّ (ذكرٌ واحد) أو دبر الصلاة (متتالية بعدد السنّة)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !ui.mixed, onClick = { vm.setMixed(false) }, label = { Text("عاديّ") })
                FilterChip(selected = ui.mixed, onClick = { vm.setMixed(true) }, label = { Text("دبر الصلاة") })
            }

            // ضبط الوضع الصوتيّ — بطاقةٌ مضغوطةٌ تظهر عند التفعيل فقط.
            if (voiceOn) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text(
                            "🎙️ يستمع — كلّ نطقةٍ تُحسَب تسبيحة. اضبط الحساسيّة إن عدّ الضجيج أو لم يستجب:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        androidx.compose.material3.Slider(
                            value = sensitivity / 100f,
                            onValueChange = { sensitivity = (it * 100).toInt().coerceIn(0, 100) },
                            onValueChangeFinished = { startVoice() },
                        )
                    }
                }
            }

            // العاديّ: اختيار الذِّكر · المختلط: اختيار النوع (كلماتُ كلٍّ تتناوب واحدةً بعد الأخرى).
            if (!ui.mixed) {
                LazyChips(
                    options = TasbihUi.DHIKR_LIST,
                    selectedIndex = ui.dhikrIndex,
                    onSelect = { vm.setDhikr(it) },
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TasbihUi.MIXED_TYPES.forEachIndexed { i, t ->
                        FilterChip(selected = ui.mixedType == i, onClick = { vm.setMixedType(i) }, label = { Text(t.name) })
                    }
                }
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

            if (ui.laps > 0) {
                Text(
                    "اكتملت ${ui.laps} ${if (ui.mixed) "دورة" else "لفّة"}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // الأهداف (في الوضعين)
            run {
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
