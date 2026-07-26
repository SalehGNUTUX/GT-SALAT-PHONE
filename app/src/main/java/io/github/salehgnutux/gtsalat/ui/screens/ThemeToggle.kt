package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
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

    /** يبدّل بين الفاتح والداكن؛ إن كان تابعاً للنظام قلبَ إلى عكس وضع النظام الحاليّ. */
    fun toggle(effectiveDark: Boolean) = viewModelScope.launch {
        settingsRepo.setTheme(if (effectiveDark) ThemeMode.LIGHT else ThemeMode.DARK)
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
