package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.salehgnutux.gtsalat.audio.RadioAudio
import io.github.salehgnutux.gtsalat.audio.RadioPlayback
import io.github.salehgnutux.gtsalat.data.RadioRepository
import io.github.salehgnutux.gtsalat.domain.RadioItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RadiosViewModel @Inject constructor(private val repo: RadioRepository) : ViewModel() {
    private val _radios = MutableStateFlow<List<RadioItem>>(emptyList())
    val radios: StateFlow<List<RadioItem>> = _radios.asStateFlow()
    init { reload() }
    fun reload() { viewModelScope.launch { _radios.value = repo.radios() } }
    fun setUrl(name: String, url: String) { repo.setUrl(name, url); reload() }
    fun resetUrl(name: String) { repo.resetUrl(name); reload() }
    fun resetAll() { repo.resetAll(); reload() }
    fun addCustom(name: String, desc: String, url: String) { repo.addCustom(name, desc, url); reload() }
    fun delete(item: RadioItem) { repo.delete(item.name, item.isCustom); reload() }
    fun restore(item: RadioItem) { repo.restore(item.name, item.isCustom, item.desc, item.url); reload() }
    fun toggleFav(name: String) { repo.toggleFav(name); reload() }
}

@Composable
fun RadiosScreen(onBack: () -> Unit, vm: RadiosViewModel = hiltViewModel()) {
    val ctx = LocalContext.current
    val radios by vm.radios.collectAsStateWithLifecycle()
    val play by RadioPlayback.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAdd by remember { mutableStateOf(false) }
    var confirmResetAll by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    val sleepPlay by RadioPlayback.state.collectAsStateWithLifecycle()
    var editItem by remember { mutableStateOf<RadioItem?>(null) }
    var confirmDelete by remember { mutableStateOf<RadioItem?>(null) }

    if (showSleepTimer) RadioSleepTimerDialog(sleepPlay) { showSleepTimer = false }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            SubScreenHeader("الإذاعات", onBack, actions = {
                IconButton(onClick = { showSleepTimer = true }) {
                    Icon(
                        Icons.Outlined.Timer,
                        "مؤقّت إيقاف البثّ",
                        tint = if (sleepPlay.sleepUntil > 0L) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = { confirmResetAll = true }) { Icon(Icons.Filled.Refresh, "إعادة الإذاعات الافتراضيّة", tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, "إضافة إذاعة", tint = MaterialTheme.colorScheme.primary) }
            })
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(radios, key = { it.name }) { r ->
                    val playingThis = play.active && play.url == r.url
                    RadioRow(
                        item = r, playing = playingThis && play.isPlaying, loading = playingThis && play.loading,
                        onPlay = { if (playingThis) RadioAudio.toggle(ctx) else RadioAudio.play(ctx, r.name, r.url) },
                        onToggleFav = { vm.toggleFav(r.name) },
                        onEditUrl = { editItem = r },
                        onResetUrl = { vm.resetUrl(r.name) },
                        onDelete = { confirmDelete = r },
                    )
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    // إضافة إذاعة مخصّصة
    if (showAdd) RadioEditDialog(title = "إضافة إذاعة", initName = "", initUrl = "", nameEditable = true, onCancel = { showAdd = false }) { n, u ->
        showAdd = false; if (n.isNotBlank() && u.isNotBlank()) vm.addCustom(n.trim(), "إذاعةٌ مخصّصة", u.trim())
    }
    // تعديل رابط إذاعة
    editItem?.let { it0 ->
        RadioEditDialog(title = "تعديل رابط «${it0.name}»", initName = it0.name, initUrl = it0.url, nameEditable = false, onCancel = { editItem = null }) { _, u ->
            editItem = null; if (u.isNotBlank()) vm.setUrl(it0.name, u.trim())
        }
    }
    // تأكيد إعادة الكلّ
    if (confirmResetAll) {
        AlertDialog(
            onDismissRequest = { confirmResetAll = false },
            title = { Text("إعادة الإذاعات الافتراضيّة") },
            text = { Text("إلغاء كلّ التعديلات والحذف واستعادة الإذاعات الافتراضيّة؟ (تبقى إذاعاتك المخصّصة.)") },
            confirmButton = { Button(onClick = { confirmResetAll = false; vm.resetAll() }) { Text("إعادة") } },
            dismissButton = { FilledTonalButton(onClick = { confirmResetAll = false }) { Text("إلغاء") } },
        )
    }
    // تأكيد الحذف + مهلة تراجع
    confirmDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("حذف الإذاعة") },
            text = { Text("حذف «${item.name}»؟") },
            confirmButton = {
                Button(onClick = {
                    confirmDelete = null
                    vm.delete(item)
                    scope.launch {
                        val res = snackbar.showSnackbar("حُذفت «${item.name}»", actionLabel = "تراجع", duration = SnackbarDuration.Long)
                        if (res == SnackbarResult.ActionPerformed) vm.restore(item)
                    }
                }) { Text("حذف") }
            },
            dismissButton = { FilledTonalButton(onClick = { confirmDelete = null }) { Text("إلغاء") } },
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun RadioRow(item: RadioItem, playing: Boolean, loading: Boolean, onPlay: () -> Unit, onToggleFav: () -> Unit, onEditUrl: () -> Unit, onResetUrl: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onPlay),
        colors = if (playing) androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else androidx.compose.material3.CardDefaults.cardColors(),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Outlined.Radio, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                // الأسماء الطويلة تُمرَّر أفقيّاً لتظهر كاملة.
                Text(
                    item.name + when { item.isCustom -> " (مخصّصة)"; item.isModified -> " (معدَّل)"; else -> "" },
                    Modifier.basicMarquee(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, maxLines = 1,
                )
                if (item.desc.isNotBlank()) Text(item.desc, Modifier.basicMarquee(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1)
            }
            IconButton(onClick = onToggleFav) {
                Icon(if (item.isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "المفضّلة", tint = if (item.isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            }
            if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            FilledIconButton(onClick = onPlay) { Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "تشغيل") }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "خيارات") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("تعديل الرابط") }, onClick = { menu = false; onEditUrl() })
                    if (item.isModified) DropdownMenuItem(text = { Text("إعادة الرابط الافتراضيّ") }, onClick = { menu = false; onResetUrl() })
                    DropdownMenuItem(text = { Text("حذف") }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

/**
 * مشغّلُ إذاعةٍ مصغّرٌ **عالميّ** (فوق الشريط السفليّ) — يبقى ظاهراً عبر كلّ الأقسام أثناء البثّ.
 * النقر على متنه يفتح قسم الإذاعات؛ وفيه السابقة/التالية للتنقّل بين الإذاعات.
 */
/** الدقائق المتبقّية من مؤقّت الإيقاف، بالتقريب إلى الأعلى. */
@Composable
private fun rememberSleepMinutesLeft(sleepUntil: Long): Int {
    var left by remember { mutableStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(sleepUntil) {
        while (sleepUntil > 0L) {
            val ms = sleepUntil - android.os.SystemClock.elapsedRealtime()
            left = ((ms + 59_999L) / 60_000L).toInt().coerceAtLeast(0)
            kotlinx.coroutines.delay(5_000L)
        }
        left = 0
    }
    return left
}

@Composable
private fun RadioSleepRemaining(sleepUntil: Long) {
    if (sleepUntil <= 0L) return
    val left = rememberSleepMinutesLeft(sleepUntil)
    Icon(Icons.Outlined.Timer, "مؤقّت الإيقاف", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
    Text("${left}د", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(start = 2.dp))
}

/** مؤقّت إيقاف البثّ: 15 أو 30 أو 45 دقيقة، أو مدّةٌ تُكتب يدويّاً. */
@Composable
private fun RadioSleepTimerDialog(play: io.github.salehgnutux.gtsalat.audio.RadioState, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var custom by remember { mutableStateOf("") }
    val customMinutes = custom.toIntOrNull()?.takeIf { it in 1..MAX_SLEEP_MINUTES }
    val left = rememberSleepMinutesLeft(play.sleepUntil)
    fun apply(minutes: Int) {
        RadioAudio.setSleepTimer(ctx, minutes)
        onDismiss()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("مؤقّت إيقاف البثّ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!play.active) {
                    Text("شغّل إذاعةً أوّلاً، ثمّ اضبط المؤقّت.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(
                        if (play.sleepUntil > 0L) "يتوقّف البثّ بعد $left دقيقة." else "يتوقّف البثّ تلقائيّاً بعد:",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 30, 45).forEach { m ->
                            androidx.compose.material3.FilledTonalButton(
                                onClick = { apply(m) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                            ) { Text("$m د") }
                        }
                    }
                    androidx.compose.material3.OutlinedTextField(
                        value = custom,
                        // أرقامٌ 0-9 حصراً، ولو أدخلتها لوحة المفاتيح بشكلٍ آخر.
                        onValueChange = { v -> custom = v.filter { it.isDigit() }.map { it.digitToInt() }.joinToString("").take(3) },
                        label = { Text("مدّةٌ أخرى بالدقائق") },
                        singleLine = true,
                        isError = custom.isNotEmpty() && customMinutes == null,
                        supportingText = { Text("من 1 إلى $MAX_SLEEP_MINUTES دقيقة") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            if (play.active) {
                Button(enabled = customMinutes != null, onClick = { customMinutes?.let(::apply) }) { Text("ضبط") }
            } else {
                Button(onClick = onDismiss) { Text("حسناً") }
            }
        },
        dismissButton = {
            if (play.sleepUntil > 0L) {
                androidx.compose.material3.TextButton(onClick = { apply(0) }) { Text("إلغاء المؤقّت") }
            } else if (play.active) {
                androidx.compose.material3.TextButton(onClick = onDismiss) { Text("إغلاق") }
            }
        },
    )
}

private const val MAX_SLEEP_MINUTES = 600

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun RadioMiniPlayer(onOpen: () -> Unit, vm: RadiosViewModel = hiltViewModel()) {
    val ctx = LocalContext.current
    val play by RadioPlayback.state.collectAsStateWithLifecycle()
    val radios by vm.radios.collectAsStateWithLifecycle()
    if (!play.active) return
    fun jump(delta: Int) {
        if (radios.isEmpty()) return
        val idx = radios.indexOfFirst { it.url == play.url }.let { if (it < 0) 0 else it }
        val next = radios[((idx + delta) % radios.size + radios.size) % radios.size]
        RadioAudio.play(ctx, next.name, next.url)
    }
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shadowElevation = 8.dp) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Outlined.Radio, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            RadioSleepRemaining(play.sleepUntil)
            Text(play.name, Modifier.weight(1f).padding(horizontal = 4.dp).basicMarquee(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 1)
            if (play.loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            // في RTL: السابقة تشير يميناً (SkipNext)، التالية يساراً (SkipPrevious).
            FilledIconButton(onClick = { jump(-1) }) { Icon(Icons.Filled.SkipNext, "السابقة") }
            FilledIconButton(onClick = { RadioAudio.toggle(ctx) }) { Icon(if (play.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "تشغيل") }
            FilledIconButton(onClick = { jump(1) }) { Icon(Icons.Filled.SkipPrevious, "التالية") }
            FilledIconButton(onClick = { RadioAudio.stop(ctx) }) { Icon(Icons.Filled.Stop, "إيقاف") }
        }
    }
}

@Composable
private fun RadioEditDialog(title: String, initName: String, initUrl: String, nameEditable: Boolean, onCancel: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf(initName) }
    var url by remember { mutableStateOf(initUrl) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (nameEditable) OutlinedTextField(name, { name = it }, label = { Text("اسم الإذاعة") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(url, { url = it }, label = { Text("رابط البثّ") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onSave(name, url) }) { Text("حفظ") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("إلغاء") } },
    )
}
