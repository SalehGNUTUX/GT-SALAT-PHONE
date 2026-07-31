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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.salehgnutux.gtsalat.util.Format
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val GOLD = Color(0xFFD4AF37)

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
                if (ui.needsCalibration) {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    ) {
                        Text(
                            "⚠️ دقّة البوصلة منخفضة — عايِر الجهاز: حرّكه في الهواء على شكل الرقم 8 عدّة مرّات، بعيداً عن المعادن.",
                            Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                CompassDial(
                    deviceBearing = ui.deviceBearing,
                    qiblaBearing = ui.qiblaBearing,
                    aligned = ui.aligned,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )
                Text(
                    if (ui.aligned) "أنت تواجه القبلة ✓" else "أدِر الجهاز حتى تعلو الكعبةُ للمؤشّر",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
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
    val measurer = rememberTextMeasurer()
    val ring = MaterialTheme.colorScheme.outlineVariant
    val tickColor = MaterialTheme.colorScheme.outline
    val minorTick = MaterialTheme.colorScheme.outlineVariant
    val cardinalColor = MaterialTheme.colorScheme.onSurface
    val northColor = MaterialTheme.colorScheme.error
    val qiblaColor = if (aligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val needleColor = MaterialTheme.colorScheme.primary
    val hubColor = MaterialTheme.colorScheme.surfaceVariant
    val cubeFill = if (isDark()) Color(0xFF20242A) else Color(0xFF15181C)
    val surface = MaterialTheme.colorScheme.surface

    // زوايا الجهات على الشاشة (0° = أعلى) = الاتّجاه الحقيقيّ ناقص اتّجاه الجهاز.
    fun screenAngle(trueBearing: Float) = trueBearing - deviceBearing
    val qiblaScreen = screenAngle(qiblaBearing)

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = min(size.width, size.height) / 2f * 0.88f

            // حلقتان خارجيّتان أنيقتان
            drawCircle(ring, r, c, style = Stroke(width = 3f))
            drawCircle(minorTick, r * 0.995f, c, style = Stroke(width = 1f))

            // شُرَط كلّ 15°: كبيرة عند الجهات الأصليّة
            var a = 0
            while (a < 360) {
                val major = a % 90 == 0
                val mid = a % 45 == 0
                val sa = screenAngle(a.toFloat())
                val outer = pointAt(c, r, sa)
                val innerLen = when { major -> r * 0.86f; mid -> r * 0.90f; else -> r * 0.93f }
                val inner = pointAt(c, r * (innerLen / r), sa)
                drawLine(
                    color = if (major) tickColor else minorTick,
                    start = inner, end = outer,
                    strokeWidth = if (major) 5f else 2.5f,
                    cap = StrokeCap.Round,
                )
                a += 15
            }

            // أسماء الجهات، مرسومةٌ قائمةً (لا تنقلب) على مواضعها الحقيقيّة
            cardinal(measurer, c, r * 0.74f, screenAngle(0f), "شمال", northColor)
            cardinal(measurer, c, r * 0.74f, screenAngle(90f), "شرق", cardinalColor)
            cardinal(measurer, c, r * 0.74f, screenAngle(180f), "جنوب", cardinalColor)
            cardinal(measurer, c, r * 0.74f, screenAngle(270f), "غرب", cardinalColor)

            // إبرة القبلة من المركز نحو اتّجاهها على الشاشة
            val tip = pointAt(c, r * 0.60f, qiblaScreen)
            needle(c, tip, qiblaScreen, needleColor)

            // محور الإبرة
            drawCircle(hubColor, r * 0.055f, c)
            drawCircle(needleColor, r * 0.055f, c, style = Stroke(width = 3f))

            // رمز الكعبة عند نقطة القبلة على الحافة (أسفل المؤشّر مباشرةً عند المحاذاة)
            kaaba(pointAt(c, r * 0.86f, qiblaScreen), r * 0.13f, cubeFill, GOLD, surface)

            // المؤشّر الثابت أعلى القرص: عندما تعلو الكعبةُ إليه تكون مواجهاً للقبلة
            topPointer(c, r, qiblaColor)
        }
    }
}

/** نقطة على مسافة radius بزاوية screenAngle حيث 0° للأعلى. */
private fun pointAt(c: Offset, radius: Float, screenAngle: Float): Offset {
    val rad = Math.toRadians(screenAngle.toDouble() - 90.0)
    return Offset(c.x + radius * cos(rad).toFloat(), c.y + radius * sin(rad).toFloat())
}

private fun DrawScope.needle(c: Offset, tip: Offset, screenAngle: Float, color: Color) {
    // مثلّثٌ نحيلٌ متّسقٌ مع اتّجاه القبلة، قاعدته قرب المركز.
    val baseW = 14f
    val leftBase = pointAt(c, baseW, screenAngle - 90f)
    val rightBase = pointAt(c, baseW, screenAngle + 90f)
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(leftBase.x, leftBase.y)
        lineTo(rightBase.x, rightBase.y)
        close()
    }
    drawPath(path, color)
}

/** مؤشّرٌ ثابتٌ (مثلّثٌ صغير) أعلى القرص بالضبط عند 12. */
private fun DrawScope.topPointer(c: Offset, r: Float, color: Color) {
    val top = Offset(c.x, c.y - r - 2f)
    val w = r * 0.06f
    val h = r * 0.11f
    val path = Path().apply {
        moveTo(top.x, top.y + h)          // رأسٌ للأسفل نحو القرص
        lineTo(top.x - w, top.y - h * 0.2f)
        lineTo(top.x + w, top.y - h * 0.2f)
        close()
    }
    drawPath(path, color)
    drawCircle(color, r * 0.028f, Offset(c.x, c.y - r - h * 0.6f))
}

/** رمز الكعبة: مكعّبٌ داكنٌ بحزامٍ ذهبيّ وبابٍ صغير، بإطارٍ خفيف ليظهر على أيّ خلفيّة. */
private fun DrawScope.kaaba(center: Offset, s: Float, fill: Color, gold: Color, stroke: Color) {
    val tl = Offset(center.x - s / 2f, center.y - s / 2f)
    drawRect(fill, tl, Size(s, s))
    drawRect(stroke, tl, Size(s, s), style = Stroke(width = 2f))
    // حزام الكسوة الذهبيّ
    drawRect(gold, Offset(tl.x, tl.y + s * 0.30f), Size(s, s * 0.13f))
    // الباب
    drawRect(gold, Offset(center.x - s * 0.12f, tl.y + s * 0.55f), Size(s * 0.24f, s * 0.40f))
}

private fun DrawScope.cardinal(
    measurer: TextMeasurer,
    c: Offset,
    radius: Float,
    screenAngle: Float,
    label: String,
    color: Color,
) {
    val pos = pointAt(c, radius, screenAngle)
    val layout = measurer.measure(
        label,
        style = TextStyle(color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold),
    )
    drawText(
        layout,
        topLeft = Offset(pos.x - layout.size.width / 2f, pos.y - layout.size.height / 2f),
    )
}

@Composable
private fun isDark(): Boolean = MaterialTheme.colorScheme.surface.luminanceIsDark()

private fun Color.luminanceIsDark(): Boolean =
    (0.299f * red + 0.587f * green + 0.114f * blue) < 0.5f

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
