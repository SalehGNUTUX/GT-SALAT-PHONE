package io.github.salehgnutux.gtsalat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.salehgnutux.gtsalat.ui.screens.AdhkarSessionScreen
import io.github.salehgnutux.gtsalat.ui.screens.AsmaScreen
import io.github.salehgnutux.gtsalat.ui.screens.DashboardScreen
import io.github.salehgnutux.gtsalat.ui.screens.DuasScreen
import io.github.salehgnutux.gtsalat.ui.screens.EventsScreen
import io.github.salehgnutux.gtsalat.ui.screens.HadithScreen
import io.github.salehgnutux.gtsalat.ui.screens.HikamScreen
import io.github.salehgnutux.gtsalat.ui.screens.HisnCategoryScreen
import io.github.salehgnutux.gtsalat.ui.screens.HisnScreen
import io.github.salehgnutux.gtsalat.ui.screens.AudioRecitationScreen
import io.github.salehgnutux.gtsalat.ui.screens.MoreScreen
import io.github.salehgnutux.gtsalat.ui.screens.MushafScreen
import io.github.salehgnutux.gtsalat.ui.screens.QiblaScreen
import io.github.salehgnutux.gtsalat.ui.screens.QuranHubScreen
import io.github.salehgnutux.gtsalat.ui.screens.SurahIndexScreen
import io.github.salehgnutux.gtsalat.ui.screens.TextReaderScreen
import io.github.salehgnutux.gtsalat.ui.screens.SettingsScreen
import io.github.salehgnutux.gtsalat.ui.screens.SetupScreen
import io.github.salehgnutux.gtsalat.ui.screens.TafsirScreen
import io.github.salehgnutux.gtsalat.ui.screens.TafsirSurahScreen
import io.github.salehgnutux.gtsalat.ui.screens.TasbihScreen
import io.github.salehgnutux.gtsalat.ui.screens.TimetableScreen

private const val MORE_GRAPH = "more_graph"
private const val MORE_HOME = "more"

private enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "الرئيسيّة", Icons.Outlined.Home),
    TIMETABLE("timetable", "المواقيت", Icons.Outlined.CalendarMonth),
    QIBLA("qibla", "القبلة", Icons.Outlined.Explore),
    MORE(MORE_GRAPH, "المزيد", Icons.Outlined.Apps),
    SETTINGS("settings", "الإعدادات", Icons.Outlined.Settings),
}

