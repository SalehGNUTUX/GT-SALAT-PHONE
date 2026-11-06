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

/** هل السمة الزجاجيّة مفعَّلة؟ بطاقات الشاشات تقرؤها لتأخذ مظهرها. */
val LocalGlassTheme = androidx.compose.runtime.staticCompositionLocalOf { false }

// ألوان السمة الزجاجيّة (من النموذج المعتمد): لا أبيض ناصع ولا أسود تامّ، أخضرٌ للتفاعل وذهبيٌّ للوقت.
// حشوتا الزجاج محسوبتان لتباين النصّ فوق أيّ خلفيّة؛ لا تخفّضهما دون إعادة الحساب.
val GlassFillDark = Color(0x940E1612)
val GlassFillLight = Color(0xA8FFFFFF)
val GlassStrongDark = Color(0xA812221A)
val GlassStrongLight = Color(0xB8FFFFFF)

private val GlassDarkScheme = darkColorScheme(
    primary = Color(0xFF8FDDB6), onPrimary = Color(0xFF00381F),
    primaryContainer = Color(0xFF1E3A2D), onPrimaryContainer = Color(0xFFCDEBDC),
    secondary = Color(0xFFE8C766), onSecondary = Color(0xFF3B2F00),
    secondaryContainer = Color(0xFF24352C), onSecondaryContainer = Color(0xFFEAF1EC),
    tertiary = Color(0xFFE8C766), onTertiary = Color(0xFF3B2F00),
    tertiaryContainer = Color(0xFF3B3113), onTertiaryContainer = Color(0xFFF3E2A8),
    background = Color(0xFF0F1512), onBackground = Color(0xFFEAF1EC),
    surface = Color(0xFF161D19), onSurface = Color(0xFFEAF1EC),
    surfaceVariant = GlassFillDark, onSurfaceVariant = Color(0xFFC2D0C7),
    surfaceContainerLowest = Color(0xFF0F1512), surfaceContainerLow = Color(0xFF161D19),
    surfaceContainer = Color(0xFF1A231E), surfaceContainerHigh = Color(0xFF202A25),
    surfaceContainerHighest = GlassFillDark,
    outline = Color(0xFFB3C1B8), outlineVariant = Color(0xFF3A4640),
)

private val GlassLightScheme = lightColorScheme(
    primary = Color(0xFF0B5039), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCFE8DA), onPrimaryContainer = Color(0xFF0B3A2A),
    secondary = Color(0xFF6F5200), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCEAE1), onSecondaryContainer = Color(0xFF12201A),
    tertiary = Color(0xFF6F5200), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3E7BE), onTertiaryContainer = Color(0xFF3E2E00),
    background = Color(0xFFF4F8F5), onBackground = Color(0xFF12201A),
    surface = Color(0xFFF8FBF9), onSurface = Color(0xFF12201A),
    surfaceVariant = GlassFillLight, onSurfaceVariant = Color(0xFF3C4E44),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF8FBF9),
    surfaceContainer = Color(0xFFF4F8F5), surfaceContainerHigh = Color(0xFFEEF3EF),
    surfaceContainerHighest = GlassFillLight,
    outline = Color(0xFF4F6157), outlineVariant = Color(0xFFC5D2C9),
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
    glass: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        // الزجاج بألوانه المحسوبة لا بألوان النظام؛ ولونٌ مخصّصٌ يغيّر لون التفاعل فقط.
        glass -> (if (darkTheme) GlassDarkScheme else GlassLightScheme).let { base ->
            if (seedColor == 0) base else {
                val seed = Color(seedColor)
                base.copy(primary = seed, onPrimary = if (seed.luminance() > 0.5f) Color.Black else Color.White)
            }
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        seedColor != 0 -> schemeFromSeed(Color(seedColor), darkTheme)
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalGlassTheme provides glass) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GtTypography,
            shapes = GtShapes,
            content = content,
        )
    }
}
