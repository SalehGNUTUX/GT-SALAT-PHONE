package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/* ============================ بيانات القرآن ============================ */

@HiltViewModel
class QuranMetaViewModel @Inject constructor(
    private val repo: QuranRepository,
    private val downloader: io.github.salehgnutux.gtsalat.data.QuranDownloader,
    private val settingsRepo: io.github.salehgnutux.gtsalat.data.settings.SettingsRepository,
) : ViewModel() {

    /** آخر صفحة مصحفٍ محفوظة (للاستئناف عند فتح المصحف المصوَّر). */
    private val _lastMushafPage = MutableStateFlow(0)  // 0 = لم يُحمَّل بعد
    val lastMushafPage: StateFlow<Int> = _lastMushafPage.asStateFlow()
    fun saveMushafPage(page: Int) { viewModelScope.launch { settingsRepo.setLastMushafPage(page) } }

    /** ملفّ صفحة المصحف المحلّيّ إن نُزِّل (حسب الرواية). */
    fun localPage(page: Int, riwaya: String = "hafs"): java.io.File? = downloader.localPage(page, riwaya)

    /** تنزيل/حذف صور المصحف (متاحٌ داخل قسم المصحف نفسه). */
    val mushafDownload = downloader.mushaf
    fun downloadMushaf(riwaya: String) = viewModelScope.launch { downloader.downloadMushaf(riwaya) }
    fun deleteMushaf(riwaya: String) = downloader.deleteMushaf(riwaya)
    fun mushafCount(riwaya: String): Int = downloader.mushafDownloadedCount(riwaya)

    /** هل نُزِّلت سورةٌ لقارئٍ محليّاً؟ */
    fun surahDownloaded(reciterId: String, surah: Int): Boolean = downloader.localSurah(reciterId, surah) != null

    /** تنزيل سورةٍ صوتيّاً؛ يُبلِّغ [onProgress] بالنسبة و[done] بالنتيجة. */
    fun downloadSurah(reciterId: String, server: String, surah: Int, onProgress: (Int) -> Unit, done: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = downloader.downloadSurah(reciterId, server, surah) { p -> viewModelScope.launch { onProgress(p) } }
            done(ok)
        }
    }

    /** حذف سورةٍ منزَّلة. */
    fun deleteSurah(reciterId: String, surah: Int) { downloader.deleteSurah(reciterId, surah) }

    /** أرقام السور المُنزَّلة لقارئ. */
    fun downloadedSurahs(reciterId: String): Set<Int> = downloader.downloadedSurahs(reciterId)

    /** أرقام السور التي نُزِّل صوت آياتها (نصّيّ، لأيّ قارئ). */
    fun downloadedAyatSurahs(): Set<Int> = downloader.downloadedAyatSurahs()

    /** كلّ القرّاء الذين لهم سورٌ منزَّلة (مُعرّف → أرقام السور). */
    fun downloadedByReciter(): Map<String, Set<Int>> = downloader.downloadedByReciter()

    /** قارئٌ من القائمة بمُعرّفه. */
    fun surahReciterById(id: String): io.github.salehgnutux.gtsalat.domain.SurahReciter? =
        _surahReciters.value.firstOrNull { it.id == id }

    /** بطاقة القارئ المحفوظة على القرص (تعمل دون إنترنت ودون قائمة القرّاء). */
    fun savedReciterMeta(id: String): io.github.salehgnutux.gtsalat.domain.SurahReciter? = downloader.reciterMeta(id)

    /** يحفظ بطاقة القارئ بجانب تنزيلاته ليظهر اسمه لاحقاً دون شبكة. */
    fun saveReciterMeta(reciter: io.github.salehgnutux.gtsalat.domain.SurahReciter) = downloader.saveReciterMeta(reciter)
    private val _surahs = MutableStateFlow<List<SurahMeta>>(emptyList())
    val surahs: StateFlow<List<SurahMeta>> = _surahs.asStateFlow()
    private val _reciters = MutableStateFlow<List<Reciter>>(emptyList())
    val reciters: StateFlow<List<Reciter>> = _reciters.asStateFlow()
    private val _riwayat = MutableStateFlow<List<Riwaya>>(emptyList())
    val riwayat: StateFlow<List<Riwaya>> = _riwayat.asStateFlow()
    private val _surahReciters = MutableStateFlow<List<io.github.salehgnutux.gtsalat.domain.SurahReciter>>(emptyList())
    val surahReciters: StateFlow<List<io.github.salehgnutux.gtsalat.domain.SurahReciter>> = _surahReciters.asStateFlow()

    /** متابعة القراءة/الاستماع — **تفاعليّة** (تتحدّث فور تغيّر الموضع)، (سورة، اسم، آية) أو null. */
    val readResume: StateFlow<Triple<Int, String, Int>?> =
        kotlinx.coroutines.flow.combine(settingsRepo.settings, surahs) { s, list ->
            if (s.lastReadSurah in 1..114) Triple(s.lastReadSurah, list.firstOrNull { it.n == s.lastReadSurah }?.ar ?: "${s.lastReadSurah}", s.lastReadAyah) else null
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), null)

    val listenResume: StateFlow<Triple<Int, String, Int>?> =
        kotlinx.coroutines.flow.combine(settingsRepo.settings, surahs) { s, list ->
            if (s.lastListenSurah in 1..114) Triple(s.lastListenSurah, list.firstOrNull { it.n == s.lastListenSurah }?.ar ?: "${s.lastListenSurah}", s.lastListenAyah) else null
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), null)

    /** متابعة القرآن المسموع (سورةٌ كاملة): آخر سورةٍ/قارئٍ وموضعٍ محفوظ — لاستئناف الاستماع. */
    val audioResume: StateFlow<AudioResume?> =
        kotlinx.coroutines.flow.combine(settingsRepo.settings, surahs) { s, list ->
            if (s.lastAudioSurah in 1..114) AudioResume(
                surah = s.lastAudioSurah,
                surahName = list.firstOrNull { it.n == s.lastAudioSurah }?.ar ?: "سورة ${s.lastAudioSurah}",
                reciterId = s.lastAudioReciter,
                reciterName = s.lastAudioReciterName,
                server = s.lastAudioServer,
                posMs = s.lastAudioPosMs,
            ) else null
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), null)

    /** الإشارات المرجعيّة مرتّبةً (سورة، اسم، آية) — تفاعليّة. */
    val bookmarks: StateFlow<List<Triple<Int, String, Int>>> =
        kotlinx.coroutines.flow.combine(settingsRepo.settings, surahs) { s, list ->
            s.bookmarks.mapNotNull { bm ->
                val p = bm.split(":")
                val sn = p.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
                val ay = p.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                Triple(sn, list.firstOrNull { it.n == sn }?.ar ?: "$sn", ay)
            }.sortedWith(compareBy({ it.first }, { it.third }))
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeBookmark(surah: Int, ayah: Int) = viewModelScope.launch { settingsRepo.removeBookmark(surah, ayah) }

    /** حالة الوِرد الخام (الوحدة/العدد/آخر إتمام/السلسلة) — تُحسَب اليوم/السلسلة السارية في الواجهة. */
    val wird: StateFlow<WirdRaw> =
        settingsRepo.settings.map { WirdRaw(it.enableWird, it.wirdGoalUnit, it.wirdGoalCount, it.wirdLastDoneDate, it.wirdStreak) }
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), WirdRaw())

    fun setWirdGoal(unit: String, count: Int) = viewModelScope.launch { settingsRepo.setWirdGoal(unit, count) }
    fun markWirdDone() = viewModelScope.launch {
        settingsRepo.markWirdDone(java.time.LocalDate.now().toString(), java.time.LocalDate.now().minusDays(1).toString())
    }
    fun undoWirdToday() = viewModelScope.launch {
        settingsRepo.undoWirdToday(java.time.LocalDate.now().toString(), java.time.LocalDate.now().minusDays(1).toString())
    }

    // بحثٌ داخل الآيات (بالكلمات والعبارات)
    private val _ayahHits = MutableStateFlow<List<io.github.salehgnutux.gtsalat.domain.AyahHit>>(emptyList())
    val ayahHits: StateFlow<List<io.github.salehgnutux.gtsalat.domain.AyahHit>> = _ayahHits.asStateFlow()
    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()
    private var searchJob: kotlinx.coroutines.Job? = null

    fun searchAyat(query: String) {
        searchJob?.cancel()
        if (io.github.salehgnutux.gtsalat.domain.Quran.normalize(query).length < 2) {
            _ayahHits.value = emptyList(); _searching.value = false; return
        }
        _searching.value = true
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(250)   // تهدئةٌ بسيطة بين ضغطات المفاتيح
            _ayahHits.value = repo.searchAyat(query)
            _searching.value = false
        }
    }

    private var onlineLoaded = false
    /** يجلب كامل قائمة قرّاء المصدر (شبكة) **كسولاً** عند فتح القرآن المسموع فقط — لا عند الإقلاع. */
    fun ensureOnlineReciters() {
        if (onlineLoaded) return
        onlineLoaded = true
        viewModelScope.launch { _surahReciters.value = repo.surahRecitersOnline() }
    }

    init {
        viewModelScope.launch {
            _surahs.value = repo.surahs()
            _reciters.value = repo.reciters()
            _riwayat.value = repo.riwayat()
            _surahReciters.value = repo.surahReciters()   // المُضمَّنون فوراً (دون شبكة/إقلاع بطيء)
            _lastMushafPage.value = settingsRepo.current().lastMushafPage.coerceIn(1, 604)
        }
    }
}

