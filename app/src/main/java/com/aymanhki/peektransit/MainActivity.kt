package com.aymanhki.peektransit

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.aymanhki.peektransit.ui.screens.BookmarkedStopsScreen
import com.aymanhki.peektransit.ui.screens.ListViewScreen
import com.aymanhki.peektransit.ui.screens.LiveBusStopScreen
import com.aymanhki.peektransit.ui.screens.MapViewScreen
import com.aymanhki.peektransit.ui.screens.MoreScreen
import com.aymanhki.peektransit.ui.screens.WidgetsScreen
import com.aymanhki.peektransit.ui.screens.ThemeSelectionScreen
import com.aymanhki.peektransit.ui.screens.AboutScreen
import com.aymanhki.peektransit.ui.screens.CreditsScreen
import com.aymanhki.peektransit.ui.screens.TermsAndPrivacyScreen
import com.aymanhki.peektransit.ui.theme.PeekTransitTheme
import com.aymanhki.peektransit.utils.location.LocationManagerProvider
import com.aymanhki.peektransit.utils.permissions.LocalPermissionManager
import com.aymanhki.peektransit.utils.permissions.PermissionManager
import com.aymanhki.peektransit.data.cache.MapSnapshotCache
import com.aymanhki.peektransit.viewmodel.MainViewModel
import com.aymanhki.peektransit.managers.SettingsManager
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.unit.dp
import com.aymanhki.peektransit.managers.FirstLaunchManager
import com.aymanhki.peektransit.ui.components.BannerView
import com.aymanhki.peektransit.ui.components.SplashScreen
import com.aymanhki.peektransit.ui.components.SupportDevelopmentSheet
import com.aymanhki.peektransit.utils.BannerType
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewManagerFactory

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Map : BottomNavItem("map", "Map", Icons.Default.Map)
    object Stops : BottomNavItem("stops", "Stops", Icons.AutoMirrored.Filled.List)
    object Saved : BottomNavItem("saved", "Saved", Icons.Default.Bookmark)
    object Widgets : BottomNavItem("widgets", "Widgets", Icons.Default.Widgets)
    object More : BottomNavItem("more", "More", Icons.Default.MoreHoriz)
}

class MainActivity : ComponentActivity() {
    private lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LocationManagerProvider.getInstance(this)
        permissionManager = PermissionManager(this)
        MapSnapshotCache.initialize(applicationContext)


        val stopNumber = intent.getIntExtra("STOP_NUMBER", -1)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val context = LocalContext.current
            val firstLaunchManager = remember { FirstLaunchManager.getInstance(context) }
            val settingsManager = remember { SettingsManager.getInstance(context) }
            var currentTheme by remember { mutableStateOf(settingsManager.stopViewTheme) }
            var showSplash by remember { mutableStateOf(firstLaunchManager.isFirstLaunch) }

            LaunchedEffect(Unit) {
                while (true) {
                    currentTheme = settingsManager.stopViewTheme
                    kotlinx.coroutines.delay(100)
                }
            }


            val forceDarkTheme = currentTheme == com.aymanhki.peektransit.utils.StopViewTheme.CLASSIC
            val isNightMode = when (applicationContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> true
                Configuration.UI_MODE_NIGHT_NO -> false
                else -> false
            }


            LaunchedEffect(forceDarkTheme, isNightMode) {
                val insetsController = WindowInsetsControllerCompat(window, window.decorView)
                insetsController.isAppearanceLightNavigationBars = !forceDarkTheme && !isNightMode
                insetsController.isAppearanceLightStatusBars = !forceDarkTheme && !isNightMode
            }

