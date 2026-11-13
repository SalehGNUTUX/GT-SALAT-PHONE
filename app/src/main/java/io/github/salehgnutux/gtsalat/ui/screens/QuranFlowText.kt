package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.salehgnutux.gtsalat.domain.QuranAyah
import io.github.salehgnutux.gtsalat.domain.QuranText
import io.github.salehgnutux.gtsalat.ui.theme.AmiriQuran
import io.github.salehgnutux.gtsalat.ui.theme.GoldAccent
import io.github.salehgnutux.gtsalat.ui.theme.GoldAccentDark
import io.github.salehgnutux.gtsalat.ui.theme.quranFontFamily

/** مقاس نصّ القرآن لكلّ خطّ: الخطّان المغربيّ والعثمانيّ أدقّ من أميري فيُكبَّران ولا يُثخَّنان. */
data class QuranTextStyle(val family: FontFamily, val size: TextUnit, val lineHeight: TextUnit, val weight: FontWeight)

fun quranTextStyle(font: String?): QuranTextStyle = when (font) {
    "Maghribi" -> QuranTextStyle(quranFontFamily(font), 26.sp, 56.sp, FontWeight.Normal)
    "UthmanicHafs" -> QuranTextStyle(quranFontFamily(font), 25.sp, 54.sp, FontWeight.Normal)
    else -> QuranTextStyle(AmiriQuran, 23.sp, 48.sp, FontWeight.Bold)
}

/** الذهبيّ المناسب لسِمة التطبيق الفعليّة (لا لسِمة النظام). */
@Composable
fun quranGold(): Color = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) GoldAccentDark else GoldAccent

/**
 * يُلحق نصّ آيةٍ مهيّأً لخطّه. في الخطّ المغربيّ تُرسَم علامة الوقف «صه» صغيرةً مرفوعةً بعد الكلمة،
 * لأنّ رمزها في الخطّ يشبه الشدّة؛ وفي غيره تبقى العلامة كما هي في النصّ.
 */
fun AnnotatedString.Builder.appendQuran(text: String, font: String?, accent: Color) {
    val shown = QuranText.forFont(text, font.orEmpty())
    if (font != "Maghribi" || QuranText.WAQF !in shown) { append(shown); return }
    shown.split(QuranText.WAQF).forEachIndexed { i, part ->
        if (i > 0) {
            append(' ')
            withStyle(SpanStyle(color = accent, fontSize = 0.42.em, baselineShift = BaselineShift(0.9f))) { append("صه") }
        }
        append(part)
    }
}

/** نصّ آيةٍ واحدةٍ للعرض (عرض البطاقات ونتائج البحث). */
fun quranAnnotated(text: String, font: String?, accent: Color): AnnotatedString =
    buildAnnotatedString { appendQuran(text, font, accent) }

/** صفحةٌ من المصحف المدنيّ: آياتٌ متتابعةٌ في فقرةٍ واحدة. */
data class QuranPageChunk(val page: Int, val ayat: List<QuranAyah>)

/** يقسّم آيات السورة على صفحات المصحف؛ وبلا جدولٍ يقسمها كلّ 10 آيات. */
fun chunkByPage(surah: Int, ayat: List<QuranAyah>, starts: List<List<Int>>): List<QuranPageChunk> {
    if (ayat.isEmpty()) return emptyList()
    if (starts.isEmpty()) return ayat.chunked(10).mapIndexed { i, c -> QuranPageChunk(i + 1, c) }
    val out = mutableListOf<QuranPageChunk>()
    var cur = mutableListOf<QuranAyah>()
    var page = QuranText.pageOf(starts, surah, ayat.first().n)
    for (a in ayat) {
        val p = QuranText.pageOf(starts, surah, a.n)
        if (p != page && cur.isNotEmpty()) { out += QuranPageChunk(page, cur); cur = mutableListOf() }
        page = p; cur += a
    }
    if (cur.isNotEmpty()) out += QuranPageChunk(page, cur)
    return out
}

