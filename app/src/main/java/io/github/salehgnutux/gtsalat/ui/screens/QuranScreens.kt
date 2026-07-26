package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.salehgnutux.gtsalat.audio.QuranAudio
import io.github.salehgnutux.gtsalat.audio.QuranMode
import io.github.salehgnutux.gtsalat.audio.QuranPlayback
import io.github.salehgnutux.gtsalat.data.QuranRepository
import io.github.salehgnutux.gtsalat.domain.QuranAyah
import io.github.salehgnutux.gtsalat.domain.Reciter
import io.github.salehgnutux.gtsalat.domain.Riwaya
import io.github.salehgnutux.gtsalat.domain.SurahMeta
import io.github.salehgnutux.gtsalat.ui.theme.AmiriQuran
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/* ============================ بيانات القرآن ============================ */

@HiltViewModel
class QuranMetaViewModel @Inject constructor(
    private val repo: QuranRepository,
    settingsRepo: io.github.salehgnutux.gtsalat.data.settings.SettingsRepository,
) : ViewModel() {
    private val _surahs = MutableStateFlow<List<SurahMeta>>(emptyList())
    val surahs: StateFlow<List<SurahMeta>> = _surahs.asStateFlow()
    private val _reciters = MutableStateFlow<List<Reciter>>(emptyList())
    val reciters: StateFlow<List<Reciter>> = _reciters.asStateFlow()
    private val _riwayat = MutableStateFlow<List<Riwaya>>(emptyList())
    val riwayat: StateFlow<List<Riwaya>> = _riwayat.asStateFlow()
    private val _surahReciters = MutableStateFlow<List<io.github.salehgnutux.gtsalat.domain.SurahReciter>>(emptyList())
    val surahReciters: StateFlow<List<io.github.salehgnutux.gtsalat.domain.SurahReciter>> = _surahReciters.asStateFlow()

    /** موضع القراءة الأخير للمتابعة: (السورة، الآية) أو null. */
    private val _resume = MutableStateFlow<Triple<Int, String, Int>?>(null)
    val resume: StateFlow<Triple<Int, String, Int>?> = _resume.asStateFlow()

    init {
        viewModelScope.launch {
            _surahs.value = repo.surahs()
            _reciters.value = repo.reciters()
            _riwayat.value = repo.riwayat()
            _surahReciters.value = repo.surahReciters()
            val s = settingsRepo.current()
            if (s.lastReadSurah in 1..114) {
                val name = repo.surah(s.lastReadSurah)?.ar ?: "سورة ${s.lastReadSurah}"
                _resume.value = Triple(s.lastReadSurah, name, s.lastReadAyah)
            }
        }
    }
}

/* ============================ محور القرآن ============================ */

private data class QSection(val label: String, val note: String, val icon: ImageVector, val route: String)