            PeekTransitTheme(forceDarkTheme = forceDarkTheme) {
                CompositionLocalProvider(LocalPermissionManager provides permissionManager) {
                    if (showSplash) {
                        SplashScreen(
                            onContinue = {
                                firstLaunchManager.setFirstLaunchCompleted()
                                showSplash = false
                            }
                        )
                    } else {
                        MainScreen(
                            activity = this@MainActivity,
                            initialStopNumber = if (stopNumber > 0) stopNumber else null
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun MainScreen(activity: ComponentActivity, initialStopNumber: Int? = null) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val mainViewModel: MainViewModel = viewModel()
    val showSupportSheet by mainViewModel.showSupportSheet.observeAsState(false)
    val thereIsAnUpdateAvailable by mainViewModel.thereIsAnUpdateAvailable.observeAsState(false)
    val theUserHasClickedOnTheUpdateAvailableBanner by mainViewModel.theUserHasClickedOnTheUpdateAvailableBanner.observeAsState(false)
    val shouldShowRateAppBanner by mainViewModel.shouldShowRateAppBanner.observeAsState(false)
    val hasShownRateAppBannerThisSession by mainViewModel.hasShownRateAppBannerThisSession.observeAsState(false)
    val wasRateAppBannerManuallyHidden by mainViewModel.wasRateAppBannerManuallyHidden.observeAsState(false)
    val shouldShowTipBanner by mainViewModel.shouldShowTipBanner.observeAsState(false)
    val hasShownTipBannerThisSession by mainViewModel.hasShownTipBannerThisSession.observeAsState(false)
    val wasTipBannerManuallyHidden by mainViewModel.wasTipBannerManuallyHidden.observeAsState(false)
    val startUpdateFlow by mainViewModel.startUpdateFlow.observeAsState(false)
    val showInAppReview by mainViewModel.showInAppReview.observeAsState(false)
    val isSearchingInProgress by mainViewModel.isSearchingDestination.observeAsState(false)

    if (startUpdateFlow) {
        LaunchedEffect(Unit) {
            val appUpdateManager = AppUpdateManagerFactory.create(context)
            val appUpdateInfoTask = appUpdateManager.appUpdateInfo

            appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    val updateOptions = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    appUpdateManager.startUpdateFlow(appUpdateInfo, activity, updateOptions)
                }
            }.addOnFailureListener { e ->
                mainViewModel.onUpdateFlowStarted()
            }

            mainViewModel.onUpdateFlowStarted()
        }
    }

    if (showInAppReview) {
        LaunchedEffect(Unit) {
            val reviewManager = ReviewManagerFactory.create(context)
            val request = reviewManager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                    flow.addOnCompleteListener { _ ->
                        mainViewModel.onInAppReviewFlowStarted()
                    }
                } else {
                    mainViewModel.onInAppReviewFlowStarted()
                }
            }
        }
    }

    val activeBanner: BannerType? = when {
        (hasShownRateAppBannerThisSession || hasShownTipBannerThisSession || thereIsAnUpdateAvailable) -> {
            when {
                (hasShownRateAppBannerThisSession && shouldShowRateAppBanner && !wasRateAppBannerManuallyHidden) -> BannerType.RATE
                (hasShownTipBannerThisSession && shouldShowTipBanner && !wasTipBannerManuallyHidden) -> BannerType.TIP
                (thereIsAnUpdateAvailable && !theUserHasClickedOnTheUpdateAvailableBanner) -> BannerType.UPDATE
                else -> null
            }
        }
        thereIsAnUpdateAvailable && !theUserHasClickedOnTheUpdateAvailableBanner -> BannerType.UPDATE
        (shouldShowRateAppBanner && !wasRateAppBannerManuallyHidden) -> BannerType.RATE
        (shouldShowTipBanner && !wasTipBannerManuallyHidden) -> BannerType.TIP
        else -> null
    }

    val shouldShowBanner by remember { derivedStateOf { activeBanner != null } }
    val isUpdateBanner by remember { derivedStateOf { activeBanner == BannerType.UPDATE } }
    val isRateAppBanner by remember { derivedStateOf { activeBanner == BannerType.RATE } }

    if (showSupportSheet) {
        SupportDevelopmentSheet(
            onDismiss = { mainViewModel.hideSupportSheet() }
        )
    }

    LaunchedEffect(Unit) {
        mainViewModel.initializeGlobal()
    }

