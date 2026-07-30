package io.github.salehgnutux.gtsalat.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** أشكالٌ موحّدةٌ عبر التطبيق لاتّساق حوافّ البطاقات والأزرار والحوارات. */
val GtShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),   // البطاقات الافتراضيّة
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val LightScheme = lightColorScheme(
    primary = GreenPrimary,
    secondary = GoldAccent,
    tertiary = GoldAccent,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
)

private val DarkScheme = darkColorScheme(
    primary = GreenPrimaryDark,
    secondary = GoldAccentDark,
    tertiary = GoldAccentDark,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
)

/** سِمةٌ مبنيّةٌ من لونٍ مخصّص (seed): يُشتقّ منه اللون الأساسيّ وحاوياته مع إبقاء أسطح مقروءة. */
private fun schemeFromSeed(seed: Color, dark: Boolean): ColorScheme {
    val base = if (dark) DarkScheme else LightScheme
    val onSeed = if (seed.luminance() > 0.5f) Color.Black else Color.White
    return base.copy(
        primary = seed,
        onPrimary = onSeed,
        primaryContainer = lerp(seed, base.surface, if (dark) 0.55f else 0.75f),
        onPrimaryContainer = if (dark) lerp(seed, Color.White, 0.65f) else lerp(seed, Color.Black, 0.6f),
        secondary = seed,
        tertiary = seed,
    )
}

@Composable
fun GtSalatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    seedColor: Int = 0,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        seedColor != 0 -> schemeFromSeed(Color(seedColor), darkTheme)
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = GtTypography,
        shapes = GtShapes,
        content = content,
    )
}