/* ============================ محور القرآن ============================ */

private data class QSection(val label: String, val note: String, val icon: ImageVector, val route: String)

/** حالة الوِرد الخام كما تُخزَّن (تُشتقّ منها حالةُ اليوم في الواجهة). */
data class WirdRaw(val enabled: Boolean = false, val unit: String = "juz", val count: Int = 1, val lastDate: String = "", val streak: Int = 0)

/** حالة متابعة القرآن المسموع (سورةٌ كاملة) — لعرض بطاقة الاستئناف. */
data class AudioResume(val surah: Int, val surahName: String, val reciterId: String, val reciterName: String, val server: String, val posMs: Long)

/** وصف هدف الوِرد بالعربيّة (مفرد/جمع مبسّط). */
fun wirdGoalLabel(unit: String, count: Int): String = when (unit) {
    "juz" -> if (count == 1) "جزءٌ واحد" else "$count أجزاء"
    "hizb" -> if (count == 1) "حزبٌ واحد" else "$count أحزاب"
    "pages" -> if (count == 1) "صفحةٌ واحدة" else "$count صفحات"
    "ayat" -> if (count == 1) "آيةٌ واحدة" else "$count آيات"
    else -> "$count"
}

val WIRD_UNITS = listOf("juz" to "أجزاء", "hizb" to "أحزاب", "pages" to "صفحات", "ayat" to "آيات")

/** بطاقة متابعةٍ (قراءة/استماع) — عنوانٌ وسورةٌ وآية، تفتح القارئ على الموضع. */
@Composable
private fun ResumeCard(title: String, name: String, ayah: Int, icon: ImageVector, container: androidx.compose.ui.graphics.Color, onContainer: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = container),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, null, tint = onContainer)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = onContainer)
                Text("سورة $name · الآية $ayah", fontFamily = AmiriQuran, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = onContainer)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = onContainer)
        }
    }
}

