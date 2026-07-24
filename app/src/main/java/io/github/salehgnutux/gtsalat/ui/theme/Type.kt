package io.github.salehgnutux.gtsalat.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import io.github.salehgnutux.gtsalat.R

val UbuntuArabic = FontFamily(Font(R.font.ubuntu_arabic))
val AmiriQuran = FontFamily(Font(R.font.amiri_quran))

/** الطباعة الأساسيّة بخطّ Ubuntu Arabic؛ يُستعمل Amiri للنصوص القرآنيّة والأذكار. */
val GtTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = AmiriQuran),
        displayMedium = displayMedium.copy(fontFamily = AmiriQuran),
        displaySmall = displaySmall.copy(fontFamily = AmiriQuran),
        headlineLarge = headlineLarge.copy(fontFamily = UbuntuArabic),
        headlineMedium = headlineMedium.copy(fontFamily = UbuntuArabic),
        headlineSmall = headlineSmall.copy(fontFamily = UbuntuArabic),
        titleLarge = titleLarge.copy(fontFamily = UbuntuArabic),
        titleMedium = titleMedium.copy(fontFamily = UbuntuArabic),
        titleSmall = titleSmall.copy(fontFamily = UbuntuArabic),
        bodyLarge = bodyLarge.copy(fontFamily = UbuntuArabic),
        bodyMedium = bodyMedium.copy(fontFamily = UbuntuArabic),
        bodySmall = bodySmall.copy(fontFamily = UbuntuArabic),
        labelLarge = labelLarge.copy(fontFamily = UbuntuArabic),
        labelMedium = labelMedium.copy(fontFamily = UbuntuArabic),
        labelSmall = labelSmall.copy(fontFamily = UbuntuArabic),
    )
}

/** نمطٌ خاصّ للعدّاد التنازليّ الكبير. */
val CountdownStyle = TextStyle(fontFamily = UbuntuArabic, fontSize = 44.sp)
