package io.github.salehgnutux.gtsalat.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut

/**
 * انتقالاتٌ موحّدةٌ خفيفةٌ للتنقّل بين الأقسام (fade-through): تلاشٍ مع مقياسٍ طفيفٍ —
 * أنعمُ وأرخصُ من الانزلاق مع القوائم الكسولة الثقيلة. تُحترَم إعدادات «تقليل الحركة»
 * (ANIMATOR_DURATION_SCALE == 0 → بلا حركة).
 */
private const val NAV_DUR = 240

/** هل حركةُ النظام مفعَّلة (يحترم «تقليل الحركة» في إمكانيّات الوصول)؟ */
fun animationsEnabled(context: Context): Boolean = runCatching {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
}.getOrDefault(true)

fun navEnter(enabled: Boolean): EnterTransition =
    if (!enabled) EnterTransition.None
    else fadeIn(tween(NAV_DUR)) + scaleIn(tween(NAV_DUR), initialScale = 0.98f)

fun navExit(enabled: Boolean): ExitTransition =
    if (!enabled) ExitTransition.None
    else fadeOut(tween(NAV_DUR)) + scaleOut(tween(NAV_DUR), targetScale = 1.02f)
