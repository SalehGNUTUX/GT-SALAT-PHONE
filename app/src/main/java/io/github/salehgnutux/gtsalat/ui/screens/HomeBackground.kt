package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.salehgnutux.gtsalat.domain.HomeBackgrounds
import io.github.salehgnutux.gtsalat.ui.theme.GlassStrongDark
import io.github.salehgnutux.gtsalat.ui.theme.GlassStrongLight
import io.github.salehgnutux.gtsalat.ui.theme.LocalGlassTheme

/** هل تعرض الرئيسيّة صورةً خلفها؟ تقرؤه بطاقاتها لتصير شبه شفّافة. */
val LocalHomeBackdropOn = compositionLocalOf { false }

/*
 * القيم محسوبةٌ لأسوأ حالة (بكسلٌ أبيض في الداكن وأسود في الفاتح): يبقى النصّ الأساسيّ داخل
 * البطاقات بتباينٍ لا يقلّ عن 6.7:1 في كلّ الدرجات. لا تخفّض شفافيّة البطاقات دون إعادة الحساب.
 */
private const val CARD_ALPHA_DARK = 0.78f
private const val CARD_ALPHA_LIGHT = 0.72f
private const val HERO_ALPHA_DARK = 0.86f
private const val HERO_ALPHA_LIGHT = 0.80f
// غشاوة الصورة المموَّهة خلف الشاشات غير الرئيسيّة (تقع نصوصٌ عليها مباشرةً، فهي أكثف).
private const val BLURRED_SCRIM_DARK = 0.62f
private const val BLURRED_SCRIM_LIGHT = 0.72f

/** نسخةٌ مموَّهةٌ محضَّرةٌ من الصورة ترسمها بطاقات الرئيسيّة خلفها، وغشاوتها. */
class FrostedBackdrop(val bitmap: androidx.compose.ui.graphics.ImageBitmap, val scrim: Color)

val LocalFrostedBackdrop = compositionLocalOf<FrostedBackdrop?> { null }

/** يحمّل النسخة المموَّهة للصورة خارج الخيط الرئيسيّ. */
@Composable
fun rememberBlurredBitmap(id: String?): androidx.compose.ui.graphics.ImageBitmap? {
    val context = LocalContext.current
    val state = androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, id) {
        value = if (id == null) null else kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                context.assets.open("backgrounds/blur/$id.webp").use {
                    android.graphics.BitmapFactory.decodeStream(it)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    return state.value
}

/**
 * زجاجٌ حقيقيّ بكلفةٍ شبه معدومة: يرسم من النسخة المموَّهة الجزءَ الواقع خلف العنصر تماماً، ويتابع موضعه أثناء
 * التمرير في طور الرسم فقط (بلا إعادة تركيب). يعمل على كلّ الإصدارات لأنّ الخلفيّة ثابتة فلا حاجة لتمويهٍ حيّ.
 */
@Composable
fun Modifier.frostedGlass(shape: androidx.compose.ui.graphics.Shape): Modifier {
    val backdrop = LocalFrostedBackdrop.current ?: return this
    val position = remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val rootSize = remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    return this
        .onGloballyPositioned { c ->
            position.value = c.findRootCoordinates().localPositionOf(c, androidx.compose.ui.geometry.Offset.Zero)
            rootSize.value = c.findRootCoordinates().size
        }
        .drawWithCache {
            val path = androidx.compose.ui.graphics.Path().apply {
                addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache))
            }
            onDrawBehind {
                val root = rootSize.value
                if (root.width == 0 || root.height == 0) return@onDrawBehind
                val bw = backdrop.bitmap.width
                val bh = backdrop.bitmap.height
                // القصّ نفسه الذي تعرض به الصورة الحادّة (Crop في الوسط)، ليتطابق ما خلف الزجاج مع ما حوله.
                val scale = maxOf(root.width.toFloat() / bw, root.height.toFloat() / bh)
                val cw = (root.width / scale).toInt().coerceIn(1, bw)
                val ch = (root.height / scale).toInt().coerceIn(1, bh)
                clipPath(path) {
                    translate(-position.value.x, -position.value.y) {
                        drawImage(
                            backdrop.bitmap,
                            srcOffset = androidx.compose.ui.unit.IntOffset((bw - cw) / 2, (bh - ch) / 2),
                            srcSize = androidx.compose.ui.unit.IntSize(cw, ch),
                            dstSize = root,
                            filterQuality = androidx.compose.ui.graphics.FilterQuality.Low,
                        )
                    }
                    drawRect(backdrop.scrim)
                }
            }
        }
}

