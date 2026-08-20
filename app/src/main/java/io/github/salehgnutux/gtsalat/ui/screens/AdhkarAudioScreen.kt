package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.salehgnutux.gtsalat.R
import io.github.salehgnutux.gtsalat.audio.AdhkarAudio
import io.github.salehgnutux.gtsalat.audio.AdhkarPlayback

/** فهرسُ الأذكار الصوتيّة المضمَّنة — أضِف تسجيلاتٍ جديدةً هنا لتظهر تلقائيّاً في القسم. */
data class AdhkarAudioTrack(
    val key: String,
    val title: String,
    val subtitle: String,
    val route: String,
    val rawResId: Int,
)

val ADHKAR_AUDIO_TRACKS = listOf(
    AdhkarAudioTrack("morning", "أذكار الصباح", "التسجيل الكامل · نحو ٢١ دقيقة", "adhkar_session/morning", R.raw.adhkar_morning),
    AdhkarAudioTrack("evening", "أذكار المساء", "التسجيل الكامل · نحو ٢١ دقيقة", "adhkar_session/evening", R.raw.adhkar_evening),
    AdhkarAudioTrack("sleep", "أذكار النوم", "التسجيل الكامل · نحو ١١ دقيقة", "hisn/2", R.raw.adhkar_sleep),
)

/** قسمٌ يجمع كلّ الأذكار الصوتيّة في مكانٍ واحد — التحكّم الكامل عبر المشغّل العائم. */
@Composable
fun AdhkarAudioScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val play by AdhkarPlayback.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("الأذكار الصوتية", onBack)
        Text(
            "استمع لتسجيلات الأذكار كاملةً — تعمل في الخلفيّة والشاشة مقفلة، وتبقى ظاهرةً في المشغّل السفليّ عبر الأقسام.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(ADHKAR_AUDIO_TRACKS) { t ->
                val mine = play.active && play.key == t.key
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                            Icon(
                                Icons.Filled.Headset, null,
                                Modifier.padding(8.dp).size(22.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(t.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                if (mine && play.isPlaying) "يُشغَّل الآن" else t.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            )
                        }
                        FilledIconButton(onClick = {
                            if (mine) AdhkarAudio.toggle(ctx) else AdhkarAudio.play(ctx, t.key, t.title, t.route, t.rawResId)
                        }) {
                            Icon(
                                if (mine && play.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "تشغيل ${t.title}",
                            )
                        }
                    }
                }
            }
        }
    }
}
