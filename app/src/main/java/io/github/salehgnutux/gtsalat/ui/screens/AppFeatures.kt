package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CleanHands
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** قسمٌ من أقسام «المزيد»؛ مصدرٌ واحدٌ لبطاقاتها ولخيارات اختصار الرئيسيّة. */
data class AppFeature(val label: String, val icon: ImageVector, val route: String)

object AppFeatures {
    const val DEFAULT_SHORTCUT = "radios"

    // الترتيب بحسب الاستعمال اليوميّ ثمّ الأهمّيّة: القرآن والإذاعات، ثمّ الأذكار اليوميّة، ثمّ الأدعية
    // والأحاديث، ثمّ التعلّم والرقية والموسميّ، ثمّ القراءة العامّة. والنجمة ترفع المفضّل فوق هذا كلّه.
    val all: List<AppFeature> = listOf(
        AppFeature("القرآن الكريم", Icons.Outlined.AutoStories, "quran"),
        AppFeature("الإذاعات", Icons.Outlined.Radio, "radios"),
        AppFeature("التفسير الميسّر", Icons.Outlined.Notes, "tafsir"),
        AppFeature("أذكار الصباح", Icons.Outlined.WbSunny, "adhkar_session/morning"),
        AppFeature("أذكار المساء", Icons.Outlined.NightsStay, "adhkar_session/evening"),
        AppFeature("أذكار النوم", Icons.Outlined.Bedtime, "hisn/2"),
        AppFeature("الأذكار الصوتية", Icons.Outlined.Headphones, "adhkar_audio"),
        AppFeature("حصن المسلم", Icons.Outlined.Shield, "hisn"),
        AppFeature("التسبيح", Icons.Outlined.Fingerprint, "tasbih"),
        AppFeature("الأدعية المأثورة", Icons.Outlined.VolunteerActivism, "duas"),
        AppFeature("الأربعون والأحاديث", Icons.Outlined.MenuBook, "hadith"),
        AppFeature("أسماء الله الحسنى", Icons.Outlined.AutoAwesome, "asma"),
        AppFeature("الطهارة والصلاة", Icons.Outlined.CleanHands, "learn_hub"),
        AppFeature("الرقية الشرعية", Icons.Outlined.Spa, "ruqyah_hub"),
        AppFeature("إمساكيّة رمضان", Icons.Outlined.DarkMode, "imsakiah"),
        AppFeature("حِكَم ومواعظ", Icons.Outlined.FormatQuote, "hikam"),
        AppFeature("أحداثٌ تاريخيّة", Icons.Outlined.HistoryEdu, "events"),
    )

    /** يسقط إلى الإذاعات إن كان المسار المحفوظ غير معروف. */
    fun byRoute(route: String): AppFeature =
        all.firstOrNull { it.route == route } ?: all.first { it.route == DEFAULT_SHORTCUT }
}

/** لوحة اختيار قسم الاختصار: تُفتَح بضغطةٍ مطوّلةٍ على الأيقونة، أو من الإعدادات. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutPickerSheet(current: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "اختصار أعلى الرئيسيّة",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        LazyColumn(Modifier.navigationBarsPadding()) {
            items(AppFeatures.all, key = { it.route }) { f ->
                val selected = f.route == AppFeatures.byRoute(current).route
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPick(f.route); onDismiss() }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(f.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        f.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                    if (selected) Icon(Icons.Filled.Check, contentDescription = "المختار", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
