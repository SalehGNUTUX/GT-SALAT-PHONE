package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import io.github.salehgnutux.gtsalat.data.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeToggleViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
) : ViewModel() {
    val mode = settingsRepo.settings
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val clock24h = settingsRepo.settings
        .map { it.clock24h }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val adhkarCardView = settingsRepo.settings
        .map { it.adhkarCardView }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleAdhkarView(current: Boolean) = viewModelScope.launch { settingsRepo.setAdhkarCardView(!current) }

    /** يبدّل بين الفاتح والداكن؛ إن كان تابعاً للنظام قلبَ إلى عكس وضع النظام الحاليّ. */
    fun toggle(effectiveDark: Boolean) = viewModelScope.launch {
        settingsRepo.setTheme(if (effectiveDark) ThemeMode.LIGHT else ThemeMode.DARK)
    }

    fun toggleClock(current: Boolean) = viewModelScope.launch { settingsRepo.setClock24h(!current) }
}

/** زرٌّ لتبديل نظام عرض الوقت 24/12 — يُوضَع في الرئيسيّة بجانب زرّ السِمة. */
@Composable
fun ClockFormatToggleButton(vm: ThemeToggleViewModel = hiltViewModel()) {
    val is24 by vm.clock24h.collectAsStateWithLifecycle()
    androidx.compose.material3.TextButton(onClick = { vm.toggleClock(is24) }) {
        androidx.compose.material3.Text(
            if (is24) "24" else "12",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        )
    }
}

/** زرٌّ لتبديل طريقة عرض الأذكار (قائمة ↔ بطاقات سلايد) — يُوضَع بجانب زرّ السِمة. */
@Composable
fun AdhkarViewToggleButton(vm: ThemeToggleViewModel = hiltViewModel()) {
    val cards by vm.adhkarCardView.collectAsStateWithLifecycle()
    IconButton(onClick = { vm.toggleAdhkarView(cards) }) {
        Icon(
            if (cards) Icons.Outlined.Notes else Icons.Outlined.AutoStories,
            contentDescription = if (cards) "العرض كقائمة" else "العرض كبطاقات",
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

/** زرٌّ ثابتٌ لتبديل الوضع الداكن/الفاتح — يُوضَع في ترويسات الشاشات بجانب العنوان. */
@Composable
fun ThemeToggleButton(vm: ThemeToggleViewModel = hiltViewModel()) {
    val mode by vm.mode.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }
    IconButton(onClick = { vm.toggle(dark) }) {
        Icon(
            if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
            contentDescription = if (dark) "التبديل إلى الوضع الفاتح" else "التبديل إلى الوضع الداكن",
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}
