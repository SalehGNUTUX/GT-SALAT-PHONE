package io.github.salehgnutux.gtsalat

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.salehgnutux.gtsalat.data.settings.ThemeMode
import io.github.salehgnutux.gtsalat.ui.AppRoot
import io.github.salehgnutux.gtsalat.ui.RootViewModel
import io.github.salehgnutux.gtsalat.ui.theme.GtSalatTheme
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val vm: RootViewModel = hiltViewModel()
            val state by vm.state.collectAsStateWithLifecycle()

            // شاشة بدايةٍ بالشعار الجديد و«GNUTUX 2026»، تبقى حتى تحميل الحالة أو مدّةً دنيا.
            var minElapsed by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { delay(1500); minElapsed = true }

            val s = state
            if (s == null || !minElapsed) {
                BrandSplash()
            } else {
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

/** شاشة البداية المُوسَمة: الشعار الجديد في الوسط، و«GNUTUX 2026» في الأسفل. */
@Composable
private fun BrandSplash() {
    androidx.compose.foundation.layout.Box(
        Modifier.fillMaxSize().background(Color(0xFF1B6B4C)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.gt_logo),
            contentDescription = "GT-SALAT",
            modifier = Modifier.size(200.dp).clip(RoundedCornerShape(36.dp)),
        )
        androidx.compose.material3.Text(
            "GNUTUX 2026",
            color = Color(0xFFE8F0EA),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp),
        )
    }
}