@Composable
private fun isDarkScheme(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

/** شفافيّة بطاقة البطل (الساعة والصلاة القادمة) فوق الصورة. */
@Composable
fun homeHeroAlpha(): Float = when {
    LocalGlassTheme.current -> 1f   // حشوة الزجاج القويّة تحمل شفافيّتها في لونها
    !LocalHomeBackdropOn.current -> 1f
    isDarkScheme() -> HERO_ALPHA_DARK
    else -> HERO_ALPHA_LIGHT
}

/** حشوة بطاقة البطل في السمة الزجاجيّة. */
@Composable
fun glassStrongFill(): Color = if (isDarkScheme()) GlassStrongDark else GlassStrongLight

/** هل تُغلَّف عناصر الرئيسيّة المكشوفة (الترويسة والتذييل) بطبقة زجاج؟ */
@Composable
fun homeElementsNeedFill(): Boolean = LocalGlassTheme.current || LocalHomeBackdropOn.current

/** لون بطاقات الرئيسيّة: شبه شفّافٍ فوق الصورة، والافتراضيّ بدونها. */
@Composable
fun homeCardColor(): Color {
    val base = CardDefaults.cardColors().containerColor
    if (LocalGlassTheme.current || !LocalHomeBackdropOn.current) return base
    return base.copy(alpha = if (isDarkScheme()) CARD_ALPHA_DARK else CARD_ALPHA_LIGHT)
}

/** غشاوة الصورة في الرئيسيّة عند الدرجة المختارة؛ يتشاركها المشهد والزجاج ليتطابقا. */
@Composable
fun homeScrimTop(level: Int): Float {
    val dark = isDarkScheme()
    return when (level) {
        HomeBackgrounds.LEVEL_CALM -> if (dark) 0.62f else 0.84f
        HomeBackgrounds.LEVEL_VIVID -> if (dark) 0.32f else 0.64f
        else -> if (dark) 0.46f else 0.72f
    }
}

/** خلفيّة الشاشات غير الرئيسيّة في السمة الزجاجيّة: الصورة نفسها مموَّهةً بشدّة. */
@Composable
fun BlurredBackdrop(id: String, modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.background
    val alpha = if (isDarkScheme()) BLURRED_SCRIM_DARK else BLURRED_SCRIM_LIGHT
    Box(modifier.background(base)) {
        AsyncImage(
            model = "file:///android_asset/backgrounds/blur/$id.webp",
            contentDescription = null,
            contentScale = ContentScale.Crop,
            filterQuality = androidx.compose.ui.graphics.FilterQuality.Low,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(base.copy(alpha = alpha)))
    }
}

/** طبقة الصورة وغشاوتها: أخفّ في الأعلى والوسط، وأكثف أسفلاً حيث تتراصّ البطاقات والشريط السفليّ. */
@Composable
fun HomeBackdrop(id: String, level: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val base = MaterialTheme.colorScheme.background
    val top = homeScrimTop(level)
    val scrim = remember(base, top) {
        Brush.verticalGradient(
            0f to base.copy(alpha = top),
            0.55f to base.copy(alpha = top),
            1f to base.copy(alpha = (top + 0.28f).coerceAtMost(0.94f)),
        )
    }
    Box(modifier.background(base)) {
        val request = remember(id) {
            ImageRequest.Builder(context)
                .data("file:///android_asset/backgrounds/$id.webp")
                .crossfade(350)
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(scrim))
    }
}

/** قسم «خلفيّة الرئيسيّة» في الإعدادات: تفعيلٌ واختيارٌ ودرجة ظهورٍ ومعاينةٌ حيّة. */
@Composable
fun HomeBackgroundSettings(
    enabled: Boolean,
    selectedId: String,
    level: Int,
    onEnabled: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    onLevel: (Int) -> Unit,
    switchRow: @Composable (label: String, checked: Boolean, onChange: (Boolean) -> Unit) -> Unit,
) {
    var fullPreview by remember { mutableStateOf(false) }
    val selected = HomeBackgrounds.byId(selectedId)
    val dark = isDarkScheme()
    val shownId = HomeBackgrounds.resolve(selected.id, dark)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        switchRow("عرض صورة خلف الرئيسيّة", enabled, onEnabled)

        // معاينةٌ بنموذجٍ مصغّرٍ للرئيسيّة نفسها، بالغشاوة والبطاقات الفعليّة.
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .width(150.dp)
                    .height(320.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(20.dp))
                    .clickable { fullPreview = true },
            ) { HomePreviewMock(enabled, shownId, level, compact = true) }
            Text(
                if (enabled) "${selected.title} — ${selected.place}" else "الخلفيّة معطَّلة: تظهر الرئيسيّة بالتدرّج اللونيّ",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            FilledTonalButton(
                onClick = { fullPreview = true },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Icon(Icons.Outlined.Fullscreen, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("معاينة بملء الشاشة", modifier = Modifier.padding(start = 6.dp))
            }
        }

        Text(
            "اختر صورة",
            style = MaterialTheme.typography.bodyLarge,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
            items(HomeBackgrounds.all, key = { it.id }) { bg ->
                BackgroundThumb(
                    id = HomeBackgrounds.resolve(bg.id, dark),
                    title = bg.title,
                    selected = enabled && bg.id == selectedId,
                    dimmed = !enabled,
                ) { onSelect(bg.id) }
            }
        }

        if (enabled) {
            Text("درجة ظهور الصورة", style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(level == HomeBackgrounds.LEVEL_CALM, { onLevel(HomeBackgrounds.LEVEL_CALM) }, { Text("هادئة") })
                FilterChip(level == HomeBackgrounds.LEVEL_BALANCED, { onLevel(HomeBackgrounds.LEVEL_BALANCED) }, { Text("متوازنة") })
                FilterChip(level == HomeBackgrounds.LEVEL_VIVID, { onLevel(HomeBackgrounds.LEVEL_VIVID) }, { Text("واضحة") })
            }
        }
    }

    if (fullPreview) {
        Dialog(
            onDismissRequest = { fullPreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            Box(Modifier.fillMaxSize()) {
                HomePreviewMock(enabled, shownId, level, compact = false)
                IconButton(
                    onClick = { fullPreview = false },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(12.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), CircleShape),
                ) { Icon(Icons.Outlined.Close, contentDescription = "إغلاق المعاينة") }
            }
        }
    }
}

