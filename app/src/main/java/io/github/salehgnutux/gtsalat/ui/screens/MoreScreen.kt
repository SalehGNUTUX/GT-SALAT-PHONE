package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** محور «المزيد»: بطاقات أقسام الميزات الإضافيّة. المتاح الآن يُفتح، والقادم مُعطَّل بوسمٍ «قريباً». */
private data class Feature(
    val label: String,
    val icon: ImageVector,
    val route: String?,     // null = لم يُنجَز بعد
)

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
) : ViewModel() {
    val favorites: StateFlow<Set<String>> = settingsRepo.settings
        .map { it.favoriteFeatures }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggle(route: String) = viewModelScope.launch { settingsRepo.toggleFavoriteFeature(route) }
}

@Composable
fun MoreScreen(onOpen: (String) -> Unit, vm: MoreViewModel = hiltViewModel()) {
    // مرتّبةٌ حسب الأولويّة الأصليّة: القرآن أوّلاً، ثمّ حصن المسلم، ثمّ الأذكار…
    val features = remember {
        listOf(
            Feature("القرآن الكريم", Icons.Outlined.AutoStories, "quran"),
            Feature("حصن المسلم", Icons.Outlined.Shield, "hisn"),
            Feature("أذكار الصباح", Icons.Outlined.WbSunny, "adhkar_session/morning"),
            Feature("أذكار المساء", Icons.Outlined.NightsStay, "adhkar_session/evening"),
            Feature("التسبيح", Icons.Outlined.Fingerprint, "tasbih"),
            Feature("الأدعية المأثورة", Icons.Outlined.VolunteerActivism, "duas"),
            Feature("التفسير الميسّر", Icons.Outlined.Notes, "tafsir"),
            Feature("الأربعون والأحاديث", Icons.Outlined.MenuBook, "hadith"),
            Feature("أسماء الله الحسنى", Icons.Outlined.AutoAwesome, "asma"),
            Feature("حِكَم ومواعظ", Icons.Outlined.FormatQuote, "hikam"),
            Feature("أحداثٌ تاريخيّة", Icons.Outlined.HistoryEdu, "events"),
            Feature("الإذاعات", Icons.Outlined.Radio, "radios"),
            Feature("إمساكيّة رمضان", Icons.Outlined.DarkMode, "imsakiah"),
        )
    }
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    // المفضّلة تصعد للأعلى، وكلا المجموعتين تحفظان ترتيبهما الأصليّ (stable partition).
    val ordered = remember(features, favorites) {
        features.filter { it.route != null && it.route in favorites } +
            features.filter { it.route == null || it.route !in favorites }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "المزيد",
                Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            ThemeToggleButton()
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(ordered, key = { it.route ?: it.label }) { f ->
                FeatureCard(f, isFav = f.route != null && f.route in favorites, onOpen = onOpen, onToggleFav = { f.route?.let(vm::toggle) })
            }
        }
    }
}

@Composable
private fun FeatureCard(f: Feature, isFav: Boolean, onOpen: (String) -> Unit, onToggleFav: () -> Unit) {
    val enabled = f.route != null
    Card(
        Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable { onOpen(f.route!!) } else Modifier),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 22.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape)
                        .background(if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        f.icon,
                        contentDescription = null,
                        tint = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline,
                    )
                }
                Text(
                    f.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                )
                if (!enabled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("قريباً", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
            // نجمة التفضيل (للأقسام المتاحة فقط) — النقر عليها يُفضّل ولا يفتح البطاقة.
            if (enabled) {
                IconButton(onClick = onToggleFav, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(
                        if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (isFav) "إزالة من المفضّلة" else "إضافة إلى المفضّلة",
                        tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}
