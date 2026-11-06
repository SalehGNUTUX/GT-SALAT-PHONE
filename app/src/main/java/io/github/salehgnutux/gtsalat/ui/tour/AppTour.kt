package io.github.salehgnutux.gtsalat.ui.tour

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.salehgnutux.gtsalat.ui.animationsEnabled

/** ما توضّحه الحركة على العنصر: نقرة، أو ضغطة مطوّلة. */
enum class TourGesture { NONE, TAP, HOLD }

data class TourStep(
    val target: String?,        // null = بطاقةٌ وسط الشاشة بلا عنصر
    val title: String,
    val body: String,
    val gesture: TourGesture = TourGesture.TAP,
    val listItem: Int = 0,      // عنصر الرئيسيّة الذي يُمرَّر إليه ليظهر الهدف
)

object TourKeys {
    const val SETTINGS = "settings"
    const val SHORTCUT = "shortcut"
    const val THEME = "theme"
    const val LOCATION = "location"
    const val NEXT = "next"
    const val DAILY = "daily"
    const val PRAYERS = "prayers"
    const val TAB_PREFIX = "tab_"
}

val TourSteps: List<TourStep> = listOf(
    TourStep(null, "أهلاً بك في GT-SALAT", "جولةٌ قصيرةٌ تعرّفك على عناصر الواجهة. يمكنك إعادتها متى شئت من الإعدادات ← «حول».", TourGesture.NONE),
    TourStep(TourKeys.SETTINGS, "الإعدادات", "منها تضبط الأذان والتنبيهات وخلفيّة الرئيسيّة والمظهر."),
    TourStep(TourKeys.SHORTCUT, "اختصارك السريع", "انقر لفتح القسم مباشرةً، واضغط مطوّلاً لتغييره إلى القرآن الكريم أو الرقية الشرعية أو أيّ قسمٍ آخر.", TourGesture.HOLD),
    TourStep(TourKeys.THEME, "الوضع الداكن والفاتح", "بدّل بينهما بنقرة، وبجانبه زرّ نظام الوقت 12 أو 24 ساعة."),
    TourStep(TourKeys.LOCATION, "موقعك", "انقر زرّ السهمين لتحديث موقعك، فتُحسَب المواقيت لمكانك الحاليّ."),
    TourStep(TourKeys.NEXT, "الصلاة القادمة", "اسمها ووقتها، وحلقةٌ تمتلئ كلّما اقترب الوقت مع عدٍّ تنازليّ.", TourGesture.NONE),
    TourStep(TourKeys.DAILY, "زادك اليوميّ", "أزرارٌ تنقلك إلى ذكر اليوم وحكمته وآيته أسفل الصفحة.", listItem = 1),
    TourStep(TourKeys.PRAYERS, "مواقيت اليوم والليلة", "كلّ الصلوات في بطاقةٍ واحدة، والقادمة منها مميَّزة.", TourGesture.NONE, listItem = 2),
    TourStep(TourKeys.TAB_PREFIX + "timetable", "جدول المواقيت", "مواقيت الشهر كاملاً بالتاريخين الهجريّ والميلاديّ."),
    TourStep(TourKeys.TAB_PREFIX + "qibla", "القبلة", "بوصلةٌ حيّةٌ تدلّك على اتّجاه القبلة."),
    TourStep(TourKeys.TAB_PREFIX + "more_graph", "المزيد", "القرآن الكريم والأذكار وحصن المسلم والرقية والإذاعات وغيرها. والنجمة ترفع ما تحبّ إلى الأعلى."),
)

class TourController {
    val targets = mutableStateMapOf<String, Rect>()
    var stepIndex by mutableIntStateOf(0)
    var active by androidx.compose.runtime.mutableStateOf(false)
}

val LocalTour = staticCompositionLocalOf<TourController?> { null }

/** يسجّل موضع العنصر ليُبرزه الشرح. */
@Composable
fun Modifier.tourTarget(key: String): Modifier {
    val tour = LocalTour.current ?: return this
    return this.onGloballyPositioned { tour.targets[key] = it.boundsInRoot() }
}