@Composable
private fun BackgroundThumb(id: String, title: String, selected: Boolean, dimmed: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.width(84.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier
                .width(84.dp)
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    BorderStroke(if (selected) 3.dp else 1.dp, if (selected) cs.primary else cs.outlineVariant),
                    RoundedCornerShape(12.dp),
                )
                .clickable(onClick = onClick),
        ) {
            AsyncImage(
                model = "file:///android_asset/backgrounds/thumb/$id.webp",
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(if (dimmed) 0.55f else 1f),
            )
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "مختارة",
                    tint = cs.onPrimary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(22.dp)
                        .background(cs.primary, CircleShape)
                        .padding(3.dp),
                )
            }
        }
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** نموذجٌ لشكل الرئيسيّة بالقيم نفسها، ليرى المستخدم تأثير الصورة على البطاقات قبل الخروج من الإعدادات. */
@Composable
private fun BoxScope.HomePreviewMock(enabled: Boolean, id: String, level: Int, compact: Boolean) {
    val cs = MaterialTheme.colorScheme
    if (enabled) {
        HomeBackdrop(id, level, Modifier.matchParentSize())
    } else {
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(listOf(cs.background, lerp(cs.background, cs.primary, 0.12f))),
            ),
        )
    }
    val pad: Dp = if (compact) 8.dp else 16.dp
    androidx.compose.runtime.CompositionLocalProvider(LocalHomeBackdropOn provides enabled) {
        val cardColor = homeCardColor()
        val heroAlpha = homeHeroAlpha()
        Column(
            Modifier.matchParentSize().then(if (compact) Modifier else Modifier.statusBarsPadding()).padding(pad),
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 14.dp),
        ) {
            if (!compact) Box(Modifier.height(52.dp))
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(if (compact) 12.dp else 22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (LocalGlassTheme.current) glassStrongFill() else cs.primaryContainer.copy(alpha = heroAlpha),
                ),
            ) {
                Column(Modifier.padding(if (compact) 8.dp else 20.dp)) {
                    Text(
                        "الصلاة القادمة",
                        style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge,
                        color = cs.onPrimaryContainer,
                    )
                    Text(
                        "المغرب",
                        style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = cs.onPrimaryContainer,
                    )
                    Text(
                        "18:42",
                        style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.headlineSmall,
                        color = cs.onPrimaryContainer,
                    )
                }
            }
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardColor)) {
                Column(Modifier.padding(if (compact) 8.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 8.dp)) {
                    Text(
                        "مواقيت اليوم والليلة",
                        style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.primary,
                    )
                    listOf("الفجر" to "05:25", "الظهر" to "12:48", "العصر" to "15:52").forEach { (name, time) ->
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            val style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyLarge
                            Text(name, style = style)
                            Text(time, style = style)
                        }
                    }
                }
            }
        }
    }
}
