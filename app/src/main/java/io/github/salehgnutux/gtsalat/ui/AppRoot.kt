package io.github.salehgnutux.gtsalat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import io.github.salehgnutux.gtsalat.ui.screens.frostedGlass
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import io.github.salehgnutux.gtsalat.ui.screens.AudioRecitersScreen
import io.github.salehgnutux.gtsalat.ui.screens.DownloadedSurahsScreen
import io.github.salehgnutux.gtsalat.ui.screens.ReciterSurahsScreen
import io.github.salehgnutux.gtsalat.ui.screens.MoreScreen
import io.github.salehgnutux.gtsalat.ui.tour.tourTarget
import io.github.salehgnutux.gtsalat.ui.screens.MushafScreen
import io.github.salehgnutux.gtsalat.ui.screens.QiblaScreen
import io.github.salehgnutux.gtsalat.ui.screens.ImsakiahScreen
import io.github.salehgnutux.gtsalat.ui.screens.QuranBookmarksScreen
import io.github.salehgnutux.gtsalat.ui.screens.QuranHubScreen
import io.github.salehgnutux.gtsalat.ui.screens.QuranMiniPlayer
import io.github.salehgnutux.gtsalat.ui.screens.RadioMiniPlayer
import io.github.salehgnutux.gtsalat.ui.screens.RadiosScreen
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
fun AppRoot(
    setupCompleted: Boolean,
    gradientTop: Int = 0,
    gradientBottom: Int = 0,
    homeBackgroundId: String? = null,
    homeBackgroundLevel: Int = 1,
    tourShown: Boolean = true,
    deepLink: String? = null,
    onDeepLinkHandled: () -> Unit = {},
) {
    if (!setupCompleted) {
        // Surface يوفّر لون المحتوى الصحيح (onBackground) لشاشة الإعداد خارج الـScaffold.
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            SetupScreen()
        }
        return
    }
    val nav = rememberNavController()
    // فتحٌ مباشرٌ لوجهةٍ من الإشعار (deep-link) — يُنفَّذ مرّةً ثمّ يُمسَح.
    LaunchedEffect(deepLink) {
        val route = deepLink ?: return@LaunchedEffect
        runCatching { nav.navigate(route) { launchSingleTop = true } }
        onDeepLinkHandled()
    }
    // «ما الجديد» — يُعرَض مرّةً بعد تحديث الإصدار.
    val whatsNewVm: io.github.salehgnutux.gtsalat.ui.screens.WhatsNewViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val showWhatsNew by whatsNewVm.shouldShow.collectAsStateWithLifecycle()
    if (showWhatsNew) {
        io.github.salehgnutux.gtsalat.ui.screens.WhatsNewDialog(onDismiss = { whatsNewVm.markShown() })
    }
    // رسالةُ الأذونات المطلوبة بعد التثبيت/التحديث — تظهر بعد «ما الجديد» إن كانت أذوناتٌ مهمّةٌ ناقصة،
    // وتنقل المستخدم إلى قسم «موثوقيّة التنبيهات» لتفعيلها ليعمل التطبيق بشكلٍ سليم.
    val permDue by whatsNewVm.permPromptDue.collectAsStateWithLifecycle()
    val permCtx = LocalContext.current
    val permsMissing = remember(permDue) { criticalPermsMissing(permCtx) }
    if (!showWhatsNew && permDue && permsMissing) {
        AlertDialog(
            onDismissRequest = { whatsNewVm.markPermShown() },
            icon = { Icon(Icons.Outlined.Settings, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("أذوناتٌ مطلوبةٌ لعمل التطبيق") },
            text = {
                Text(
                    "ليصلك الأذان في وقته حتى والتطبيق مغلق، فعّل الأذونات من بطاقة «موثوقيّة التنبيهات»: " +
                        "الإشعارات، والإنذارات الدقيقة، وإعفاء البطاريّة، ونافذة ملء الشاشة (على أندرويد 14+).",
                )
            },
            confirmButton = {
                Button(onClick = {
                    whatsNewVm.openReliabilitySection()
                    nav.navigate(Dest.SETTINGS.route) { launchSingleTop = true }
                }) { Text("فتح الإعدادات") }
            },
            dismissButton = { FilledTonalButton(onClick = { whatsNewVm.markPermShown() }) { Text("لاحقاً") } },
        )
    }
    val backStack by nav.currentBackStackEntryAsState()
    val currentDest = backStack?.destination
    // رسالةٌ عند إعادة النقر على «المزيد» ونحن داخل أحد أقسامه.
    var askReturnToMore by remember { mutableStateOf(false) }
    // رسالة تأكيد الخروج عند الرجوع من الرئيسيّة.
    var askExit by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? android.app.Activity
    val ctx = LocalContext.current

    // طريقة الخروج: نافذة تأكيد أو نقرتان للرجوع (اختياريّة، تُختار عند أوّل استخدام).
    val exitVm: io.github.salehgnutux.gtsalat.ui.screens.ExitPrefViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val exitMode by exitVm.exitMode.collectAsStateWithLifecycle()
    var askExitMethod by remember { mutableStateOf(false) }   // نافذة اختيار الطريقة (أوّل مرّة)
    var lastBackMs by remember { mutableStateOf(0L) }

    // زرّ رجوع النظام: من أيّ قسمٍ يعود للرئيسيّة؛ ومن الرئيسيّة حسب طريقة الخروج المختارة.
    val onDashboard = currentDest?.route == Dest.DASHBOARD.route
    BackHandler {
        if (onDashboard) {
            when (exitMode) {
                "double" -> {
                    val now = System.currentTimeMillis()
                    if (now - lastBackMs < 2000) activity?.finish()
                    else { lastBackMs = now; android.widget.Toast.makeText(ctx, "اضغط «رجوع» مرّةً أخرى للخروج", android.widget.Toast.LENGTH_SHORT).show() }
                }
                "dialog" -> askExit = true
                else -> askExitMethod = true   // لم تُختَر الطريقة بعد → نعرض خيار الاختيار المبدئيّ
            }
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
    val glass = io.github.salehgnutux.gtsalat.ui.theme.LocalGlassTheme.current
    val gradient = remember(gradientTop, gradientBottom, cs.background, cs.primary, glass) {
        io.github.salehgnutux.gtsalat.ui.screens.appGradient(gradientTop, gradientBottom, glass, cs.background, cs.primary)
    }

    // الشرح الحركيّ: يبدأ في الرئيسيّة بعد الرسائل المعروضة، مرّةً عند التثبيت ومتى طُلب من الإعدادات.
    val tour = remember { io.github.salehgnutux.gtsalat.ui.tour.TourController() }
    val tourActive = onDashboard && !tourShown && !showWhatsNew && !(permDue && permsMissing)
    LaunchedEffect(tourShown) { if (!tourShown) tour.stepIndex = 0 }
    LaunchedEffect(tourActive) { tour.active = tourActive }
    val blurredBitmap = io.github.salehgnutux.gtsalat.ui.screens.rememberBlurredBitmap(homeBackgroundId.takeIf { glass })
    val frostScrim = cs.background.copy(alpha = io.github.salehgnutux.gtsalat.ui.screens.homeScrimTop(homeBackgroundLevel))
    val frosted = remember(blurredBitmap, frostScrim) {
        blurredBitmap?.let { io.github.salehgnutux.gtsalat.ui.screens.FrostedBackdrop(it, frostScrim) }
    }
    // القارئ النصّيّ والمصحف بلا صورة: القراءة الطويلة أريح على خلفيّةٍ هادئة.
    val readingRoute = currentDest?.route?.let { it.startsWith("quran_read") || it == "quran_mushaf" } == true

    Box(Modifier.fillMaxSize().background(gradient)) {
        androidx.compose.animation.AnimatedVisibility(
            visible = glass && !onDashboard && !readingRoute && homeBackgroundId != null,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
        ) {
            val lastBlurId = remember { mutableStateOf(homeBackgroundId) }
            if (homeBackgroundId != null) lastBlurId.value = homeBackgroundId
            lastBlurId.value?.let {
                io.github.salehgnutux.gtsalat.ui.screens.BlurredBackdrop(it, Modifier.fillMaxSize())
            }
        }
        // صورة الرئيسيّة خلف الشاشة كلّها (تحت شريط الحالة أيضاً)، وتتلاشى عند مغادرة الرئيسيّة.
        androidx.compose.animation.AnimatedVisibility(
            visible = onDashboard && homeBackgroundId != null,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
        ) {
            // يبقى آخر معرّفٍ أثناء التلاشي بعد إطفاء الخلفيّة.
            val lastId = remember { mutableStateOf(homeBackgroundId) }
            if (homeBackgroundId != null) lastId.value = homeBackgroundId
            lastId.value?.let {
                io.github.salehgnutux.gtsalat.ui.screens.HomeBackdrop(it, homeBackgroundLevel, Modifier.fillMaxSize())
            }
        }
        androidx.compose.runtime.CompositionLocalProvider(
            io.github.salehgnutux.gtsalat.ui.tour.LocalTour provides tour,
            io.github.salehgnutux.gtsalat.ui.screens.LocalFrostedBackdrop provides frosted.takeIf { onDashboard },
        ) {
        Scaffold(
            // الخلفيّة شفّافة لإظهار التدرّج؛ لكن يجب تحديد لون المحتوى صراحةً وإلّا
            // سقط Material3 إلى الأسود للنصوص/الأيقونات غير المحدَّدة اللون (خلل الوضع الداكن).
            containerColor = Color.Transparent,
            contentColor = cs.onBackground,
            bottomBar = {
                Column {
                    // مشغّلٌ مصغّرٌ عائمٌ يعيد المستخدم إلى موضع التلاوة الجاريّة.
                    QuranMiniPlayer(onOpen = { route ->
                        nav.navigate(route) { launchSingleTop = true }
                    })
                    // مشغّل الإذاعة العالميّ — يبقى ظاهراً عبر الأقسام أثناء البثّ.
                    RadioMiniPlayer(onOpen = { nav.navigate("radios") { launchSingleTop = true } })
                    // مشغّل صوت الأذكار العائم — يبقى عبر الأقسام، والنقر يعيد لقسمه الأصليّ.
                    io.github.salehgnutux.gtsalat.ui.screens.AdhkarMiniPlayer(onOpen = { route ->
                        nav.navigate(route) { launchSingleTop = true }
                    })
                    if (glass) {
                        GlassBottomBar(currentDest) { d, selected ->
                            onTabClick(nav, d, selected, currentDest?.route) { askReturnToMore = true }
                        }
                    } else {
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
                                    modifier = Modifier.tourTarget(io.github.salehgnutux.gtsalat.ui.tour.TourKeys.TAB_PREFIX + d.route),
                                )
                            }
                        }
                    }
                }
            },
        ) { padding ->
            val animOn = remember { animationsEnabled(ctx) }
            NavHost(
                navController = nav,
                startDestination = Dest.DASHBOARD.route,
                modifier = Modifier.padding(padding),
                enterTransition = { navEnter(animOn) },
                exitTransition = { navExit(animOn) },
                popEnterTransition = { navEnter(animOn) },
                popExitTransition = { navExit(animOn) },
            ) {
                composable(Dest.DASHBOARD.route) {
                    androidx.compose.runtime.CompositionLocalProvider(
                        io.github.salehgnutux.gtsalat.ui.screens.LocalHomeBackdropOn provides (homeBackgroundId != null),
                    ) {
                        DashboardScreen(
                            onOpenSettings = {
                                nav.navigate(Dest.SETTINGS.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            },
                            onOpenShortcut = { route -> nav.navigate(route) { launchSingleTop = true } },
                        )
                    }
                }
                composable(Dest.TIMETABLE.route) { TimetableScreen() }
                composable(Dest.QIBLA.route) { QiblaScreen() }
                composable(Dest.SETTINGS.route) {
                    SettingsScreen(onShowTour = {
                        nav.navigate(Dest.DASHBOARD.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    })
                }

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
                    composable("adhkar_audio") { io.github.salehgnutux.gtsalat.ui.screens.AdhkarAudioScreen(onBack = { nav.popBackStack() }) }
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
                        SurahIndexScreen(
                            "القرآن النصّيّ",
                            onOpen = { nav.navigate("quran_read/$it") },
                            onBack = { nav.popBackStack() },
                            ayahSearch = true,
                            onOpenAyah = { s, a -> nav.navigate("quran_read/$s?ayah=$a") },
                        )
                    }
                    composable(
                        "quran_read/{n}?ayah={ayah}",
                        arguments = listOf(
                            navArgument("n") { type = NavType.StringType },
                            navArgument("ayah") { type = NavType.StringType; defaultValue = "0" },
                        ),
                    ) { TextReaderScreen(onBack = { nav.popBackStack() }) }
                    composable("quran_bookmarks") {
                        QuranBookmarksScreen(
                            onOpenAyah = { s, a -> nav.navigate("quran_read/$s?ayah=$a") },
                            onBack = { nav.popBackStack() },
                        )
                    }
                    composable("quran_audio") {
                        AudioRecitersScreen(
                            onOpenReciter = { nav.navigate("quran_reciter/$it") },
                            onOpenDownloaded = { nav.navigate("quran_downloaded") },
                            onBack = { nav.popBackStack() },
                        )
                    }
                    composable(
                        "quran_reciter/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.StringType }),
                    ) { entry ->
                        ReciterSurahsScreen(reciterId = entry.arguments?.getString("id") ?: "", onBack = { nav.popBackStack() })
                    }
                    composable("quran_downloaded") {
                        DownloadedSurahsScreen(onOpenReciter = { nav.navigate("quran_reciter/$it") }, onBack = { nav.popBackStack() })
                    }
                    composable("quran_mushaf") { MushafScreen(onBack = { nav.popBackStack() }) }
                    composable("radios") { RadiosScreen(onBack = { nav.popBackStack() }) }
                    composable("imsakiah") { ImsakiahScreen(onBack = { nav.popBackStack() }) }
                    // تعلّم الطهارة والصلاة + الرقية الشرعية
                    composable("learn_hub") { io.github.salehgnutux.gtsalat.ui.screens.LearnHubScreen(onOpen = { nav.navigate(it) }, onBack = { nav.popBackStack() }) }
                    composable(
                        "learn_section/{id}",
                        arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.StringType }),
                    ) { io.github.salehgnutux.gtsalat.ui.screens.LearnSectionScreen(it.arguments?.getString("id") ?: "", onOpen = { nav.navigate(it) }, onBack = { nav.popBackStack() }) }
                    composable("qasr_tool") { io.github.salehgnutux.gtsalat.ui.screens.QasrToolScreen(onBack = { nav.popBackStack() }) }
                    composable("tahara_tool") { io.github.salehgnutux.gtsalat.ui.screens.TaharaToolScreen(onBack = { nav.popBackStack() }) }
                    composable("ruqyah_hub") { io.github.salehgnutux.gtsalat.ui.screens.RuqyahHubScreen(onOpen = { nav.navigate(it) }, onBack = { nav.popBackStack() }) }
                    composable(
                        "ruqyah_section/{id}",
                        arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.StringType }),
                    ) { io.github.salehgnutux.gtsalat.ui.screens.RuqyahSectionScreen(it.arguments?.getString("id") ?: "", onOpen = { nav.navigate(it) }, onBack = { nav.popBackStack() }) }
                }
            }
        }
        }
        // طبقة الشرح الحركيّ فوق الواجهة الحقيقيّة.
        if (tourActive) {
            io.github.salehgnutux.gtsalat.ui.tour.AppTourOverlay(tour, onFinish = { whatsNewVm.markTourShown() })
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
                    // القسم المفتوح باختصارٍ أو إشعارٍ ليس تحته بطاقات «المزيد» في المكدّس.
                    if (!nav.popBackStack(MORE_HOME, inclusive = false)) {
                        nav.navigate(MORE_GRAPH) {
                            popUpTo(nav.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    }
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

    // اختيارٌ مبدئيٌّ لطريقة الخروج (أوّل مرّةٍ يُضغَط فيها «رجوع» من الرئيسيّة).
    if (askExitMethod) {
        AlertDialog(
            onDismissRequest = { askExitMethod = false },
            title = { Text("طريقة الخروج من التطبيق") },
            text = { Text("اختر كيف تُغادر التطبيق عند ضغط «رجوع» من الرئيسيّة (يمكنك تغييرها لاحقاً من الإعدادات):") },
            confirmButton = {
                Button(onClick = { exitVm.setExitMode("double"); askExitMethod = false }) { Text("نقرتان على «رجوع»") }
            },
            dismissButton = {
                FilledTonalButton(onClick = { exitVm.setExitMode("dialog"); askExitMethod = false }) { Text("نافذة تأكيد") }
            },
        )
    }
}

/** الشريط السفليّ العائم في السمة الزجاجيّة. */
@Composable
private fun GlassBottomBar(currentDest: androidx.navigation.NavDestination?, onClick: (Dest, Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
    val fill = io.github.salehgnutux.gtsalat.ui.screens.glassStrongFill()
    androidx.compose.foundation.layout.Row(
        Modifier
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .fillMaxWidth()
            .frostedGlass(shape)
            .background(fill, shape)
            .border(1.dp, cs.outlineVariant.copy(alpha = 0.6f), shape)
            .padding(4.dp),
    ) {
        Dest.entries.forEach { d ->
            val selected = currentDest?.hierarchy?.any { it.route == d.route } == true
            val tint = if (selected) cs.primary else cs.onSurfaceVariant
            androidx.compose.foundation.layout.Column(
                Modifier
                    .weight(1f)
                    .tourTarget(io.github.salehgnutux.gtsalat.ui.tour.TourKeys.TAB_PREFIX + d.route)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                    .background(if (selected) cs.primary.copy(alpha = 0.16f) else Color.Transparent)
                    .clickable(role = androidx.compose.ui.semantics.Role.Tab) { onClick(d, selected) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(d.icon, contentDescription = null, tint = tint)
                Text(
                    d.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = tint,
                    fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else null,
                    maxLines = 1,
                )
            }
        }
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
    if (!selected && dest == Dest.DASHBOARD) {
        // الرئيسيّة بالرجوع إليها لا بالتنقّل إليها: popUpTo(saveState) يحفظ القسم الحاليّ تحت مفتاح الرئيسيّة إن
        // دُخل بتنقّلٍ مباشر (اختصار/إشعار)، فيعيده restoreState بدلها ويبدو الزرّ معطّلاً.
        nav.popBackStack(dest.route, inclusive = false, saveState = true)
        return
    }
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

/** هل تنقص أذوناتٌ مهمّةٌ لعمل التنبيهات؟ (إشعارات · إنذاراتٌ دقيقة · إعفاء بطاريّة). */
private fun criticalPermsMissing(context: android.content.Context): Boolean {
    val notif = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
    } else false
    val exact = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        !(context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager).canScheduleExactAlarms()
    } else false
    val battery = !(context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager)
        .isIgnoringBatteryOptimizations(context.packageName)
    return notif || exact || battery
}