@Composable
fun QuranHubScreen(onOpen: (String) -> Unit, onBack: () -> Unit, vm: QuranMetaViewModel = hiltViewModel()) {
    val resume by vm.resume.collectAsStateWithLifecycle()
    val sections = listOf(
        QSection("القرآن النصّيّ", "قراءةٌ واستماعٌ آية-بآية مع تظليل", Icons.Outlined.MenuBook, "quran_text"),
        QSection("القرآن المسموع", "تلاواتٌ كاملةٌ بالقرّاء والروايات", Icons.Outlined.Headphones, "quran_audio"),
        QSection("المصحف المصوَّر", "صور صفحات مصحف المدينة — للقراءة", Icons.Outlined.AutoStories, "quran_mushaf"),
    )
    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("القرآن الكريم", onBack)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            resume?.let { (surah, name, ayah) ->
                item {
                    Card(
                        Modifier.fillMaxWidth().clickable { onOpen("quran_read/$surah") },
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Column(Modifier.weight(1f)) {
                                Text("متابعة القراءة", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("سورة $name · الآية $ayah", fontFamily = AmiriQuran, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
            items(sections) { s ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(s.route) }) {
                    Row(
                        Modifier.fillMaxWidth().padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(
                            Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) { Icon(s.icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                        Column(Modifier.weight(1f)) {
                            Text(s.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(s.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

/* ============================ فهرس السور (مشترَك) ============================ */

@Composable
fun SurahIndexScreen(
    title: String,
    onOpen: (Int) -> Unit,
    onBack: () -> Unit,
    vm: QuranMetaViewModel = hiltViewModel(),
) {
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        SubScreenHeader(title, onBack)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(surahs, key = { it.n }) { s ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(s.n) }) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) { Text("${s.n}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                        Column(Modifier.weight(1f)) {
                            Text(s.ar, fontFamily = AmiriQuran, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("${s.place} · ${s.verses} آية · صفحة ${s.page}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

/* ============================ ① القرآن النصّيّ (قراءة + استماع) ============================ */

@HiltViewModel
class TextReaderViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: QuranRepository,
    private val settingsRepo: io.github.salehgnutux.gtsalat.data.settings.SettingsRepository,
) : ViewModel() {
    val n: Int = (savedState.get<String>("n") ?: "1").toIntOrNull() ?: 1
    private val _surah = MutableStateFlow<SurahMeta?>(null)
    val surah: StateFlow<SurahMeta?> = _surah.asStateFlow()
    private val _ayat = MutableStateFlow<List<QuranAyah>>(emptyList())
    val ayat: StateFlow<List<QuranAyah>> = _ayat.asStateFlow()
    private val _reciters = MutableStateFlow<List<Reciter>>(emptyList())
    val reciters: StateFlow<List<Reciter>> = _reciters.asStateFlow()
    private val _reciter = MutableStateFlow<Reciter?>(null)
    val reciter: StateFlow<Reciter?> = _reciter.asStateFlow()

    /** الآية التي يُفتَح عليها القارئ (المحفوظة إن كانت لنفس السورة، وإلّا 1). */
    var initialAyah: Int = 1
        private set

    fun pick(r: Reciter) {
        _reciter.value = r
        viewModelScope.launch { settingsRepo.setLastReciter(r.id) }
    }

    /** حفظ موضع القراءة الحاليّ (للمتابعة لاحقاً). */
    fun savePosition(ayah: Int) {
        viewModelScope.launch { settingsRepo.setLastRead(n, ayah.coerceAtLeast(1)) }
    }

    init {
        viewModelScope.launch {
            _surah.value = repo.surah(n)
            _ayat.value = repo.ayat(n)
            // القرّاء ذوو صوت آية-بآية فقط
            val list = repo.reciters().filter { it.hasAyahAudio }
            _reciters.value = list
            val cur = settingsRepo.current()
            _reciter.value = list.firstOrNull { it.id == cur.lastReciterId }
                ?: list.firstOrNull { it.id == "alafasy" } ?: list.firstOrNull()
            // إن عُدنا لنفس السورة نتابع من آيتها المحفوظة، وإلّا من أوّلها.
            initialAyah = if (cur.lastReadSurah == n) cur.lastReadAyah.coerceAtLeast(1) else 1
            settingsRepo.setLastRead(n, initialAyah)
        }
    }
}

@Composable
fun TextReaderScreen(onBack: () -> Unit, vm: TextReaderViewModel = hiltViewModel()) {
    val ctx = LocalContext.current
    val surah by vm.surah.collectAsStateWithLifecycle()
    val ayat by vm.ayat.collectAsStateWithLifecycle()
    val reciters by vm.reciters.collectAsStateWithLifecycle()
    val reciter by vm.reciter.collectAsStateWithLifecycle()
    val play by QuranPlayback.state.collectAsStateWithLifecycle()

    val here = play.active && play.mode == QuranMode.AYAH && play.surah == vm.n
    val current = if (here) play.ayah else 0
    val listState = rememberLazyListState()

    // تمريرٌ تلقائيٌّ للآية الجاريّة + حفظ الموضع للمتابعة.
    LaunchedEffect(current) {
        if (current > 0) {
            vm.savePosition(current)
            val idx = ayat.indexOfFirst { it.n == current }
            if (idx >= 0) runCatching { listState.animateScrollToItem(idx) }
        }
    }
    // عند الفتح (بلا تشغيلٍ جارٍ) نقفز إلى الآية المحفوظة للمتابعة.
    LaunchedEffect(ayat) {
        if (ayat.isNotEmpty() && !here && vm.initialAyah > 1) {
            val idx = ayat.indexOfFirst { it.n == vm.initialAyah }
            if (idx >= 0) runCatching { listState.scrollToItem(idx) }
        }
    }

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("سورة ${surah?.ar ?: ""}".trim(), onBack)

        // شريط التحكّم: اختيار القارئ + تشغيل/إيقاف.
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shadowElevation = 1.dp) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ReciterPicker(reciters, reciter, Modifier.weight(1f)) { vm.pick(it) }
                if (here && play.loading) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                }
                FilledIconButton(onClick = {
                    val r = reciter ?: return@FilledIconButton
                    if (here) QuranAudio.toggle(ctx)
                    else QuranAudio.playAyat(ctx, vm.n, surah?.ar ?: "سورة ${vm.n}", ayat.size, r, 1)
                }) {
                    Icon(if (here && play.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "تشغيل")
                }
                if (here) {
                    FilledIconButton(onClick = { QuranAudio.stop(ctx) }) {
                        Icon(Icons.Filled.Stop, contentDescription = "إيقاف")
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(ayat, key = { it.n }) { a ->
                val active = a.n == current
                val r = reciter
                Card(
                    Modifier.fillMaxWidth().clickable {
                        if (r != null) QuranAudio.playAyat(ctx, vm.n, surah?.ar ?: "سورة ${vm.n}", ayat.size, r, a.n)
                    },
                    colors = if (active)
                        androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    else androidx.compose.material3.CardDefaults.cardColors(),
                ) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier.size(30.dp).clip(CircleShape)
                                .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${a.n}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text(
                            a.text, fontFamily = AmiriQuran, fontSize = 23.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold,
                            color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/* ============================ ② القرآن المسموع (تلاوات كاملة) ============================ */

@Composable
fun AudioRecitationScreen(vm: QuranMetaViewModel = hiltViewModel(), onBack: () -> Unit) {
    val ctx = LocalContext.current
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val reciters by vm.surahReciters.collectAsStateWithLifecycle()
    val play by QuranPlayback.state.collectAsStateWithLifecycle()

    var selected by remember { mutableStateOf<io.github.salehgnutux.gtsalat.domain.SurahReciter?>(null) }
    LaunchedEffect(reciters) { if (selected == null) selected = reciters.firstOrNull() }

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("القرآن المسموع", onBack)
        Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("القارئ:", Modifier.padding(end = 10.dp), fontWeight = FontWeight.Bold)
                PickerButton(selected?.let { "${it.ar} · ${riwayaLabel(it.riwaya)}" } ?: "اختر قارئاً", Modifier.weight(1f)) { close ->
                    reciters.forEach { r ->
                        DropdownMenuItem(
                            text = { Text("${r.ar} · ${riwayaLabel(r.riwaya)}") },
                            onClick = { selected = r; close() },
                        )
                    }
                }
            }
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(surahs, key = { it.n }) { s ->
                val playingThis = play.active && play.mode == QuranMode.SURAH && play.surah == s.n
                Card(
                    Modifier.fillMaxWidth().clickable {
                        selected?.let { QuranAudio.playSurah(ctx, s.n, s.ar, it.id, it.ar, it.server) }
                    },
                    colors = if (playingThis)
                        androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    else androidx.compose.material3.CardDefaults.cardColors(),
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) { Text("${s.n}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) }
                        Text(s.ar, fontFamily = AmiriQuran, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        Icon(
                            if (playingThis && play.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            null, tint = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }
        if (play.active && play.mode == QuranMode.SURAH) NowPlayingBar(play.surahName, play.reciterName, play.isPlaying, play.loading)
    }
}

/** شريط تشغيلٍ سفليٌّ للتلاوة الكاملة: السابق/تشغيل/التالي/إيقاف. */
@Composable
private fun NowPlayingBar(title: String, reciter: String, playing: Boolean, loading: Boolean) {
    val ctx = LocalContext.current
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shadowElevation = 8.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontFamily = AmiriQuran, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(reciter, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            if (loading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
            OutlinedButton(onClick = { QuranAudio.prev(ctx) }, contentPadding = PaddingValues(8.dp)) { Text("السابقة") }
            FilledIconButton(onClick = { QuranAudio.toggle(ctx) }) {
                Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "تشغيل")
            }
            OutlinedButton(onClick = { QuranAudio.next(ctx) }, contentPadding = PaddingValues(8.dp)) { Text("التالية") }
            FilledIconButton(onClick = { QuranAudio.stop(ctx) }) { Icon(Icons.Filled.Stop, "إيقاف") }
        }
    }
}

/* ============================ المشغّل المصغّر العائم (عالميّ) ============================ */

/**
 * شريطُ تشغيلٍ مصغّرٌ يظهر فوق الشريط السفليّ متى كانت تلاوةٌ جاريةً في أيّ قسم.
 * النقر على متنه **يعيدك إلى موضع القراءة/الاستماع** (السورة والآية الجاريّة).
 */
@Composable
fun QuranMiniPlayer(onOpen: (String) -> Unit) {
    val ctx = LocalContext.current
    val play by QuranPlayback.state.collectAsStateWithLifecycle()
    if (!play.active) return
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shadowElevation = 8.dp) {
        Row(
            Modifier.fillMaxWidth()
                .clickable { onOpen(if (play.mode == QuranMode.AYAH) "quran_read/${play.surah}" else "quran_audio") }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Outlined.Headphones, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Column(Modifier.weight(1f)) {
                Text(
                    play.surahName.ifBlank { "سورة ${play.surah}" },
                    fontFamily = AmiriQuran, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 1,
                )
                Text(
                    buildString {
                        append(play.reciterName)
                        if (play.mode == QuranMode.AYAH && play.ayah > 0) append(" · آية ${play.ayah}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 1,
                )
            }
            if (play.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 3.dp)
            FilledIconButton(onClick = { QuranAudio.toggle(ctx) }) {
                Icon(if (play.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "تشغيل")
            }
            FilledIconButton(onClick = { QuranAudio.stop(ctx) }) { Icon(Icons.Filled.Stop, "إيقاف") }
        }
    }
}

/* ============================ القارئ المنسدل (مشترَك) ============================ */

@Composable
private fun ReciterPicker(reciters: List<Reciter>, selected: Reciter?, modifier: Modifier = Modifier, onPick: (Reciter) -> Unit) {
    PickerButton(selected?.ar ?: "اختر قارئاً", modifier) { close ->
        reciters.forEach { r ->
            DropdownMenuItem(
                text = { Text("${r.ar}${if (r.style.isNotBlank()) " · ${r.style}" else ""}") },
                onClick = { onPick(r); close() },
            )
        }
    }
}

/** زرٌّ منسدلٌ عامٌّ: يعرض [label] ويفتح قائمةً يبنيها [items] (مع دالّة إغلاق). */
@Composable
private fun PickerButton(label: String, modifier: Modifier = Modifier, items: @Composable (close: () -> Unit) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(label, maxLines = 1)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            items { open = false }
        }
    }
}

/** تسميةٌ عربيّةٌ مختصرةٌ للرواية. */
private fun riwayaLabel(id: String): String = when (id) {
    "warsh" -> "ورش"
    "hafs" -> "حفص"
    "qaloon" -> "قالون"
    "aldoori" -> "الدوري"
    else -> id
}
