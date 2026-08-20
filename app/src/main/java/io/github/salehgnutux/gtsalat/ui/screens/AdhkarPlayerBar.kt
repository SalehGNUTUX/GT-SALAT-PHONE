package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.salehgnutux.gtsalat.audio.AdhkarAudio
import io.github.salehgnutux.gtsalat.audio.AdhkarPlayback

/**
 * شريط مشغّلٍ لتسجيل الأذكار الصوتيّ المضمَّن (يظهر أعلى القسم). [key] يميّز الملفّ (morning/evening/sleep)
 * فلا يظهر الشريط نشطاً إلّا في قسمه. التشغيل عبر خدمةٍ مقدّمة تعمل والشاشة مقفلة.
 */
@Composable
fun AdhkarPlayerBar(sectionKey: String, title: String, rawResId: Int, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val state by AdhkarPlayback.state.collectAsStateWithLifecycle()
    val mine = state.active && state.key == sectionKey

    // موضع المنزلق أثناء السحب (لا يُقاد من الحالة حتى يُفلِت المستخدم).
    var dragValue by remember { mutableFloatStateOf(-1f) }
    val dur = if (mine) state.durMs.coerceAtLeast(1) else 1
    val pos = if (mine) state.posMs.coerceIn(0, dur) else 0
    val sliderValue = if (dragValue >= 0f) dragValue else pos.toFloat() / dur

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                when {
                    mine && state.loading -> CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                    else -> IconButton(onClick = {
                        if (mine) AdhkarAudio.toggle(ctx) else AdhkarAudio.play(ctx, sectionKey, title, rawResId)
                    }) {
                        Icon(
                            if (mine && state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (mine && state.isPlaying) "إيقاف مؤقّت" else "تشغيل التلاوة",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(
                    "🎧 استماع: $title",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (mine) {
                    IconButton(onClick = { AdhkarAudio.stop(ctx) }) {
                        Icon(Icons.Filled.Stop, contentDescription = "إيقاف", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (mine) {
                Slider(
                    value = sliderValue.coerceIn(0f, 1f),
                    onValueChange = { dragValue = it },
                    onValueChangeFinished = {
                        if (dragValue >= 0f) { AdhkarAudio.seek(ctx, (dragValue * dur).toInt()); dragValue = -1f }
                    },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatMs(pos), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(formatMs(dur), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

/** تنسيق المدّة MM:SS بأرقامٍ غربيّة. */
private fun formatMs(ms: Int): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return String.format(java.util.Locale.US, "%d:%02d", totalSec / 60, totalSec % 60)
}
