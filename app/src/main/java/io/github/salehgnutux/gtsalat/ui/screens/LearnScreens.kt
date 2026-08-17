package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.CleanHands
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.salehgnutux.gtsalat.data.ContentRepository
import io.github.salehgnutux.gtsalat.domain.LearnFile
import io.github.salehgnutux.gtsalat.domain.LearnRulingGroup
import io.github.salehgnutux.gtsalat.domain.LearnSection
import io.github.salehgnutux.gtsalat.domain.LearnSource
import io.github.salehgnutux.gtsalat.domain.LearnStep
import io.github.salehgnutux.gtsalat.domain.RuqyahFile
import io.github.salehgnutux.gtsalat.domain.RuqyahSection
import io.github.salehgnutux.gtsalat.ui.theme.AmiriQuran
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** مقطعُ رقيةٍ جاهزٌ للعرض (نصّه من مصحف التطبيق للقرآن، أو المضمَّن للدعاء). */
data class RuqyahItem(
    val label: String, val kind: String, val text: String,
    val surah: Int, val ayahFrom: Int, val ayahTo: Int,
    val source: LearnSource?, val note: String,
)

@HiltViewModel
class LearnViewModel @Inject constructor(
    private val content: ContentRepository,
) : ViewModel() {
    private val _purity = MutableStateFlow(LearnFile())
    val purity: StateFlow<LearnFile> = _purity.asStateFlow()
    private val _ruqyah = MutableStateFlow(RuqyahFile())
    val ruqyah: StateFlow<RuqyahFile> = _ruqyah.asStateFlow()

    init {
        viewModelScope.launch { _purity.value = content.puritySalah() }
        viewModelScope.launch { _ruqyah.value = content.ruqyah() }
    }

    fun learnSection(id: String): LearnSection? = _purity.value.sections.firstOrNull { it.id == id }
    fun ruqyahSection(id: String): RuqyahSection? = _ruqyah.value.sections.firstOrNull { it.id == id }

    /** مقاطعُ قسمِ رقيةٍ مع نصوصها (قرآنٌ من المصحف المُدمَج، دعاءٌ من النصّ المضمَّن). */
    suspend fun ruqyahItems(section: RuqyahSection): List<RuqyahItem> = section.segments.map { seg ->
        val text = if (seg.kind == "quran" && seg.surah > 0)
            content.ayatText(seg.surah, seg.ayahFrom, seg.ayahTo).joinToString("  ") { it.second }
        else seg.text
        RuqyahItem(seg.label, seg.kind, text, seg.surah, seg.ayahFrom, seg.ayahTo, seg.source, seg.note)
    }

    /** قائمةُ تشغيلٍ من الآيات (السور/الآيات/العناوين متوازية) لقسمِ رقية. */
    fun ruqyahPlaylist(section: RuqyahSection): Triple<IntArray, IntArray, Array<String>> {
        val s = ArrayList<Int>(); val a = ArrayList<Int>(); val l = ArrayList<String>()
        section.segments.filter { it.kind == "quran" && it.surah > 0 }.forEach { seg ->
            for (ay in seg.ayahFrom..seg.ayahTo) { s.add(seg.surah); a.add(ay); l.add(seg.label) }
        }
        return Triple(s.toIntArray(), a.toIntArray(), l.toTypedArray())
    }
}

/* ============================ محور «تعلّم الطهارة والصلاة» ============================ */

