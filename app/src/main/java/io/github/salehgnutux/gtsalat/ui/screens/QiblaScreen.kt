package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.salehgnutux.gtsalat.util.Format
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun QiblaScreen(vm: QiblaViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("اتّجاه القبلة", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        when {
            !ui.hasLocation -> HintCard("حدّد موقعك أوّلاً من الإعدادات لعرض اتّجاه القبلة.")
            !ui.sensorAvailable -> HintCard("لا يتوفّر في جهازك مستشعر بوصلة، فلا يمكن عرض الاتّجاه الحيّ.\nاتّجاه القبلة من موقعك: ${Format.degrees(ui.qiblaBearing)} عن الشمال.")
            else -> {
                CompassDial(
                    deviceBearing = ui.deviceBearing,
                    qiblaBearing = ui.qiblaBearing,
                    aligned = ui.aligned,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )
                Text(
                    if (ui.aligned) "أنت تواجه القبلة ✓" else "أدِر الجهاز حتى يشير السهم للأعلى",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (ui.aligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        InfoLine("القبلة عن الشمال", Format.degrees(ui.qiblaBearing))
                        InfoLine("اتّجاه الجهاز", Format.degrees(ui.deviceBearing))
                        if (ui.city.isNotBlank()) InfoLine("الموقع", ui.city)
                    }
                }
                Text(
                    "أمسك الجهاز أفقيّاً بعيداً عن المعادن والأجهزة الكهربائيّة لدقّةٍ أعلى.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun CompassDial(
    deviceBearing: Float,
    qiblaBearing: Float,
    aligned: Boolean,
    modifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary
    val marker = if (aligned) primary else MaterialTheme.colorScheme.tertiary
    val northColor = MaterialTheme.colorScheme.error

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = min(size.width, size.height) / 2f * 0.86f
            val c = Offset(size.width / 2f, size.height / 2f)

            // القرص الدوّار: يدور عكس اتّجاه الجهاز فيبقى الشمال مشيراً لشمال الأرض.
            rotate(-deviceBearing, pivot = c) {
                drawCircle(color = outline, radius = r, center = c, style = Stroke(width = 3f))
                // علامات الجهات الأربع
                tick(c, r, 0f, northColor, long = true)   // شمال
                tick(c, r, 90f, onSurface, long = false)
                tick(c, r, 180f, onSurface, long = false)
                tick(c, r, 270f, onSurface, long = false)
                // علامات كلّ 30°
                for (a in 0 until 360 step 30) if (a % 90 != 0) tick(c, r, a.toFloat(), outline, long = false, small = true)
                // علامة القبلة على القرص (بزاوية اتّجاهها الحقيقيّ)
                qiblaMark(c, r, qiblaBearing, marker)
            }

            // المؤشّر الثابت أعلى الشاشة (12): حين تصله علامة القبلة تكون مواجهاً لها.
            val top = Offset(c.x, c.y - r - 6f)
            drawCircle(color = marker, radius = 10f, center = top)
        }
    }
}

private fun DrawScope.tick(c: Offset, r: Float, angleDeg: Float, color: Color, long: Boolean, small: Boolean = false) {
    val rad = Math.toRadians(angleDeg.toDouble() - 90.0)
    val outer = Offset(c.x + r * cos(rad).toFloat(), c.y + r * sin(rad).toFloat())
    val innerLen = if (long) r * 0.80f else if (small) r * 0.93f else r * 0.88f
    val inner = Offset(c.x + innerLen * cos(rad).toFloat(), c.y + innerLen * sin(rad).toFloat())
    drawLine(color, inner, outer, strokeWidth = if (long) 6f else 3f)
}

/** سهمٌ ممتلئ من المركز نحو زاوية القبلة (رمز اتّجاه الكعبة). */
private fun DrawScope.qiblaMark(c: Offset, r: Float, angleDeg: Float, color: Color) {
    val rad = Math.toRadians(angleDeg.toDouble() - 90.0)
    val tip = Offset(c.x + r * 0.78f * cos(rad).toFloat(), c.y + r * 0.78f * sin(rad).toFloat())
    val backRad1 = Math.toRadians(angleDeg.toDouble() - 90.0 + 150.0)
    val backRad2 = Math.toRadians(angleDeg.toDouble() - 90.0 - 150.0)
    val w = r * 0.16f
    val b1 = Offset(tip.x + w * cos(backRad1).toFloat(), tip.y + w * sin(backRad1).toFloat())
    val b2 = Offset(tip.x + w * cos(backRad2).toFloat(), tip.y + w * sin(backRad2).toFloat())
    val path = Path().apply { moveTo(tip.x, tip.y); lineTo(b1.x, b1.y); lineTo(b2.x, b2.y); close() }
    drawPath(path, color)
    drawLine(color, c, tip, strokeWidth = 8f)
}

@Composable
private fun InfoLine(label: String, value: String) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth(),
        Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun HintCard(text: String) {
    Card(Modifier.fillMaxWidth()) {
        Text(
            text,
            Modifier.padding(20.dp).fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}
