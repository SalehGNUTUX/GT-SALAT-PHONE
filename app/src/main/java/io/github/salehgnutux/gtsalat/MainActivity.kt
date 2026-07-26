package io.github.salehgnutux.gtsalat

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.salehgnutux.gtsalat.data.settings.ThemeMode
import io.github.salehgnutux.gtsalat.ui.AppRoot
import io.github.salehgnutux.gtsalat.ui.RootViewModel
import io.github.salehgnutux.gtsalat.ui.theme.GtSalatTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val vm: RootViewModel = hiltViewModel()
            val state by vm.state.collectAsStateWithLifecycle()
            splash.setKeepOnScreenCondition { state == null }

            val s = state
            if (s != null) {
                val dark = when (s.themeMode) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                }
                val gradTop = if (dark) s.gradTopDark else s.gradTopLight
                val gradBot = if (dark) s.gradBotDark else s.gradBotLight
                GtSalatTheme(darkTheme = dark, dynamicColor = s.dynamicColor, seedColor = s.seedColor) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        AppRoot(setupCompleted = s.setupCompleted, gradientTop = gradTop, gradientBottom = gradBot)
                    }
                }
            }
        }
    }
}
