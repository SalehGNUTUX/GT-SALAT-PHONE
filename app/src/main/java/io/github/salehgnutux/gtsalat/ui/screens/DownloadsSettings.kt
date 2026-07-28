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
    fun downloadMushaf() = viewModelScope.launch { downloader.downloadMushaf() }
    fun deleteMushaf() = downloader.deleteMushaf()
    fun mushafCount() = downloader.mushafDownloadedCount()
}

/** محتوى مطويّة «تنزيل المحتوى» في الإعدادات — للاستخدام دون إنترنت. */
@Composable
fun DownloadsSectionContent(vm: DownloadsViewModel = hiltViewModel()) {
    val state by vm.mushaf.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf(false) }   // في مهلة التراجع

    // بعد انقضاء مهلة التراجع يُحذف المصحف فعليّاً.
    LaunchedEffect(pendingDelete) {
        if (pendingDelete) {
            kotlinx.coroutines.delay(6000)
            if (pendingDelete) { vm.deleteMushaf(); pendingDelete = false }
        }
    }

    val count = if (pendingDelete) 0 else vm.mushafCount()

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "نزّل المحتوى ليعمل دون إنترنت. الصوت (السور) يُنزَّل من قسم «القرآن المسموع» بزرّ التنزيل بجانب كلّ سورة.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )

        // المصحف المصوَّر (604 صفحة)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("المصحف المصوَّر", fontWeight = FontWeight.Bold)
                val done = if (state.running) state.done else count
                Text(
                    when {
                        state.running -> "جارٍ التنزيل… $done / ${state.total}"
                        done >= state.total -> "مُنزَّلٌ كاملاً ($done صفحة)"
                        done > 0 -> "مُنزَّلٌ جزئيّاً: $done / ${state.total}"
                        else -> "604 صفحة (مصحف المدينة، حفص) — للتصفّح دون إنترنت"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            when {
                state.running -> CircularProgressIndicator(Modifier.padding(start = 8.dp).size(28.dp), strokeWidth = 3.dp)
                count > 0 -> IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.Delete, "حذف المصحف", tint = MaterialTheme.colorScheme.error)
                }
                else -> Button(onClick = { vm.downloadMushaf() }) {
                    Icon(Icons.Filled.Download, null, Modifier.size(18.dp))
                    Text("  تنزيل")
                }
            }
        }
        if (state.running) {
            LinearProgressIndicator(
                progress = { (state.done.toFloat() / state.total).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // شريطُ تراجعٍ مضمَّنٌ أثناء المهلة (لا يُحذف الملفّ إلّا بعد انقضائها).
        if (pendingDelete) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("سيُحذف المصحف…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                FilledTonalButton(onClick = { pendingDelete = false }) { Text("تراجع") }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف المصحف المصوَّر") },
            text = { Text("حذف كلّ صفحات المصحف المُنزَّلة؟") },
            confirmButton = { Button(onClick = { confirmDelete = false; pendingDelete = true }) { Text("حذف") } },
            dismissButton = { FilledTonalButton(onClick = { confirmDelete = false }) { Text("إلغاء") } },
        )
    }
}