/** عنوان السورة في العرض المتّصل: الاسم ثمّ البسملة (إلّا الفاتحة، فالبسملة آيتها الأولى، والتوبة). */
@Composable
fun QuranFlowHeader(surahName: String, surahN: Int, bismillah: String, font: String?) {
    val st = quranTextStyle(font)
    val gold = quranGold()
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("سورة $surahName", fontFamily = AmiriQuran, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = gold)
        if (surahN != 1 && surahN != 9 && bismillah.isNotBlank()) {
            Text(
                quranAnnotated(bismillah, font, gold), fontFamily = st.family, fontSize = st.size, lineHeight = st.lineHeight,
                fontWeight = st.weight, color = gold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * صفحةٌ متّصلة: نصّ آياتها متتابعاً بأرقامٍ مزخرفة ﴿n﴾ (أرقامٌ 0-9). النقر على آيةٍ يحدّدها،
 * والضغط المطوّل يبدّل إشارتها المرجعيّة. الآية الجارية والمحدَّدة تُظلَّلان، والمُشار إليها رقمها بلون السِمة.
 * [onLayout] يسلّم تخطيط النصّ وبداية كلّ آيةٍ فيه، ليمرّر القارئ إلى سطر الآية داخل الصفحة.
 */
@Composable
fun QuranFlowPage(
    chunk: QuranPageChunk,
    surahN: Int,
    font: String?,
    playing: Int,
    target: Int,
    bookmarked: Set<Int>,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
    onLayout: (TextLayoutResult, Map<Int, Int>) -> Unit,
) {
    val st = quranTextStyle(font)
    val gold = quranGold()
    val cs = MaterialTheme.colorScheme
    val nums = chunk.ayat.map { it.n }
    // لا يُعاد بناء نصّ الصفحة إلّا إن تغيّر ما يخصّها هي.
    val playingHere = if (playing in nums) playing else 0
    val targetHere = if (playing == 0 && target > 1 && target in nums) target else 0
    val marksHere = bookmarked.filter { it in nums }.toSet()
    val built = remember(chunk, font, playingHere, targetHere, marksHere, gold, cs.primary) {
        val starts = HashMap<Int, Int>()
        val text = buildAnnotatedString {
            chunk.ayat.forEach { a ->
                starts[a.n] = length
                val body = if (a.n == 1 && surahN != 1) QuranText.stripBismillah(a.text) else a.text
                val bg = when (a.n) {
                    playingHere -> cs.primaryContainer
                    targetHere -> cs.tertiaryContainer
                    else -> null
                }
                pushStringAnnotation(AYAH_TAG, a.n.toString())
                if (bg != null) withStyle(SpanStyle(background = bg)) { appendQuran(body, font, gold) } else appendQuran(body, font, gold)
                append(' ')
                val marked = a.n in marksHere
                withStyle(
                    SpanStyle(
                        color = if (marked) cs.primary else gold, fontFamily = AmiriQuran,
                        fontWeight = if (marked) FontWeight.Bold else FontWeight.Normal, fontSize = 0.8.em,
                    )
                ) { append("﴿${a.n}﴾") }
                pop()
                append(' ')
            }
        }
        text to starts
    }
    val (text, starts) = built
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    fun ayahAt(pos: androidx.compose.ui.geometry.Offset): Int? = layout?.let { l ->
        val off = l.getOffsetForPosition(pos)
        text.getStringAnnotations(AYAH_TAG, off, off).firstOrNull()?.item?.toIntOrNull()
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text,
                fontFamily = st.family, fontSize = st.size, lineHeight = st.lineHeight, fontWeight = st.weight,
                color = cs.onSurface, textAlign = TextAlign.Justify,
                modifier = Modifier.fillMaxWidth().pointerInput(text) {
                    detectTapGestures(
                        onTap = { p -> ayahAt(p)?.let(onTap) },
                        onLongPress = { p -> ayahAt(p)?.let(onLongPress) },
                    )
                },
                onTextLayout = { layout = it; onLayout(it, starts) },
            )
            Text(
                "${chunk.page}", style = MaterialTheme.typography.labelSmall, color = cs.outline,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

private const val AYAH_TAG = "ayah"
