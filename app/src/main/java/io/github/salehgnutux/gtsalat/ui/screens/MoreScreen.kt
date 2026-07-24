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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** محور «المزيد»: بطاقات أقسام الميزات الإضافيّة. المتاح الآن يُفتح، والقادم مُعطَّل بوسمٍ «قريباً». */
private data class Feature(
    val label: String,
    val icon: ImageVector,
    val route: String?,     // null = لم يُنجَز بعد
)

@Composable
fun MoreScreen(onOpen: (String) -> Unit) {
    val features = listOf(
        Feature("أذكار الصباح", Icons.Outlined.WbSunny, "adhkar_session/morning"),
        Feature("أذكار المساء", Icons.Outlined.NightsStay, "adhkar_session/evening"),
        Feature("الأذكار العامّة", Icons.Outlined.FavoriteBorder, "adhkar"),
        Feature("الأدعية المأثورة", Icons.Outlined.VolunteerActivism, "duas"),
        Feature("الأربعون والأحاديث", Icons.Outlined.MenuBook, "hadith"),
        Feature("حِكَم ومواعظ", Icons.Outlined.FormatQuote, "hikam"),
        Feature("التسبيح", Icons.Outlined.Fingerprint, "tasbih"),
        Feature("أسماء الله الحسنى", Icons.Outlined.AutoAwesome, "asma"),
        Feature("المصحف", Icons.Outlined.AutoStories, null),
        Feature("الإذاعات", Icons.Outlined.Radio, null),
    )

    Column(Modifier.fillMaxSize()) {
        Text(
            "المزيد",
            Modifier.fillMaxWidth().padding(20.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(features) { f -> FeatureCard(f, onOpen) }
        }
    }
}

@Composable
private fun FeatureCard(f: Feature, onOpen: (String) -> Unit) {
    val enabled = f.route != null
    Card(
        Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable { onOpen(f.route!!) } else Modifier),
    ) {
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
    }
}