@Composable
fun AppRoot(setupCompleted: Boolean, gradientTop: Int = 0, gradientBottom: Int = 0) {
    if (!setupCompleted) {
        // Surface يوفّر لون المحتوى الصحيح (onBackground) لشاشة الإعداد خارج الـScaffold.
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            SetupScreen()
        }
        return
    }
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentDest = backStack?.destination
    // رسالةٌ عند إعادة النقر على «المزيد» ونحن داخل أحد أقسامه.
    var askReturnToMore by remember { mutableStateOf(false) }
    // رسالة تأكيد الخروج عند الرجوع من الرئيسيّة.
    var askExit by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? android.app.Activity

    // زرّ رجوع النظام: من أيّ قسمٍ يعود للرئيسيّة؛ ومن الرئيسيّة يسأل قبل الخروج.
    val onDashboard = currentDest?.route == Dest.DASHBOARD.route
    BackHandler {
        if (onDashboard) {
            askExit = true
        } else {
            nav.navigate(Dest.DASHBOARD.route) {
                popUpTo(nav.graph.findStartDestination().id) { inclusive = false }
                launchSingleTop = true
            }
            UiEvents.requestHomeTop()
        }
    }

    val cs = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    // تدرّجٌ مخصّصٌ إن عُيّن، وإلّا مشتقٌّ من ألوان السِمة (يحترم الداكن/الفاتح وMaterial You).
    // يُثبَّت بـ remember حتّى لا يُعاد توليد الشيدر مع كلّ إعادة تركيبٍ أثناء انتقالات التنقّل.
    val gradient = remember(gradientTop, gradientBottom, dark, cs.background, cs.primary) {
        if (gradientTop != 0 && gradientBottom != 0) {
            Brush.verticalGradient(listOf(Color(gradientTop), Color(gradientBottom)))
        } else {
            Brush.verticalGradient(
                listOf(cs.background, lerp(cs.background, cs.primary, if (dark) 0.14f else 0.06f)),
            )
        }
    }

    Box(Modifier.fillMaxSize().background(gradient)) {
        Scaffold(
            // الخلفيّة شفّافة لإظهار التدرّج؛ لكن يجب تحديد لون المحتوى صراحةً وإلّا
            // سقط Material3 إلى الأسود للنصوص/الأيقونات غير المحدَّدة اللون (خلل الوضع الداكن).
            containerColor = Color.Transparent,
            contentColor = cs.onBackground,
            bottomBar = {
                NavigationBar {
                    Dest.entries.forEach { d ->
                        val selected = currentDest?.hierarchy?.any { it.route == d.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                onTabClick(nav, d, selected, currentDest?.route) { askReturnToMore = true }
                            },
                            icon = { Icon(d.icon, contentDescription = d.label) },
                            label = { Text(d.label) },
                        )
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = Dest.DASHBOARD.route,
                modifier = Modifier.padding(padding),
            ) {
                composable(Dest.DASHBOARD.route) { DashboardScreen() }
                composable(Dest.TIMETABLE.route) { TimetableScreen() }
                composable(Dest.QIBLA.route) { QiblaScreen() }
                composable(Dest.SETTINGS.route) { SettingsScreen() }

                // «المزيد» رسمٌ متداخل: تُحفظ حالته وتُستعاد عند العودة إليه من تبويبٍ آخر.
                navigation(startDestination = MORE_HOME, route = MORE_GRAPH) {
                    composable(MORE_HOME) { MoreScreen(onOpen = { nav.navigate(it) }) }
                    composable("hisn") { HisnScreen(onOpen = { nav.navigate("hisn/$it") }, onBack = { nav.popBackStack() }) }
                    composable(
                        "hisn/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.StringType }),
                    ) { HisnCategoryScreen(onBack = { nav.popBackStack() }) }
                    composable(
                        "adhkar_session/{type}",
                        arguments = listOf(navArgument("type") { type = NavType.StringType }),
                    ) { AdhkarSessionScreen(onBack = { nav.popBackStack() }) }
                    composable("tasbih") { TasbihScreen(onBack = { nav.popBackStack() }) }
                    composable("asma") { AsmaScreen(onBack = { nav.popBackStack() }) }
                    composable("hadith") { HadithScreen(onBack = { nav.popBackStack() }) }
                    composable("duas") { DuasScreen(onBack = { nav.popBackStack() }) }
                    composable("hikam") { HikamScreen(onBack = { nav.popBackStack() }) }
                    composable("events") { EventsScreen(onBack = { nav.popBackStack() }) }
                    composable("tafsir") { TafsirScreen(onOpen = { nav.navigate("tafsir/$it") }, onBack = { nav.popBackStack() }) }
                    composable(
                        "tafsir/{n}",
                        arguments = listOf(navArgument("n") { type = NavType.StringType }),
                    ) { TafsirSurahScreen(onBack = { nav.popBackStack() }) }

                    // القرآن الكريم: محورٌ بثلاثة أقسام.
                    composable("quran") { QuranHubScreen(onOpen = { nav.navigate(it) }, onBack = { nav.popBackStack() }) }
                    composable("quran_text") {
                        SurahIndexScreen("القرآن النصّيّ", onOpen = { nav.navigate("quran_read/$it") }, onBack = { nav.popBackStack() })
                    }
                    composable(
                        "quran_read/{n}",
                        arguments = listOf(navArgument("n") { type = NavType.StringType }),
                    ) { TextReaderScreen(onBack = { nav.popBackStack() }) }
                    composable("quran_audio") { AudioRecitationScreen(onBack = { nav.popBackStack() }) }
                    composable("quran_mushaf") { MushafScreen(onBack = { nav.popBackStack() }) }
                }
            }
        }
    }

    if (askReturnToMore) {
        AlertDialog(
            onDismissRequest = { askReturnToMore = false },
            title = { Text("المزيد") },
            text = { Text("هل تريد العودة للمزيد أم البقاء؟") },
            confirmButton = {
                Button(onClick = {
                    askReturnToMore = false
                    nav.popBackStack(MORE_HOME, inclusive = false)
                }) { Text("العودة للمزيد") }
            },
            dismissButton = {
                FilledTonalButton(onClick = { askReturnToMore = false }) { Text("البقاء") }
            },
        )
    }

    if (askExit) {
        AlertDialog(
            onDismissRequest = { askExit = false },
            title = { Text("مغادرة التطبيق") },
            text = { Text("أنت على وشك مغادرة التطبيق. هل تريد الخروج أم البقاء؟") },
            confirmButton = {
                Button(
                    onClick = { askExit = false; activity?.finish() },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = cs.error,
                        contentColor = cs.onError,
                    ),
                ) { Text("خروج") }
            },
            dismissButton = {
                FilledTonalButton(onClick = { askExit = false }) { Text("البقاء") }
            },
        )
    }
}

/**
 * سلوك أزرار الشريط السفليّ:
 * - تبويبٌ غير محدَّد ← الانتقال إليه مع استعادة حالته السابقة (restoreState) — فيعود المستخدم لآخر قسمٍ كان فيه.
 * - «المزيد» وهو محدَّد وداخل قسمٍ فرعيّ ← رسالةُ «العودة للبطاقات أم البقاء».
 * - تبويبٌ آخر محدَّد ← الرجوع إلى جذره.
 */
private fun onTabClick(
    nav: NavHostController,
    dest: Dest,
    selected: Boolean,
    currentRoute: String?,
    askReturnToMore: () -> Unit,
) {
    if (!selected) {
        nav.navigate(dest.route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        return
    }
    when (dest) {
        Dest.MORE -> if (currentRoute != MORE_HOME) askReturnToMore()
        Dest.DASHBOARD -> UiEvents.requestHomeTop()  // إعادة النقر على الرئيسيّة تعود لرأس الصفحة
        else -> nav.popBackStack(dest.route, inclusive = false)
    }
}