/** طبقة الشرح الحركيّ: تعتّم الواجهة الحقيقيّة وتفتح نافذةً متحرّكةً على العنصر المشروح. */
@Composable
fun AppTourOverlay(tour: TourController, onFinish: () -> Unit) {
    val context = LocalContext.current
    val motion = remember { animationsEnabled(context) }
    val step = TourSteps[tour.stepIndex]
    val rect = step.target?.let { tour.targets[it] }
    val density = LocalDensity.current
    val inflate = with(density) { 8.dp.toPx() }
    val corner = with(density) { 18.dp.toPx() }

    BackHandler { if (tour.stepIndex > 0) tour.stepIndex-- else onFinish() }

    val dur = if (motion) 420 else 0
    val spec = tween<Float>(dur, easing = FastOutSlowInEasing)
    val left by animateFloatAsState((rect?.left ?: 0f) - inflate, spec, label = "l")
    val top by animateFloatAsState((rect?.top ?: 0f) - inflate, spec, label = "t")
    val right by animateFloatAsState((rect?.right ?: 0f) + inflate, spec, label = "r")
    val bottom by animateFloatAsState((rect?.bottom ?: 0f) + inflate, spec, label = "b")
    val holeAlpha by animateFloatAsState(if (rect != null) 1f else 0f, tween(if (motion) 250 else 0), label = "h")

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by pulseTransition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "p",
    )
    val shownPulse = if (motion) pulse else 0.35f
    val ringColor = MaterialTheme.colorScheme.primary

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            // يبتلع اللمس فلا تُنقر عناصر الواجهة خلف الشرح.
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(Color(0xCC050A08))
            if (holeAlpha > 0f) {
                val tl = Offset(left, top)
                val sz = Size(right - left, bottom - top)
                drawRoundRect(Color.Black.copy(alpha = holeAlpha), tl, sz, CornerRadius(corner), blendMode = BlendMode.Clear)
                // حلقةٌ نابضةٌ تتّسع حول العنصر.
                val grow = 22.dp.toPx() * shownPulse
                drawRoundRect(
                    ringColor.copy(alpha = (1f - shownPulse) * holeAlpha),
                    Offset(tl.x - grow, tl.y - grow),
                    Size(sz.width + grow * 2, sz.height + grow * 2),
                    CornerRadius(corner + grow),
                    style = Stroke(3.dp.toPx()),
                )
            }
        }

        if (rect != null && step.gesture != TourGesture.NONE) {
            // إحداثيّات العنصر مطلقة، فتُرسَم اليد في طبقة LTR وإلّا انعكس موضعها أفقيّاً في الواجهة العربيّة.
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr,
            ) {
                Box(Modifier.fillMaxSize()) { GestureHand(rect, step.gesture, tour.stepIndex, motion) }
            }
        }

        val screenH = with(density) { maxHeight.toPx() }
        val cardAtTop = rect != null && rect.center.y > screenH * 0.5f
        Box(
            Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
            contentAlignment = when {
                rect == null -> Alignment.Center
                cardAtTop -> Alignment.TopCenter
                else -> Alignment.BottomCenter
            },
        ) {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AnimatedContent(
                        targetState = tour.stepIndex,
                        transitionSpec = {
                            if (motion) {
                                (fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 6 }) togetherWith fadeOut(tween(140))
                            } else {
                                fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                            }
                        },
                        label = "tourText",
                    ) { i ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                TourSteps[i].title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(TourSteps[i].body, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${tour.stepIndex + 1} / ${TourSteps.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.weight(1f))
                        val last = tour.stepIndex == TourSteps.lastIndex
                        if (!last) TextButton(onClick = onFinish) { Text("تخطّي") }
                        if (tour.stepIndex > 0) TextButton(onClick = { tour.stepIndex-- }) { Text("السابق") }
                        Button(onClick = { if (last) onFinish() else tour.stepIndex++ }) {
                            Text(if (last) "ابدأ" else "التالي")
                        }
                    }
                }
            }
        }
    }
}

/** يدٌ متحرّكةٌ تمثّل الإيماءة: تهبط للنقر، وتثبت مع حلقةٍ تمتلئ للضغطة المطوّلة. */
@Composable
private fun GestureHand(rect: Rect, gesture: TourGesture, stepKey: Int, motion: Boolean) {
    val progress = remember(stepKey) { Animatable(0f) }
    LaunchedEffect(stepKey, motion) {
        if (!motion) { progress.snapTo(1f); return@LaunchedEffect }
        kotlinx.coroutines.delay(420)
        while (true) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(if (gesture == TourGesture.HOLD) 1400 else 700, easing = FastOutSlowInEasing))
            kotlinx.coroutines.delay(600)
        }
    }
    val handColor = Color.White
    val fill = MaterialTheme.colorScheme.primary
    val handPx = with(LocalDensity.current) { 44.dp.toPx() }
    val p = progress.value
    val press = if (p < 0.35f) p / 0.35f else 1f    // هبوط اليد
    val dropPx = with(LocalDensity.current) { 16.dp.toPx() } * (1f - press)
    Box(
        Modifier
            .offset { IntOffset((rect.center.x - handPx / 2).toInt(), (rect.center.y - handPx * 0.15f + dropPx).toInt()) }
            .size(44.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (gesture == TourGesture.HOLD) {
            Canvas(Modifier.fillMaxSize()) {
                drawArc(
                    fill,
                    startAngle = -90f,
                    sweepAngle = 360f * ((p - 0.3f) / 0.7f).coerceIn(0f, 1f),
                    useCenter = false,
                    style = Stroke(4.dp.toPx()),
                )
            }
        }
        Icon(
            Icons.Outlined.TouchApp,
            contentDescription = null,
            tint = handColor,
            modifier = Modifier.size(30.dp).scale(if (press >= 1f) 0.9f else 1f),
        )
    }
}