@Composable
fun LearnHubScreen(onOpen: (String) -> Unit, onBack: () -> Unit, vm: LearnViewModel = hiltViewModel()) {
    val file by vm.purity.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("تعلّم الطهارة والصلاة", onBack)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (file.disclaimer.isNotBlank()) item("d") { InfoBanner(file.disclaimer) }
            items(file.sections, key = { it.id }) { s ->
                Card(Modifier.fillMaxWidth().clickable { onOpen("learn_section/${s.id}") }) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.CleanHands, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(s.title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                            if (s.draft) Text("مسودّة — تحتاج مراجعةً فقهيّة", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
fun LearnSectionScreen(id: String, onOpen: (String) -> Unit, onBack: () -> Unit, vm: LearnViewModel = hiltViewModel()) {
    val file by vm.purity.collectAsStateWithLifecycle()
    val section = remember(file, id) { file.sections.firstOrNull { it.id == id } }
    var sourceDialog by remember { mutableStateOf<LearnSource?>(null) }
    val cards by hiltViewModel<ThemeToggleViewModel>().adhkarCardView.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader(section?.title ?: "الدرس", onBack, actions = {
            if (section != null && section.steps.isNotEmpty()) AdhkarViewToggleButton()
        })
        if (section == null) return@Column
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (section.imageCount > 0) item("gallery") { LearnGallery(section.imageDir, section.imageCount) }
            if (section.draft) item("draft") { InfoBanner("محتوى فقهيّ (مالكيّ) — مسودّةٌ تحتاج مراجعةً علميّةً قبل الاعتماد.") }
            if (section.intro.isNotBlank()) item("intro") { Text(section.intro, color = MaterialTheme.colorScheme.outline) }
            if (section.note.isNotBlank() && section.steps.isEmpty()) item("note") {
                Text(section.note, Modifier.fillMaxWidth().padding(24.dp), color = MaterialTheme.colorScheme.outline)
            }
            if (section.tool.isNotBlank()) {
                val (toolLabel, toolRoute) = when (section.tool) {
                    "qasr" -> "هل يجوز لي القصر الآن؟" to "qasr_tool"
                    "tahara" -> "طهرت الآن، ماذا أصلّي؟" to "tahara_tool"
                    else -> "" to ""
                }
                if (toolRoute.isNotBlank()) item("tool") {
                    Card(
                        Modifier.fillMaxWidth().clickable { onOpen(toolRoute) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Filled.Calculate, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("أداة: $toolLabel", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
            if (section.steps.isNotEmpty()) {
                if (cards) item("steps") { StepPager(section.steps) { sourceDialog = it } }
                else items(section.steps, key = { "s${it.n}" }) { st -> StepListCard(st) { sourceDialog = it } }
            }
            items(section.rulings, key = { it.title }) { g -> RulingCard(g) { sourceDialog = it } }
        }
    }
    sourceDialog?.let { src ->
        AlertDialog(
            onDismissRequest = { sourceDialog = null },
            confirmButton = { TextButton(onClick = { sourceDialog = null }) { Text("إغلاق") } },
            title = { Text("المصدر") },
            text = { Text("${src.title}${if (src.ref.isNotBlank()) "\n${src.ref}" else ""}") },
        )
    }
}

@Composable
private fun StepPager(steps: List<LearnStep>, onSource: (LearnSource) -> Unit) {
    val pager = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()
    // حالة «شرح أكثر» مشتركةٌ بين كلّ الخطوات: تبقى ظاهرةً عند التمرير للبطاقة التالية.
    var more by remember { mutableStateOf(false) }
    Column {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) { page ->
            val st = steps[page]
            Card(
                Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    .clickable(enabled = page < steps.lastIndex) { scope.launch { pager.animateScrollToPage(page + 1) } },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("الخطوة ${st.n} من ${steps.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(st.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    if (st.ruling.isNotBlank()) AssistChip(onClick = {}, label = { Text(st.ruling) })
                    Text(st.short, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                    if (more && st.full.isNotBlank()) Text(st.full, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (st.full.isNotBlank()) TextButton(onClick = { more = !more }) { Text(if (more) "إخفاء" else "شرح أكثر") }
                        st.source?.let { s -> TextButton(onClick = { onSource(s) }) { Text("المصدر") } }
                        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        ShareCopyRow(listOfNotNull(st.title, st.short, st.full.ifBlank { null }).joinToString("\n"))
                    }
                }
            }
        }
        Text("انقر البطاقة أو مرّر للخطوة التالية", Modifier.fillMaxWidth().padding(top = 6.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

/** بطاقة خطوةٍ في القائمة الطوليّة (بلا سلايد). */
@Composable
private fun StepListCard(st: LearnStep, onSource: (LearnSource) -> Unit) {
    var more by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("الخطوة ${st.n}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(st.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (st.ruling.isNotBlank()) AssistChip(onClick = {}, label = { Text(st.ruling) })
            Text(st.short, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            if (more && st.full.isNotBlank()) Text(st.full, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (st.full.isNotBlank()) TextButton(onClick = { more = !more }) { Text(if (more) "إخفاء" else "شرح أكثر") }
                st.source?.let { s -> TextButton(onClick = { onSource(s) }) { Text("المصدر") } }
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                ShareCopyRow(listOfNotNull(st.title, st.short, st.full.ifBlank { null }).joinToString("\n"))
            }
        }
    }
}

@Composable
private fun RulingCard(group: LearnRulingGroup, onSource: (LearnSource) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(group.title, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.primary)
                group.source?.let { s -> TextButton(onClick = { onSource(s) }) { Text("المصدر") } }
            }
            if (group.note.isNotBlank()) Text(group.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            group.items.forEach { it2 ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("•", color = MaterialTheme.colorScheme.primary)
                    Text(it2.text, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                    if (it2.ruling.isNotBlank()) Text(it2.ruling, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    it2.source?.let { s -> TextButton(onClick = { onSource(s) }) { Text("المصدر") } }
                }
            }
        }
    }
}

/** معرضٌ مصوَّرٌ قابلٌ للتمرير (نقر/سحب) — صورٌ مكتملةٌ بذاتها فيها الخطوة والشرح. */
@Composable
private fun LearnGallery(dir: String, count: Int) {
    val pager = rememberPagerState(pageCount = { count })
    val scope = rememberCoroutineScope()
    Column {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().aspectRatio(1f)) { page ->
            coil.compose.AsyncImage(
                model = "file:///android_asset/$dir/${"%02d".format(page + 1)}.webp",
                contentDescription = "صورةٌ توضيحيّة ${page + 1}",
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .clickable(enabled = page < count - 1) { scope.launch { pager.animateScrollToPage(page + 1) } },
            )
        }
        Text(
            "الدليل المصوَّر — ${pager.currentPage + 1} / $count",
            Modifier.fillMaxWidth().padding(top = 4.dp),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
fun InfoBanner(text: String) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}
