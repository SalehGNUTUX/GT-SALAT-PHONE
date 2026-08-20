package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import io.github.salehgnutux.gtsalat.audio.AdhkarAudio
import io.github.salehgnutux.gtsalat.audio.AdhkarPlayback

/**
 * مشغّلٌ عائمٌ نحيفٌ لصوت الأذكار — يبقى ظاهراً عبر الأقسام أثناء التشغيل (فوق الشريط السفليّ)،
 * وبالنقر عليه يعود المستخدم إلى قسمه الأصليّ. أزراره: تشغيل/إيقاف مؤقّت · تشغيلٌ مستمرّ · إيقاف،
 * مع خطّ تقدّمٍ رفيعٍ في أعلاه. (لا يشغل مساحة النص كالبطاقة السابقة.)
 */
@Composable
fun AdhkarMiniPlayer(onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val play by AdhkarPlayback.state.collectAsStateWithLifecycle()
    if (!play.active) return
    val dur = play.durMs.coerceAtLeast(1)
    val progress = (play.posMs.toFloat() / dur).coerceIn(0f, 1f)

    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shadowElevation = 8.dp, modifier = modifier) {
        Column {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(3.dp),
            )
            Row(
                Modifier.fillMaxWidth().clickable { if (play.route.isNotBlank()) onOpen(play.route) }
                    .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(Icons.Filled.Headset, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    play.title,
                    Modifier.weight(1f).padding(horizontal = 6.dp).basicMarquee(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                )
                if (play.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                IconButton(onClick = { AdhkarAudio.toggleRepeat(ctx) }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.Repeat, "تشغيلٌ مستمرّ", Modifier.size(18.dp),
                        tint = if (play.repeat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    )
                }
                IconButton(onClick = { AdhkarAudio.toggle(ctx) }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        if (play.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        "تشغيل/إيقاف مؤقّت", Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                IconButton(onClick = { AdhkarAudio.stop(ctx) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Stop, "إيقاف", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/**
 * زرٌّ صغيرٌ في ترويسة القسم لبدء/إيقاف الاستماع لتسجيله الصوتيّ — التحكّم الكامل عبر المشغّل العائم.
 */
@Composable
fun AdhkarListenAction(sectionKey: String, title: String, route: String, rawResId: Int) {
    val ctx = LocalContext.current
    val play by AdhkarPlayback.state.collectAsStateWithLifecycle()
    val mine = play.active && play.key == sectionKey
    IconButton(onClick = {
        if (mine) AdhkarAudio.toggle(ctx) else AdhkarAudio.play(ctx, sectionKey, title, route, rawResId)
    }) {
        Icon(
            if (mine && play.isPlaying) Icons.Filled.Pause else Icons.Filled.Headset,
            contentDescription = "استماع صوتيّ",
            tint = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