    LaunchedEffect(initialStopNumber) {
        initialStopNumber?.let { stopNumber ->
            navController.navigate("live_stop/$stopNumber")
        }
    }

    val items = listOf(
        BottomNavItem.Map,
        BottomNavItem.Stops,
        BottomNavItem.Saved,
        BottomNavItem.Widgets,
        BottomNavItem.More
    )

    val startDestination = remember {
        val defaultTab = settingsManager.defaultTab
        when (defaultTab) {
            com.aymanhki.peektransit.utils.DefaultTab.MAP -> BottomNavItem.Map.route
            com.aymanhki.peektransit.utils.DefaultTab.STOPS -> BottomNavItem.Stops.route
            com.aymanhki.peektransit.utils.DefaultTab.SAVED -> BottomNavItem.Saved.route
            com.aymanhki.peektransit.utils.DefaultTab.WIDGETS -> BottomNavItem.Widgets.route
            com.aymanhki.peektransit.utils.DefaultTab.MORE -> BottomNavItem.More.route
        }
    }

    val isMapScreen = currentDestination?.route == BottomNavItem.Map.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = if (isMapScreen) {
                    Modifier.padding(
                        bottom = innerPadding.calculateBottomPadding()
                    )
                } else {
                    Modifier.padding(innerPadding)
                }
            ) {
                composable(BottomNavItem.Map.route) {
                    MapViewScreen(
                        viewModel = mainViewModel,
                        onNavigateToLiveStop = { stopNumber ->
                            navController.navigate("live_stop/$stopNumber")
                        },
                        isCurrentDestination = currentDestination?.route == BottomNavItem.Map.route
                    )
                }
                composable(BottomNavItem.Stops.route) {
                    ListViewScreen(
                        viewModel = mainViewModel,
                        onNavigateToLiveStop = { stopNumber ->
                            navController.navigate("live_stop/$stopNumber")
                        },
                        isCurrentDestination = currentDestination?.route == BottomNavItem.Stops.route
                    )
                }
                composable(BottomNavItem.Saved.route) {
                    BookmarkedStopsScreen(
                        onNavigateToLiveStop = { stopNumber ->
                            navController.navigate("live_stop/$stopNumber")
                        }
                    )
                }
                composable(BottomNavItem.Widgets.route) {
                    WidgetsScreen(
                        stopsDataStore = mainViewModel.stopsDataStore,
                        mainViewModel = mainViewModel
                    )
                }
                composable(BottomNavItem.More.route) {
                    MoreScreen(
                        onNavigateToThemeSelection = { navController.navigate("theme_selection") },
                        onNavigateToAbout = { navController.navigate("about") },
                        onNavigateToCredits = { navController.navigate("credits") },
                        onNavigateToTermsAndPrivacy = { navController.navigate("terms_privacy") },
                        onNavigateToSupportDevelopment = {
                            val settingsManager = SettingsManager.getInstance(context)
                            settingsManager.userHasClickedSupportDevelopment = true

                            //    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/aymhki"))
                            //    context.startActivity(intent)

                            mainViewModel.showSupportSheet()

                        }
                    )
                }
                composable(
                    "live_stop/{stopNumber}",
                    arguments = listOf(navArgument("stopNumber") { type = NavType.IntType })
                ) { backStackEntry ->
                    val stopNumber = backStackEntry.arguments?.getInt("stopNumber") ?: return@composable
                    LiveBusStopScreen(
                        stopNumber = stopNumber,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("theme_selection") {
                    ThemeSelectionScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("about") {
                    AboutScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("credits") {
                    CreditsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("terms_privacy") {
                    TermsAndPrivacyScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

            BannerView(
                activeBanner = if (isMapScreen && !isSearchingInProgress) activeBanner else null,
                mainViewModel = mainViewModel,
                isMapScreen = true,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
                    .padding(innerPadding)
            )

            BannerView(
                activeBanner = if (!isMapScreen) activeBanner else null,
                mainViewModel = mainViewModel,
                isMapScreen = false,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 30.dp)
                    .padding(innerPadding)
            )
        }
    }
}
