package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    fun mushafCount() = downloader.mushafDownloadedCount()
}

/** محتوى مطويّة «تنزيل المحتوى» في الإعدادات — للاستخدام دون إنترنت. */
@Composable
fun DownloadsSectionContent(vm: DownloadsViewModel = hiltViewModel()) {
    val state by vm.mushaf.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "نزّل المحتوى ليعمل دون إنترنت. الصوت (السور) يُنزَّل من قسم «القرآن المسموع» بزرّ التنزيل بجانب كلّ سورة.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )

        // المصحف المصوَّر (604 صفحة)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("المصحف المصوَّر", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                val done = if (state.running) state.done else vm.mushafCount()
                Text(
                    when {
                        state.running -> "جارٍ التنزيل… $done / ${state.total}"
                        done >= state.total -> "مُنزَّلٌ كاملاً ($done صفحة)"
                        done > 0 -> "مُنزَّلٌ جزئيّاً: $done / ${state.total}"
                        else -> "604 صفحة (مصحف المدينة) — للتصفّح دون إنترنت"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            when {
                state.running -> CircularProgressIndicator(Modifier.padding(start = 8.dp).size(28.dp), strokeWidth = 3.dp)
                vm.mushafCount() >= state.total ->
                    Icon(Icons.Filled.DownloadDone, "مُنزَّل", tint = MaterialTheme.colorScheme.primary)
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
    }
}
