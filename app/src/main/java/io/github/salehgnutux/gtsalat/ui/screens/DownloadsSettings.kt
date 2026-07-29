package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.salehgnutux.gtsalat.data.QuranDownloader
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(private val downloader: QuranDownloader) : ViewModel() {
    val mushaf = downloader.mushaf
    fun downloadMushaf(riwaya: String) = viewModelScope.launch { downloader.downloadMushaf(riwaya) }
    fun deleteMushaf(riwaya: String) = downloader.deleteMushaf(riwaya)
    fun mushafCount(riwaya: String) = downloader.mushafDownloadedCount(riwaya)
}

/** محتوى مطويّة «تنزيل المحتوى» في الإعدادات — للاستخدام دون إنترنت. */
@Composable
fun DownloadsSectionContent(vm: DownloadsViewModel = hiltViewModel()) {
    val state by vm.mushaf.collectAsStateWithLifecycle()
    var riwaya by remember { mutableStateOf("hafs") }
    var confirmDelete by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }   // الرواية في مهلة التراجع

    // بعد انقضاء مهلة التراجع يُحذف مصحف الرواية فعليّاً.
    LaunchedEffect(pendingDelete) {
        val r = pendingDelete
        if (r != null) {
            kotlinx.coroutines.delay(6000)
            if (pendingDelete == r) { vm.deleteMushaf(r); pendingDelete = null }
        }
    }

    val runningThis = state.running && state.riwaya == riwaya
    val count = if (pendingDelete == riwaya) 0 else vm.mushafCount(riwaya)

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "نزّل المحتوى ليعمل دون إنترنت. الصوت (السور) يُنزَّل من قسم «القرآن المسموع» بزرّ التنزيل بجانب كلّ سورة.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )

        Text("المصحف المصوَّر", fontWeight = FontWeight.Bold)
        // مبدّل الرواية للتنزيل.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.FilterChip(riwaya == "hafs", { riwaya = "hafs" }, { Text("حفص") }, enabled = !state.running)
            androidx.compose.material3.FilterChip(riwaya == "warsh", { riwaya = "warsh" }, { Text("ورش") }, enabled = !state.running)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val done = if (runningThis) state.done else count
                Text(
                    when {
                        runningThis -> "جارٍ تنزيل مصحف ${riwayaName(riwaya)}… $done / ${state.total}"
                        done >= state.total -> "مصحف ${riwayaName(riwaya)}: مُنزَّلٌ كاملاً"
                        done > 0 -> "مصحف ${riwayaName(riwaya)}: مُنزَّلٌ جزئيّاً ($done / ${state.total})"
                        else -> "604 صفحة (${riwayaName(riwaya)}) — للتصفّح دون إنترنت"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // إكمال التنزيل الجزئيّ (يتخطّى الصفحات الموجودة).
                if (!state.running && count in 1 until state.total) {
                    FilledTonalButton(onClick = { vm.downloadMushaf(riwaya) }) {
                        Icon(Icons.Filled.Download, null, Modifier.size(16.dp)); Text(" إكمال")
                    }
                }
                when {
                    runningThis -> CircularProgressIndicator(Modifier.padding(start = 8.dp).size(28.dp), strokeWidth = 3.dp)
                    count > 0 -> IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, "حذف المصحف", tint = MaterialTheme.colorScheme.error)
                    }
                    else -> Button(onClick = { vm.downloadMushaf(riwaya) }, enabled = !state.running) {
                        Icon(Icons.Filled.Download, null, Modifier.size(18.dp))
                        Text("  تنزيل")
                    }
                }
            }
        }
        if (runningThis) {
            LinearProgressIndicator(
                progress = { (state.done.toFloat() / state.total).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (pendingDelete == riwaya) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("سيُحذف مصحف ${riwayaName(riwaya)}…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                FilledTonalButton(onClick = { pendingDelete = null }) { Text("تراجع") }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف المصحف المصوَّر") },
            text = { Text("حذف كلّ صفحات مصحف ${riwayaName(riwaya)} المُنزَّلة؟") },
            confirmButton = { Button(onClick = { confirmDelete = false; pendingDelete = riwaya }) { Text("حذف") } },
            dismissButton = { FilledTonalButton(onClick = { confirmDelete = false }) { Text("إلغاء") } },
        )
    }
}

private fun riwayaName(id: String) = if (id == "warsh") "ورش" else "حفص"
