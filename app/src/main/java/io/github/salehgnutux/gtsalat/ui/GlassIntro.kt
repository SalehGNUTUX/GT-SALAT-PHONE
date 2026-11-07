package io.github.salehgnutux.gtsalat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** معاينةٌ مؤقّتةٌ للسمة الزجاجيّة: تغيّر مظهر التطبيق دون حفظ، حتى يؤكّد المستخدم أو يتراجع. */
object GlassPreview {
    private val _override = MutableStateFlow<Boolean?>(null)
    val override: StateFlow<Boolean?> = _override.asStateFlow()
    fun set(v: Boolean?) { _override.value = v }
}

/**
 * نافذة التعريف بالسمة الزجاجيّة بعد التثبيت أو التحديث: بطاقةٌ أسفل الشاشة بلا تعتيم، فيرى المستخدم أثر
 * اختياره على الرئيسيّة نفسها مباشرةً، ولا يُحفظ شيءٌ إلّا بالتأكيد. والرجوع يعيد ما كان.
 */
@Composable
fun GlassIntroOverlay(
    saved: Boolean,
    onConfirm: (Boolean) -> Unit,
    onConfirmAndOpenSettings: (Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    val preview by GlassPreview.override.collectAsState()
    LaunchedEffect(Unit) { if (GlassPreview.override.value == null) GlassPreview.set(saved) }
    val choice = preview ?: saved
    BackHandler { GlassPreview.set(null); onCancel() }

    Box(
        Modifier
            .fillMaxSize()
            // لا تعتيم، لكنّ اللمس خلف البطاقة لا يصل إلى الواجهة أثناء المعاينة.
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Card(
            Modifier.safeDrawingPadding().padding(12.dp).fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "✨ السمة الزجاجيّة",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "بطاقاتٌ شفّافةٌ كالزجاج فوق خلفيّة الرئيسيّة، بألوانٍ مريحةٍ للعين في الوضعين الداكن والفاتح. " +
                        "جرّب الخيارين: يتغيّر المظهر خلف هذه البطاقة مباشرةً، ولا يُحفَظ اختيارك حتى تضغط «تأكيد».",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(choice, { GlassPreview.set(true) }, { Text("مفعّلة") })
                    FilterChip(!choice, { GlassPreview.set(false) }, { Text("معطّلة") })
                }
                Text(
                    "ولها إعداداتٌ أخرى في الإعدادات ← «خلفيّة الرئيسيّة»: الصورة، وشدّة التغبيش، ودرجة الظهور.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { GlassPreview.set(null); onConfirmAndOpenSettings(choice) },
                        modifier = Modifier.weight(1f),
                    ) { Text("تأكيد وفتح الإعدادات", maxLines = 1) }
                    Button(onClick = { GlassPreview.set(null); onConfirm(choice) }) { Text("تأكيد") }
                }
            }
        }
    }
}
