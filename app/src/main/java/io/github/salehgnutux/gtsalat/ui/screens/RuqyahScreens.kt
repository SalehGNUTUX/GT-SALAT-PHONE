package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.salehgnutux.gtsalat.audio.RuqyahAudio
import io.github.salehgnutux.gtsalat.audio.RuqyahPlayback
import io.github.salehgnutux.gtsalat.ui.theme.AmiriQuran
import kotlinx.coroutines.launch

/* ============================ محور «الرقية الشرعية» ============================ */

@Composable
fun RuqyahHubScreen(onOpen: (String) -> Unit, onBack: () -> Unit, vm: LearnViewModel = hiltViewModel()) {
    val ctx = LocalContext.current
    val file by vm.ruqyah.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("الرقية الشرعية", onBack)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (file.disclaimer.isNotBlank()) item("d") { InfoBanner(file.disclaimer) }
            item("radio") {
                Card(
                    Modifier.fillMaxWidth().clickable {
                        io.github.salehgnutux.gtsalat.audio.RadioAudio.play(ctx, "إذاعة الرقية الشرعية", "https://backup.qurango.net/radio/roqiah")
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(Icons.Filled.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Column(Modifier.weight(1f)) {
                            Text("إذاعة الرقية الشرعية — بثٌّ مباشر", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("تحصين البيت والنفس والجسد", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
            itemsIndexed(file.sections, key = { _, s -> s.id }) { _, s ->
                Card(Modifier.fillMaxWidth().clickable { onOpen("ruqyah_section/${s.id}") }) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Spa, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(s.title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                            if (s.note.isNotBlank()) Text(s.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
fun RuqyahSectionScreen(id: String, onOpen: (String) -> Unit, onBack: () -> Unit, vm: LearnViewModel = hiltViewModel()) {
    val ctx = LocalContext.current
    val file by vm.ruqyah.collectAsStateWithLifecycle()
    val section = remember(file, id) { file.sections.firstOrNull { it.id == id } }
    val play by RuqyahPlayback.state.collectAsStateWithLifecycle()
    val cards by hiltViewModel<ThemeToggleViewModel>().adhkarCardView.collectAsStateWithLifecycle()

    var items by remember(id) { mutableStateOf<List<RuqyahItem>>(emptyList()) }
    LaunchedEffect(section) { section?.let { items = vm.ruqyahItems(it) } }
    val playlist = remember(section) { section?.let { vm.ruqyahPlaylist(it) } }
    val segStart = remember(section) {
        val out = ArrayList<Int>(); var acc = 0
        section?.segments?.forEach { seg ->
            out.add(if (seg.kind == "quran" && seg.surah > 0) acc else -1)
            if (seg.kind == "quran" && seg.surah > 0) acc += (seg.ayahTo - seg.ayahFrom + 1)
        }
        out
    }
    var mode by remember { mutableStateOf("read") }   // read | listen
    val pager = rememberPagerState(pageCount = { items.size })
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader(section?.title ?: "الرقية", onBack, actions = { if (mode == "read") AdhkarViewToggleButton() })
        if (section == null) return@Column
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(mode == "read", { mode = "read" }, { Text("أقرأ بنفسي") })
            FilterChip(mode == "listen", { mode = "listen" }, { Text("أستمع") })
        }
        if (section.note.isNotBlank()) {
            Box(Modifier.padding(horizontal = 12.dp)) {
                if (section.link.isNotBlank()) LinkBanner(section.note) { onOpen(section.link) } else InfoBanner(section.note)
            }
        }

        when {
            // وضع القراءة — بطاقات (نقر/تمرير جانبيّ) افتراضيّاً، مع إمكانية التبديل للقائمة الطوليّة.
            mode == "read" && cards -> {
                HorizontalPager(
                    state = pager,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    pageSpacing = 12.dp,
                ) { page ->
                    RuqyahCard(items.getOrNull(page)) {
                        if (page < items.lastIndex) scope.launch { pager.animateScrollToPage(page + 1) }
                    }
                }
                Text(
                    "${(pager.currentPage + 1).coerceAtMost(items.size)} / ${items.size}",
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center,
                )
            }
            mode == "read" -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(items, key = { i, _ -> i }) { _, seg -> RuqyahListCard(seg) }
            }
            // وضع الاستماع — قائمةٌ بتظليل المقطع الجاري + «تشغيل من هنا».
            else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (playlist != null && playlist.first.isNotEmpty()) item("playall") {
                    Card(
                        Modifier.fillMaxWidth().clickable { RuqyahAudio.play(ctx, playlist.first, playlist.second, playlist.third, 0) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Filled.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("تشغيل الرقية كاملةً", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
                itemsIndexed(items, key = { i, _ -> i }) { i, seg ->
                    val start = segStart.getOrElse(i) { -1 }
                    val highlighted = play.active && start >= 0 && play.index >= start &&
                        play.index < (segStart.drop(i + 1).firstOrNull { it >= 0 } ?: Int.MAX_VALUE)
                    Card(
                        Modifier.fillMaxWidth().clickable(enabled = playlist != null && start >= 0) {
                            playlist?.let { RuqyahAudio.play(ctx, it.first, it.second, it.third, start) }
                        },
                        colors = if (highlighted) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer) else CardDefaults.cardColors(),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(seg.label, Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                if (start >= 0) Icon(Icons.Filled.PlayArrow, "تشغيل من هنا", tint = MaterialTheme.colorScheme.outline)
                            }
                            if (seg.text.isNotBlank()) Text(seg.text, fontFamily = AmiriQuran, fontSize = 20.sp, lineHeight = 34.sp, maxLines = 3, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        if (play.active) RuqyahPlayerBar(ctx, play)
    }
}

/** بطاقة مقطع رقيةٍ (عرض سلايد): نصٌّ + مصدرٌ + نسخ/مشاركة، تُنقَر للتالي. */
@Composable
private fun RuqyahCard(item: RuqyahItem?, onTap: () -> Unit) {
    if (item == null) return
    Card(
        Modifier.fillMaxSize().padding(vertical = 6.dp).clickable(onClick = onTap),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(item.label, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            val scroll = rememberScrollState()
            if (item.text.isNotBlank()) Text(
                item.text, fontFamily = AmiriQuran,
                fontSize = if (item.text.length > 240) 23.sp else 27.sp,
                lineHeight = if (item.text.length > 240) 42.sp else 48.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll),
            )
            // صفٌّ سفليٌّ مضغوط: النسخ/المشاركة + المصدر — فيبقى معظمُ البطاقة للنصّ.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                ShareCopyRow(item.text)
                item.source?.let { s -> Text("${s.title}${if (s.ref.isNotBlank()) " — ${s.ref}" else ""}", Modifier.weight(1f, fill = false), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.End, maxLines = 2) }
            }
        }
    }
}

/** بطاقة مقطعٍ في القائمة الطوليّة (نصٌّ + مصدرٌ + نسخ/مشاركة). */
@Composable
private fun RuqyahListCard(item: RuqyahItem) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.label, Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                ShareCopyRow(item.text)
            }
            if (item.text.isNotBlank()) Text(item.text, fontFamily = AmiriQuran, fontSize = 22.sp, lineHeight = 38.sp, color = MaterialTheme.colorScheme.onSurface)
            item.source?.let { s -> Text("المصدر: ${s.title}${if (s.ref.isNotBlank()) " — ${s.ref}" else ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline) }
        }
    }
}

/** لافتةٌ قابلةٌ للنقر تنقل إلى مسارٍ (مثل قسم الأذكار). */
@Composable
private fun LinkBanner(text: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
private fun RuqyahPlayerBar(ctx: android.content.Context, play: io.github.salehgnutux.gtsalat.audio.RuqyahState) {
    Card(Modifier.fillMaxWidth().padding(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${play.label}  (${play.index + 1}/${play.total})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            LinearProgressIndicator(
                progress = { if (play.durMs > 0) (play.posMs.toFloat() / play.durMs).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { RuqyahAudio.stop(ctx) }) { Icon(Icons.Filled.Stop, "إيقاف", tint = MaterialTheme.colorScheme.error) }
                IconButton(onClick = { RuqyahAudio.next(ctx) }) { Icon(Icons.Filled.SkipPrevious, "التالي") }
                IconButton(onClick = { RuqyahAudio.toggle(ctx) }) {
                    Icon(if (play.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "تشغيل/إيقاف", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { RuqyahAudio.prev(ctx) }) { Icon(Icons.Filled.SkipNext, "السابق") }
                IconButton(onClick = { RuqyahAudio.toggleRepeat(ctx) }) {
                    Icon(
                        if (play.repeatMode == 1) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = when (play.repeatMode) { 1 -> "تكرار المقطع"; 2 -> "تكرار الكلّ"; else -> "بلا تكرار" },
                        tint = if (play.repeatMode != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}