@Composable
fun QuranHubScreen(onOpen: (String) -> Unit, onBack: () -> Unit, vm: QuranMetaViewModel = hiltViewModel()) {
    val readResume by vm.readResume.collectAsStateWithLifecycle()
    val listenResume by vm.listenResume.collectAsStateWithLifecycle()
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()
    val wird by vm.wird.collectAsStateWithLifecycle()
    var editWird by remember { mutableStateOf(false) }
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
            if (wird.enabled) {
                item("wird") { WirdCard(wird, onMark = { vm.markWirdDone() }, onUndo = { vm.undoWirdToday() }, onEdit = { editWird = true }) }
            }
            readResume?.let { (surah, name, ayah) ->
                item("resume_read") {
                    ResumeCard("متابعة القراءة", name, ayah, Icons.Outlined.MenuBook, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer) {
                        onOpen("quran_read/$surah?ayah=$ayah")
                    }
                }
            }
            listenResume?.let { (surah, name, ayah) ->
                item("resume_listen") {
                    ResumeCard("متابعة الاستماع", name, ayah, Icons.Outlined.Headphones, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer) {
                        onOpen("quran_read/$surah?ayah=$ayah")
                    }
                }
            }
            if (bookmarks.isNotEmpty()) {
                item("bookmarks") {
                    Card(
                        Modifier.fillMaxWidth().clickable { onOpen("quran_bookmarks") },
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Icon(Icons.Filled.Bookmark, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Column(Modifier.weight(1f)) {
                                Text("الإشارات المرجعيّة", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text("${bookmarks.size} إشارةٌ محفوظة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
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
    if (editWird) {
        WirdGoalDialog(wird.unit, wird.count, onDismiss = { editWird = false }) { unit, count ->
            vm.setWirdGoal(unit, count); editWird = false
        }
    }
}

/** بطاقة الوِرد اليوميّ: الهدف + سلسلة الأيّام + زرّ إتمام/تراجع + تعديل الهدف. */
@Composable
private fun WirdCard(wird: WirdRaw, onMark: () -> Unit, onUndo: () -> Unit, onEdit: () -> Unit) {
    val today = remember { java.time.LocalDate.now().toString() }
    val yesterday = remember { java.time.LocalDate.now().minusDays(1).toString() }
    val doneToday = wird.lastDate == today
    // السلسلة ساريةٌ فقط إن كان آخر إتمامٍ اليوم أو الأمس، وإلّا انقطعت.
    val streak = if (wird.lastDate == today || wird.lastDate == yesterday) wird.streak else 0

    Card(
        Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (doneToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.MenuBook, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text("وِرد التلاوة اليوميّ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(wirdGoalLabel(wird.unit, wird.count), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                if (streak > 0) {
                    Text("🔥 $streak", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "تعديل الهدف", tint = MaterialTheme.colorScheme.outline) }
            }
            if (doneToday) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("✓ أتممتَ وردك اليوم — بارك الله فيك", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                    androidx.compose.material3.TextButton(onClick = onUndo) { Text("تراجع") }
                }
            } else {
                androidx.compose.material3.Button(onClick = onMark, modifier = Modifier.fillMaxWidth()) {
                    Text("أتممتُ وردي اليوم")
                }
            }
        }
    }
}

/** حوار تعديل هدف الوِرد: الوحدة (أجزاء/أحزاب/صفحات/آيات) + العدد. */
@Composable
private fun WirdGoalDialog(unit: String, count: Int, onDismiss: () -> Unit, onSave: (String, Int) -> Unit) {
    var u by remember { mutableStateOf(unit) }
    var c by remember { mutableStateOf(count.coerceAtLeast(1)) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("هدف الوِرد اليوميّ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("الوحدة:", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WIRD_UNITS.forEach { (key, label) ->
                        androidx.compose.material3.FilterChip(selected = u == key, onClick = { u = key }, label = { Text(label) })
                    }
                }
                Text("العدد:", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.material3.FilledTonalButton(onClick = { if (c > 1) c-- }) { Text("−", fontWeight = FontWeight.Bold) }
                    Text("$c", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    androidx.compose.material3.FilledTonalButton(onClick = { if (c < 60) c++ }) { Text("+", fontWeight = FontWeight.Bold) }
                }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { onSave(u, c) }) { Text("حفظ") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

/* ============================ الإشارات المرجعيّة ============================ */

@Composable
fun QuranBookmarksScreen(onOpenAyah: (Int, Int) -> Unit, onBack: () -> Unit, vm: QuranMetaViewModel = hiltViewModel()) {
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("الإشارات المرجعيّة", onBack)
        if (bookmarks.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Outlined.Bookmarks, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(56.dp))
                Text(
                    "لا إشاراتٍ بعد.\nاضغط أيقونة الإشارة بجانب أيّ آيةٍ في القارئ النصّيّ لحفظها هنا.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            return
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(bookmarks, key = { "${it.first}:${it.third}" }) { (surah, name, ayah) ->
                Card(Modifier.fillMaxWidth().clickable { onOpenAyah(surah, ayah) }) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Filled.Bookmark, null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text("سورة $name", fontFamily = AmiriQuran, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("الآية $ayah", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        IconButton(onClick = { vm.removeBookmark(surah, ayah) }) {
                            Icon(Icons.Filled.Close, "إزالة", tint = MaterialTheme.colorScheme.error)
                        }
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
    ayahSearch: Boolean = false,                    // بحثٌ داخل الآيات (للقرآن النصّيّ)
    onOpenAyah: (Int, Int) -> Unit = { s, _ -> onOpen(s) },
) {
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val ayahHits by vm.ayahHits.collectAsStateWithLifecycle()
    val searching by vm.searching.collectAsStateWithLifecycle()
    var showSearch by remember { mutableStateOf(false) }
    var q by remember { mutableStateOf("") }
    val shown = remember(surahs, q) { filterSurahs(surahs, q) }
    // السور المُنزَّل صوتُها (نصّيّ) — لعرض مؤشّرٍ عليها.
    val downloadedAyat = remember(ayahSearch, surahs) { if (ayahSearch) vm.downloadedAyatSurahs() else emptySet() }
    // بحثٌ داخل الآيات عند تغيّر النصّ (للقرآن النصّيّ فقط).
    LaunchedEffect(q, ayahSearch) { if (ayahSearch) vm.searchAyat(q) }

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader(title, onBack, actions = {
            IconButton(onClick = { showSearch = !showSearch; if (!showSearch) q = "" }) {
                Icon(if (showSearch) Icons.Filled.Close else Icons.Filled.Search, contentDescription = "بحث")
            }
        })
        if (showSearch) SurahSearchField(q, ayahSearch) { q = it }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // السور المطابقة بالاسم
            if (shown.isNotEmpty()) {
                if (q.isNotBlank()) item("h_surah") { SearchHeader("السور (${shown.size})") }
                items(shown, key = { "s${it.n}" }) { s ->
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
                            if (s.n in downloadedAyat) Icon(Icons.Filled.DownloadDone, "صوتٌ مُنزَّل", tint = MaterialTheme.colorScheme.primary)
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            // الآيات المطابقة بالنصّ (بحثٌ شامل)
            if (ayahSearch && q.isNotBlank()) {
                item("h_ayah") {
                    if (searching) SearchHeader("جارٍ البحث في الآيات…")
                    else SearchHeader(if (ayahHits.isEmpty()) "لا آياتٍ مطابِقة" else "الآيات (${ayahHits.size})")
                }
                items(ayahHits, key = { "a${it.surah}_${it.ayah}" }) { hit ->
                    Card(Modifier.fillMaxWidth().clickable { onOpenAyah(hit.surah, hit.ayah) }) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("سورة ${hit.surahName} · الآية ${hit.ayah}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(hit.text, fontFamily = AmiriQuran, fontSize = 20.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 3)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHeader(text: String) {
    Text(
        text,
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

/* ============================ ① القرآن النصّيّ (قراءة + استماع) ============================ */

@HiltViewModel
class TextReaderViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: QuranRepository,
    private val downloader: io.github.salehgnutux.gtsalat.data.QuranDownloader,
    private val settingsRepo: io.github.salehgnutux.gtsalat.data.settings.SettingsRepository,
) : ViewModel() {
    val n: Int = (savedState.get<String>("n") ?: "1").toIntOrNull() ?: 1
    // آية وجهةٍ من البحث (0 = لا شيء، فنستعمل الموضع المحفوظ).
    private val gotoAyah: Int = (savedState.get<String>("ayah") ?: "0").toIntOrNull() ?: 0
    private val _surah = MutableStateFlow<SurahMeta?>(null)
    val surah: StateFlow<SurahMeta?> = _surah.asStateFlow()
    private val _ayat = MutableStateFlow<List<QuranAyah>>(emptyList())
    val ayat: StateFlow<List<QuranAyah>> = _ayat.asStateFlow()
    private val _reciters = MutableStateFlow<List<Reciter>>(emptyList())
    val reciters: StateFlow<List<Reciter>> = _reciters.asStateFlow()
    private val _reciter = MutableStateFlow<Reciter?>(null)
    val reciter: StateFlow<Reciter?> = _reciter.asStateFlow()

    // الرواية المختارة لنصّ القراءة (حفص مُضمَّن، وغيرها يُجلب ويُخزَّن).
    private val _riwayat = MutableStateFlow<List<Riwaya>>(emptyList())
    val riwayat: StateFlow<List<Riwaya>> = _riwayat.asStateFlow()
    private val _riwaya = MutableStateFlow<Riwaya?>(null)
    val riwaya: StateFlow<Riwaya?> = _riwaya.asStateFlow()
    private val _textLoading = MutableStateFlow(false)
    val textLoading: StateFlow<Boolean> = _textLoading.asStateFlow()

    fun pickRiwaya(r: Riwaya) {
        if (_riwaya.value?.id == r.id) return
        _riwaya.value = r
        viewModelScope.launch {
            settingsRepo.setLastRiwaya(r.id)   // تُستعاد عند العودة للتطبيق
            _textLoading.value = true
            _ayat.value = repo.ayatForRiwaya(n, r.apiSlug)
            _textLoading.value = false
            // نوافق الصوت مع النصّ: نختار قارئاً بنفس الرواية إن توفّر.
            _reciters.value.firstOrNull { it.riwaya == r.id }?.let { _reciter.value = it }
        }
    }

    /** آية المتابعة/الموضع الحاليّ (يُظلَّل بلونٍ مميّز ويُبدأ منه عند التشغيل). */
    private val _target = MutableStateFlow(1)
    val target: StateFlow<Int> = _target.asStateFlow()

    /** إشارات هذه السورة المرجعيّة (أرقام الآيات) — تفاعليّة. */
    val bookmarkedAyat: StateFlow<Set<Int>> =
        settingsRepo.settings.map { s -> s.bookmarks.mapNotNull { bm ->
            val parts = bm.split(":"); if (parts.size == 2 && parts[0].toIntOrNull() == n) parts[1].toIntOrNull() else null
        }.toSet() }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptySet())

    /** يبدّل إشارة آية. */
    fun toggleBookmark(ayah: Int) = viewModelScope.launch { settingsRepo.toggleBookmark(n, ayah) }

    /** يضبط موضع المتابعة يدويّاً (عند نقر آيةٍ لبدء القراءة منها). */
    fun setTarget(ayah: Int) {
        _target.value = ayah
        savePosition(ayah)
    }

    fun pick(r: Reciter) {
        _reciter.value = r
        viewModelScope.launch { settingsRepo.setLastReciter(r.id) }
    }

    /** حفظ موضع القراءة الحاليّ (متابعة القراءة). */
    fun savePosition(ayah: Int) {
        viewModelScope.launch { settingsRepo.setLastRead(n, ayah.coerceAtLeast(1)) }
    }

    /** حفظ موضع الاستماع الحاليّ (متابعة الاستماع — مستقلٌّ عن القراءة). */
    fun saveListenPosition(ayah: Int) {
        viewModelScope.launch { settingsRepo.setLastListen(n, ayah.coerceAtLeast(1)) }
    }

    /** هل نُزِّل صوت آيات هذه السورة للقارئ؟ */
    fun ayatDownloaded(reciterId: String): Boolean = downloader.ayatDownloaded(reciterId, n, _ayat.value.size)

    /** تنزيل صوت آيات السورة الحاليّة لقارئٍ (بنسبة). */
    fun downloadAyat(reciter: Reciter, onProgress: (Int) -> Unit, done: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = downloader.downloadSurahAyat(reciter.id, reciter.everyayah, n, _ayat.value.size) { p -> viewModelScope.launch { onProgress(p) } }
            done(ok)
        }
    }

    /** حذف صوت آيات السورة الحاليّة لقارئ. */
    fun deleteAyat(reciterId: String) { downloader.deleteAyat(reciterId, n, _ayat.value.size) }

    /** مهلة القراءة التلقائيّة (٪) — تُقرأ من الإعدادات وتُطبَّق حيّاً في حلقة القراءة. */
    val scrollSpeed: StateFlow<Int> = settingsRepo.settings.map { it.quranScrollSpeed }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), 100)

    fun setScrollSpeed(pct: Int) = viewModelScope.launch { settingsRepo.setQuranScrollSpeed(pct) }

    init {
        viewModelScope.launch {
            _surah.value = repo.surah(n)
            _ayat.value = repo.ayat(n)
            _riwayat.value = repo.riwayat()
            val cur = settingsRepo.current()
            // نستعيد آخر روايةٍ مختارة (لا نعود دائماً لحفص)؛ وإن كانت غير حفص نجلب نصّها.
            val savedRiwaya = repo.riwayat().firstOrNull { it.id == cur.lastRiwaya }
                ?: repo.riwayat().firstOrNull { it.id == "hafs" }
            _riwaya.value = savedRiwaya
            if (savedRiwaya != null && savedRiwaya.id != "hafs") {
                _textLoading.value = true
                _ayat.value = repo.ayatForRiwaya(n, savedRiwaya.apiSlug)
                _textLoading.value = false
            }
            // القرّاء ذوو صوت آية-بآية فقط
            val list = repo.reciters().filter { it.hasAyahAudio }
            _reciters.value = list
            _reciter.value = list.firstOrNull { it.id == cur.lastReciterId }
                ?: list.firstOrNull { it.id == "alafasy" } ?: list.firstOrNull()
            // آية البحث لها الأولويّة، وإلّا نتابع من الآية المحفوظة لنفس السورة، وإلّا من أوّلها.
            val startAyah = when {
                gotoAyah > 0 -> gotoAyah
                cur.lastReadSurah == n -> cur.lastReadAyah.coerceAtLeast(1)
                else -> 1
            }
            _target.value = startAyah
            settingsRepo.setLastRead(n, startAyah)
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
    val target by vm.target.collectAsStateWithLifecycle()
    val riwayat by vm.riwayat.collectAsStateWithLifecycle()
    val riwaya by vm.riwaya.collectAsStateWithLifecycle()
    val textLoading by vm.textLoading.collectAsStateWithLifecycle()
    val bookmarked by vm.bookmarkedAyat.collectAsStateWithLifecycle()
    val scrollSpeed by vm.scrollSpeed.collectAsStateWithLifecycle()
    // بحثٌ داخل السورة (بالكلمات) للوصول إلى آيةٍ معيّنة.
    var showSearch by remember { mutableStateOf(false) }
    var searchQ by remember { mutableStateOf("") }
    val shownAyat = remember(ayat, searchQ) {
        val nq = io.github.salehgnutux.gtsalat.domain.Quran.normalize(searchQ)
        if (nq.isBlank()) ayat else ayat.filter { io.github.salehgnutux.gtsalat.domain.Quran.normalize(it.text).contains(nq) }
    }

    val here = play.active && play.mode == QuranMode.AYAH && play.surah == vm.n
    val playingAyah = if (here) play.ayah else 0
    // الآية المُظلَّلة: الجاريةُ أثناء التشغيل، وإلّا موضعُ المتابعة (يُبدأ منه).
    val highlight = if (playingAyah > 0) playingAyah else target
    val listState = rememberLazyListState()
    // وضع القراءة التلقائيّة (بلا صوت): تنتقل الآية المظلَّلة تلقائيّاً بوتيرةٍ تناسب طولها.
    var readingMode by remember { mutableStateOf(false) }
    // تنزيل صوت آيات السورة الحاليّة للقارئ (للاستماع دون إنترنت).
    var ayatProgress by remember { mutableStateOf<Int?>(null) }
    var ayatDownloaded by remember(reciter?.id, ayat) { mutableStateOf(reciter?.let { vm.ayatDownloaded(it.id) } ?: false) }
    var confirmDeleteAyat by remember { mutableStateOf(false) }
    val readerSnackbar = remember { androidx.compose.material3.SnackbarHostState() }
    val readerScope = androidx.compose.runtime.rememberCoroutineScope()

    // أثناء الاستماع: نحفظ موضع الاستماع (مستقلّاً عن القراءة).
    LaunchedEffect(playingAyah) {
        if (playingAyah > 0) vm.saveListenPosition(playingAyah)
    }
    LaunchedEffect(highlight, ayat) {
        if (ayat.isNotEmpty() && highlight > 1) {
            val idx = ayat.indexOfFirst { it.n == highlight }
            if (idx >= 0) runCatching { listState.animateScrollToItem(idx) }
        }
    }
    // حلقة التنقّل التلقائيّ في وضع القراءة (تتوقّف عند بدء الاستماع أو نهاية السورة).
    LaunchedEffect(readingMode, here, ayat) {
        if (readingMode && !here && ayat.isNotEmpty()) {
            var cur = vm.target.value.coerceIn(1, ayat.size)
            while (cur <= ayat.size) {
                vm.setTarget(cur)
                val len = ayat.getOrNull(cur - 1)?.text?.length ?: 40
                // المهلة حسب طول الآية × مهلة القراءة المختارة (٪) — تُقرأ حيّاً فتنطبق فوراً.
                val speed = vm.scrollSpeed.value
                kotlinx.coroutines.delay(maxOf(2600L, len * 95L) * speed / 100L)
                cur++
            }
            readingMode = false
        }
    }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("سورة ${surah?.ar ?: ""}".trim(), onBack, actions = {
            IconButton(onClick = { showSearch = !showSearch; if (!showSearch) searchQ = "" }) {
                Icon(if (showSearch) Icons.Filled.Close else Icons.Filled.Search, contentDescription = "بحثٌ في السورة")
            }
        })
        if (showSearch) {
            OutlinedTextField(
                value = searchQ, onValueChange = { searchQ = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                placeholder = { Text("ابحث بكلمةٍ داخل السورة…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = { if (searchQ.isNotEmpty()) IconButton(onClick = { searchQ = "" }) { Icon(Icons.Filled.Close, "مسح") } },
                singleLine = true,
            )
        }

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
                // تنزيل/حذف صوت آيات السورة للقارئ الحاليّ (للاستماع دون إنترنت).
                reciter?.let { r ->
                    when {
                        ayatProgress != null -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("${ayatProgress}٪", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            CircularProgressIndicator(progress = { (ayatProgress ?: 0) / 100f }, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                        ayatDownloaded -> IconButton(onClick = { confirmDeleteAyat = true }) { Icon(Icons.Filled.Delete, "حذف الصوت المُنزَّل", tint = MaterialTheme.colorScheme.error) }
                        else -> IconButton(onClick = {
                            ayatProgress = 0
                            vm.downloadAyat(r, onProgress = { p -> ayatProgress = p }) { ok -> ayatProgress = null; if (ok) ayatDownloaded = true }
                        }) { Icon(Icons.Filled.Download, "تنزيل صوت السورة", tint = MaterialTheme.colorScheme.outline) }
                    }
                }
                // وضع القراءة التلقائيّة (بلا صوت) — يتعطّل أثناء الاستماع.
                if (!here) {
                    if (readingMode) {
                        FilledIconButton(onClick = { readingMode = false }) {
                            Icon(Icons.Filled.Stop, contentDescription = "إيقاف القراءة التلقائيّة")
                        }
                    } else {
                        IconButton(onClick = { readingMode = true }) {
                            Icon(Icons.Outlined.AutoStories, contentDescription = "قراءةٌ تلقائيّة", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                FilledIconButton(onClick = {
                    val r = reciter ?: return@FilledIconButton
                    readingMode = false
                    if (here) QuranAudio.toggle(ctx)
                    else QuranAudio.playAyat(ctx, vm.n, surah?.ar ?: "سورة ${vm.n}", ayat.size, r, highlight.coerceAtLeast(1))
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

        // مهلة القراءة التلقائيّة (تظهر في وضع القراءة): حسب طول الآية × النسبة المختارة.
        if (readingMode && !here) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("مهلة القراءة:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    listOf(70 to "أسرع", 100 to "معتاد", 150 to "أمهل", 220 to "تدبُّر").forEach { (pct, label) ->
                        androidx.compose.material3.FilterChip(
                            selected = scrollSpeed == pct,
                            onClick = { vm.setScrollSpeed(pct) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        }

        // اختيار رواية النصّ (ورش/حفص/قالون/الدوري) — غير حفص يُجلب مرّةً ويُخزَّن.
        if (riwayat.isNotEmpty()) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("الرواية:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    riwayat.forEach { r ->
                        androidx.compose.material3.FilterChip(
                            selected = riwaya?.id == r.id,
                            onClick = { vm.pickRiwaya(r) },
                            label = { Text(r.ar) },
                            enabled = !textLoading,
                        )
                    }
                    if (textLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            }
        }

        if (showSearch && shownAyat.isEmpty() && searchQ.isNotBlank()) {
            Text("لا آيةَ تطابق «$searchQ» في هذه السورة.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.outline)
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(shownAyat, key = { it.n }) { a ->
                val isPlaying = a.n == playingAyah                 // الآية الجاريّة أثناء التلاوة (أخضر)
                val isResume = playingAyah == 0 && a.n == target && target > 1  // موضع المتابعة (لونٌ مميّز)
                val container = when {
                    isPlaying -> MaterialTheme.colorScheme.primaryContainer
                    isResume -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> null
                }
                val onContainer = when {
                    isPlaying -> MaterialTheme.colorScheme.onPrimaryContainer
                    isResume -> MaterialTheme.colorScheme.onTertiaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Card(
                    // النقر يحدّد الآية ويحفظ الموضع (بلا تشغيل). إن كانت التلاوة جاريةً يقفز إليها.
                    Modifier.fillMaxWidth().clickable {
                        vm.setTarget(a.n)
                        if (here) QuranAudio.seekAyah(ctx, a.n)
                    },
                    colors = if (container != null)
                        androidx.compose.material3.CardDefaults.cardColors(containerColor = container)
                    else androidx.compose.material3.CardDefaults.cardColors(),
                ) {
                    Row(Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier.size(30.dp).clip(CircleShape)
                                .background(if (container != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${a.n}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                                color = if (container != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text(
                            a.text, fontFamily = AmiriQuran, fontSize = 23.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold,
                            color = onContainer,
                            modifier = Modifier.weight(1f),
                        )
                        // إشارةٌ مرجعيّة: نقرةٌ تحفظ الآية/تزيلها.
                        val isMarked = a.n in bookmarked
                        IconButton(onClick = { vm.toggleBookmark(a.n) }, modifier = Modifier.size(34.dp)) {
                            Icon(
                                if (isMarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isMarked) "إزالة الإشارة" else "إضافة إشارة",
                                tint = if (isMarked) MaterialTheme.colorScheme.primary else onContainer.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }
        }
    }
    androidx.compose.material3.SnackbarHost(readerSnackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (confirmDeleteAyat) {
        DeleteConfirmDialog("آيات ${surah?.ar ?: ""}".trim(), onCancel = { confirmDeleteAyat = false }) {
            confirmDeleteAyat = false
            ayatDownloaded = false   // إخفاءٌ فوريّ؛ لا يُحذف الملفّ إلّا بعد انقضاء مهلة التراجع.
            val rid = reciter?.id
            readerScope.launch {
                val res = readerSnackbar.showSnackbar("حُذف صوت آيات السورة", actionLabel = "تراجع", duration = androidx.compose.material3.SnackbarDuration.Long)
                if (res == androidx.compose.material3.SnackbarResult.ActionPerformed) ayatDownloaded = true
                else rid?.let { vm.deleteAyat(it) }
            }
        }
    }
}

/* ============================ ② القرآن المسموع — قائمة القرّاء ============================ */

@Composable
fun AudioRecitersScreen(
    vm: QuranMetaViewModel = hiltViewModel(),
    onOpenReciter: (String) -> Unit,
    onOpenDownloaded: () -> Unit,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val reciters by vm.surahReciters.collectAsStateWithLifecycle()
    val audioResume by vm.audioResume.collectAsStateWithLifecycle()
    var askResume by remember { mutableStateOf(false) }   // حوار: إتمام الاستماع أم البدء من جديد
    LaunchedEffect(Unit) { vm.ensureOnlineReciters() }   // جلبُ كامل القائمة عند فتح القسم فقط
    var riwayaFilter by remember { mutableStateOf("all") }
    var showSearch by remember { mutableStateOf(false) }
    var q by remember { mutableStateOf("") }
    val hasDownloads = remember(reciters) { vm.downloadedByReciter().isNotEmpty() }
    val shown = remember(reciters, riwayaFilter, q) {
        val nq = io.github.salehgnutux.gtsalat.domain.Quran.normalize(q)
        reciters.filter {
            (riwayaFilter == "all" || it.riwaya == riwayaFilter) &&
                (nq.isBlank() || io.github.salehgnutux.gtsalat.domain.Quran.normalize(it.ar).contains(nq))
        }
    }
    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("القرآن المسموع", onBack, actions = {
            IconButton(onClick = { showSearch = !showSearch; if (!showSearch) q = "" }) {
                Icon(if (showSearch) Icons.Filled.Close else Icons.Filled.Search, contentDescription = "بحث عن قارئ")
            }
        })
        if (showSearch) SearchField(q, "ابحث عن قارئ…") { q = it }
        Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf("all" to "الكلّ", "warsh" to "ورش", "hafs" to "حفص").forEach { (id, label) ->
                    androidx.compose.material3.FilterChip(riwayaFilter == id, { riwayaFilter = id }, { Text(label) })
                }
                Text("${shown.size} قارئاً", Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            audioResume?.let { ar ->
                item("audio_resume") {
                    val m = ar.posMs / 60_000L; val sec = (ar.posMs / 1000L) % 60L
                    val at = String.format(java.util.Locale.US, "%d:%02d", m, sec)
                    Card(
                        Modifier.fillMaxWidth().clickable { askResume = true },
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Icon(Icons.Filled.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Column(Modifier.weight(1f)) {
                                Text("متابعة الاستماع", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(
                                    "${ar.surahName}${if (ar.reciterName.isNotBlank()) " · ${ar.reciterName}" else ""} — $at",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
            if (hasDownloads) item("downloaded") {
                Card(
                    Modifier.fillMaxWidth().clickable { onOpenDownloaded() },
                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(Icons.Filled.DownloadDone, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Text("السور المُنزَّلة (دون إنترنت)", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
            items(shown, key = { it.id }) { r ->
                Card(Modifier.fillMaxWidth().clickable { onOpenReciter(r.id) }) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Headphones, null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text(r.ar, fontFamily = AmiriQuran, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("رواية ${riwayaLabel(r.riwaya)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }

    // حوار المتابعة: عند النقر على بطاقة «متابعة الاستماع» يختار المستخدم الإتمام أو البدء من جديد.
    if (askResume) audioResume?.let { ar ->
        val m = ar.posMs / 60_000L; val sec = (ar.posMs / 1000L) % 60L
        val at = String.format(java.util.Locale.US, "%d:%02d", m, sec)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { askResume = false },
            title = { Text("متابعة الاستماع") },
            text = {
                Text("${ar.surahName}${if (ar.reciterName.isNotBlank()) " · ${ar.reciterName}" else ""}\nهل تتابع من $at أم تبدأ من جديد؟")
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    askResume = false
                    QuranAudio.playSurah(ctx, ar.surah, ar.surahName, ar.reciterId, ar.reciterName, ar.server, ar.posMs)
                }) { Text("إتمام الاستماع") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    askResume = false
                    QuranAudio.playSurah(ctx, ar.surah, ar.surahName, ar.reciterId, ar.reciterName, ar.server, 0L)
                }) { Text("من البداية") }
            },
        )
    }
}

/* ============================ صفحة قارئٍ بعينه (سوره + تنزيل/حذف) ============================ */

@Composable
fun ReciterSurahsScreen(reciterId: String, vm: QuranMetaViewModel = hiltViewModel(), onBack: () -> Unit) {
    val ctx = LocalContext.current
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val reciters by vm.surahReciters.collectAsStateWithLifecycle()
    val play by QuranPlayback.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.ensureOnlineReciters() }
    // اسم القارئ: من قائمة الشبكة، وإلّا المُضمَّنون، وإلّا البطاقة المحفوظة على القرص (دون إنترنت).
    val reciter = remember(reciters, reciterId) {
        reciters.firstOrNull { it.id == reciterId } ?: vm.surahReciterById(reciterId) ?: vm.savedReciterMeta(reciterId)
    }

    var showSearch by remember { mutableStateOf(false) }
    var q by remember { mutableStateOf("") }
    val shownSurahs = remember(surahs, q) { filterSurahs(surahs, q) }

    var downloaded by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var progress by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }   // surah → نسبة التنزيل
    LaunchedEffect(reciterId) { downloaded = vm.downloadedSurahs(reciterId) }

    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()

    // تمريرٌ تلقائيٌّ إلى السورة الجاريّة عند فتح صفحة القارئ من المشغّل.
    LaunchedEffect(play.surah, play.mode, shownSurahs) {
        if (play.active && play.mode == QuranMode.SURAH && play.reciterId == reciterId) {
            val idx = shownSurahs.indexOfFirst { it.n == play.surah }
            if (idx >= 0) runCatching { listState.animateScrollToItem(idx) }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            SubScreenHeader(reciter?.ar ?: "القارئ", onBack, actions = {
                IconButton(onClick = { showSearch = !showSearch; if (!showSearch) q = "" }) {
                    Icon(if (showSearch) Icons.Filled.Close else Icons.Filled.Search, contentDescription = "بحث")
                }
            })
            if (showSearch) SurahSearchField(q) { q = it }
            reciter?.let { Text("رواية ${riwayaLabel(it.riwaya)}", Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) }
            LazyColumn(state = listState, modifier = Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shownSurahs, key = { it.n }) { s ->
                    val playingThis = play.active && play.mode == QuranMode.SURAH && play.surah == s.n
                    SurahAudioRow(
                        n = s.n, name = s.ar, playing = playingThis && play.isPlaying,
                        downloaded = s.n in downloaded, progress = progress[s.n],
                        onPlay = { reciter?.let { QuranAudio.playSurah(ctx, s.n, s.ar, it.id, it.ar, it.server) } },
                        onDownload = {
                            reciter?.let { r ->
                                vm.saveReciterMeta(r)   // نحفظ اسم القارئ ليظهر لاحقاً دون شبكة
                                progress = progress + (s.n to 0)
                                vm.downloadSurah(r.id, r.server, s.n, onProgress = { p -> progress = progress + (s.n to p) }) { ok ->
                                    progress = progress - s.n
                                    if (ok) downloaded = downloaded + s.n
                                }
                            }
                        },
                        onDelete = { pendingDelete = s.n },
                        highlight = playingThis,
                    )
                }
            }
        }
        androidx.compose.material3.SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    pendingDelete?.let { sn ->
        val name = surahs.firstOrNull { it.n == sn }?.ar ?: "$sn"
        DeleteConfirmDialog(name, onCancel = { pendingDelete = null }) {
            pendingDelete = null
            downloaded = downloaded - sn   // إخفاءٌ فوريّ؛ لا يُحذف الملفّ إلّا بعد انقضاء المهلة.
            scope.launch {
                val res = snackbar.showSnackbar("حُذف تنزيل سورة $name", actionLabel = "تراجع", duration = androidx.compose.material3.SnackbarDuration.Long)
                if (res == androidx.compose.material3.SnackbarResult.ActionPerformed) downloaded = downloaded + sn
                else vm.deleteSurah(reciterId, sn)
            }
        }
    }
}

/* ============================ استعراض السور المُنزَّلة (عبر مختلف القرّاء) ============================ */

@Composable
fun DownloadedSurahsScreen(vm: QuranMetaViewModel = hiltViewModel(), onOpenReciter: (String) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val play by QuranPlayback.state.collectAsStateWithLifecycle()
    // نراقب قائمة القرّاء (تفاعليّاً) ليظهر اسم القارئ فور تحميلها لا مُعرّفه الخام.
    val reciters by vm.surahReciters.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.ensureOnlineReciters() }
    var refresh by remember { mutableIntStateOf(0) }
    val byReciter = remember(refresh) { vm.downloadedByReciter() }
    // متى تأكّد اسمُ قارئٍ من قائمة الشبكة، نحفظه بجانب تنزيلاته ليبقى ظاهراً دون إنترنت مستقبلاً.
    LaunchedEffect(reciters, byReciter) {
        byReciter.keys.forEach { rid -> reciters.firstOrNull { it.id == rid }?.let { vm.saveReciterMeta(it) } }
    }

    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var hidden by remember { mutableStateOf<Set<String>>(emptySet()) }   // "rid-sn" المخفيّة (بانتظار مهلة الحذف)
    fun surahName(n: Int) = surahs.firstOrNull { it.n == n }?.ar ?: "$n"

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            SubScreenHeader("السور المُنزَّلة", onBack)
            if (byReciter.isEmpty()) {
                Text("لا سورَ منزَّلةٌ بعد. نزّل سورةً من صفحة أيّ قارئ.", Modifier.fillMaxWidth().padding(24.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.outline)
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                byReciter.forEach { (rid, surahNums) ->
                    val r = reciters.firstOrNull { it.id == rid } ?: vm.surahReciterById(rid) ?: vm.savedReciterMeta(rid)
                    val visible = surahNums.sorted().filter { "$rid-$it" !in hidden }
                    if (visible.isNotEmpty()) {
                        item("h_$rid") {
                            Row(Modifier.fillMaxWidth().clickable { onOpenReciter(rid) }.padding(vertical = 8.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Headphones, null, tint = MaterialTheme.colorScheme.primary)
                                Text("  ${r?.ar ?: rid}${r?.let { " · ${riwayaLabel(it.riwaya)}" } ?: ""}", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                        items(visible, key = { "$rid-$it" }) { sn ->
                            val playingThis = play.active && play.mode == QuranMode.SURAH && play.surah == sn && play.reciterId == rid
                            SurahAudioRow(
                                n = sn, name = surahName(sn), playing = playingThis && play.isPlaying, downloaded = true, progress = null,
                                onPlay = { QuranAudio.playSurah(ctx, sn, surahName(sn), rid, r?.ar ?: rid, r?.server ?: "") },
                                onDownload = {}, onDelete = { pendingDelete = rid to sn }, highlight = playingThis,
                            )
                        }
                    }
                }
            }
        }
        androidx.compose.material3.SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    pendingDelete?.let { (rid, sn) ->
        val name = surahName(sn)
        val key = "$rid-$sn"
        DeleteConfirmDialog(name, onCancel = { pendingDelete = null }) {
            pendingDelete = null
            hidden = hidden + key           // إخفاءٌ فوريّ؛ لا يُحذف الملفّ إلّا بعد انقضاء المهلة.
            scope.launch {
                val res = snackbar.showSnackbar("حُذف تنزيل سورة $name", actionLabel = "تراجع", duration = androidx.compose.material3.SnackbarDuration.Long)
                if (res == androidx.compose.material3.SnackbarResult.ActionPerformed) hidden = hidden - key
                else { vm.deleteSurah(rid, sn); hidden = hidden - key; refresh++ }
            }
        }
    }
}

/** صفٌّ لسورةٍ في القرآن المسموع: رقم + اسم + تنزيل(بنسبة)/حذف + تشغيل. */
@Composable
private fun SurahAudioRow(
    n: Int, name: String, playing: Boolean,
    downloaded: Boolean, progress: Int?,   // progress=null: لا تنزيل جارٍ · 0..100: النسبة
    onPlay: () -> Unit, onDownload: () -> Unit, onDelete: () -> Unit, highlight: Boolean,
) {
    Card(
        Modifier.fillMaxWidth().clickable { onPlay() },
        colors = if (highlight) androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else androidx.compose.material3.CardDefaults.cardColors(),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Text("$n", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
            Text(name, fontFamily = AmiriQuran, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            when {
                progress != null -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("$progress٪", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    if (progress <= 0) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else CircularProgressIndicator(progress = { progress / 100f }, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
                downloaded -> IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "حذف التنزيل", tint = MaterialTheme.colorScheme.error) }
                else -> IconButton(onClick = onDownload) { Icon(Icons.Filled.Download, "تنزيل", tint = MaterialTheme.colorScheme.outline) }
            }
            Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun DeleteConfirmDialog(name: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("حذف التنزيل") },
        text = { Text("حذف تنزيل سورة $name؟") },
        confirmButton = { androidx.compose.material3.Button(onClick = onConfirm) { Text("حذف") } },
        dismissButton = { FilledTonalButton(onClick = onCancel) { Text("إلغاء") } },
    )
}

/** حقل بحثٍ عامٌّ بتلميحٍ مخصّص. */
@Composable
private fun SearchField(q: String, placeholder: String, onChange: (String) -> Unit) {
    androidx.compose.material3.OutlinedTextField(
        value = q, onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        placeholder = { Text(placeholder, maxLines = 1) },
        textStyle = MaterialTheme.typography.bodyMedium,
        leadingIcon = { Icon(Icons.Filled.Search, null) },
        trailingIcon = { if (q.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Filled.Close, "مسح") } },
        singleLine = true, shape = RoundedCornerShape(28.dp),
    )
}

/* ============================ المشغّل المصغّر العائم (عالميّ) ============================ */

/**
 * شريطُ تشغيلٍ مصغّرٌ يظهر فوق الشريط السفليّ متى كانت تلاوةٌ جاريةً في أيّ قسم.
 * النقر على متنه **يعيدك إلى موضع القراءة/الاستماع** (السورة والآية الجاريّة).
 */
@Composable
fun QuranMiniPlayer(onOpen: (String) -> Unit, vm: QuranMetaViewModel = hiltViewModel()) {
    val ctx = LocalContext.current
    val play by QuranPlayback.state.collectAsStateWithLifecycle()
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    if (!play.active) return
    // اسم السورة من الفهرس (يصحّ مع التتابع التلقائيّ والمُنزَّل حيث لا يمرّر الاسم).
    val surahName = remember(surahs, play.surah) {
        surahs.firstOrNull { it.n == play.surah }?.ar ?: play.surahName.ifBlank { "${play.surah}" }
    }
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shadowElevation = 8.dp) {
        Row(
            Modifier.fillMaxWidth()
                .clickable {
                    onOpen(
                        when {
                            play.mode == QuranMode.AYAH -> "quran_read/${play.surah}"
                            play.reciterId.isNotBlank() -> "quran_reciter/${play.reciterId}"
                            else -> "quran_audio"
                        },
                    )
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Outlined.Headphones, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Column(Modifier.weight(1f)) {
                Text(
                    "سورة $surahName",
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
            // في التلاوة الكاملة: تنقّلٌ بين السور.
            if (play.mode == QuranMode.SURAH) {
                // في RTL: «السابقة» تشير يميناً، فنستعمل أيقونة SkipNext (والعكس للتالية).
                FilledIconButton(onClick = { QuranAudio.prev(ctx) }) { Icon(Icons.Filled.SkipNext, "السابقة") }
            }
            FilledIconButton(onClick = { QuranAudio.toggle(ctx) }) {
                Icon(if (play.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "تشغيل")
            }
            if (play.mode == QuranMode.SURAH) {
                // في RTL: «التالية» تشير يساراً، فنستعمل أيقونة SkipPrevious.
                FilledIconButton(onClick = { QuranAudio.next(ctx) }) { Icon(Icons.Filled.SkipPrevious, "التالية") }
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

/** حقل بحث السور (يظهر عند النقر على زرّ البحث). */
@Composable
private fun SurahSearchField(q: String, ayahSearch: Boolean = false, onChange: (String) -> Unit) {
    androidx.compose.material3.OutlinedTextField(
        value = q,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        placeholder = { Text(if (ayahSearch) "ابحث باسم سورةٍ أو كلمةٍ داخل الآيات…" else "ابحث بالاسم أو رقم السورة…", maxLines = 1) },
        textStyle = MaterialTheme.typography.bodyMedium,
        leadingIcon = { Icon(Icons.Filled.Search, null) },
        trailingIcon = { if (q.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Filled.Close, "مسح") } },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
    )
}

/** تطبيعٌ خفيفٌ للعربيّة ليكون البحث شاملاً (إزالة تشكيل + توحيد الألف/الياء/التاء المربوطة). */
private fun normalizeAr(s: String): String = s
    .replace("[\\u064B-\\u0652\\u0640]".toRegex(), "")
    .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ى', 'ي').replace('ة', 'ه')
    .trim().lowercase()

/** تصفية السور بالاسم أو الأسماء البديلة أو الاسم الإنجليزيّ أو الرقم. */
private fun filterSurahs(list: List<SurahMeta>, q: String): List<SurahMeta> {
    val query = normalizeAr(q)
    if (query.isBlank()) return list
    return list.filter { s ->
        normalizeAr(s.ar).contains(query) ||
            s.en.lowercase().contains(query) ||
            s.n.toString() == q.trim() ||
            s.aliases.any { normalizeAr(it).contains(query) }
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
