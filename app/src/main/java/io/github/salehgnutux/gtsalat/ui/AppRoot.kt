package io.github.salehgnutux.gtsalat.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.salehgnutux.gtsalat.ui.screens.AdhkarScreen
import io.github.salehgnutux.gtsalat.ui.screens.AsmaScreen
import io.github.salehgnutux.gtsalat.ui.screens.DashboardScreen
import io.github.salehgnutux.gtsalat.ui.screens.MoreScreen
import io.github.salehgnutux.gtsalat.ui.screens.QiblaScreen
import io.github.salehgnutux.gtsalat.ui.screens.SettingsScreen
import io.github.salehgnutux.gtsalat.ui.screens.SetupScreen
import io.github.salehgnutux.gtsalat.ui.screens.TasbihScreen
import io.github.salehgnutux.gtsalat.ui.screens.TimetableScreen

private enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "الرئيسيّة", Icons.Outlined.Home),
    TIMETABLE("timetable", "المواقيت", Icons.Outlined.CalendarMonth),
    QIBLA("qibla", "القبلة", Icons.Outlined.Explore),
    MORE("more", "المزيد", Icons.Outlined.Apps),
    SETTINGS("settings", "الإعدادات", Icons.Outlined.Settings),
}

@Composable
fun AppRoot(setupCompleted: Boolean) {
    if (!setupCompleted) {
        SetupScreen()
        return
    }
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                Dest.entries.forEach { d ->
                    val selected = currentRoute?.hierarchy?.any { it.route == d.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(d.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
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
            composable(Dest.MORE.route) { MoreScreen(onOpen = { nav.navigate(it) }) }
            composable("adhkar") { AdhkarScreen(onBack = { nav.popBackStack() }) }
            composable("tasbih") { TasbihScreen(onBack = { nav.popBackStack() }) }
            composable("asma") { AsmaScreen(onBack = { nav.popBackStack() }) }
            composable(Dest.SETTINGS.route) { SettingsScreen() }
        }
    }
}
