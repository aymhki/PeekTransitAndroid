package com.aymanhki.peektransit

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.location.LocationManager
import com.aymanhki.peektransit.utils.permissions.LocalPermissionManager
import com.aymanhki.peektransit.utils.permissions.PermissionManager
import com.aymanhki.peektransit.data.cache.MapSnapshotCache
import com.aymanhki.peektransit.viewmodel.MainViewModel
import com.aymanhki.peektransit.managers.SettingsManager

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Map : BottomNavItem("map", "Map", Icons.Default.Map)
    object Stops : BottomNavItem("stops", "Stops", Icons.AutoMirrored.Filled.List)
    object Saved : BottomNavItem("saved", "Saved", Icons.Default.Bookmark)
    object Widgets : BottomNavItem("widgets", "Widgets", Icons.AutoMirrored.Filled.Note)
    object More : BottomNavItem("more", "More", Icons.Default.MoreHoriz)
}

class MainActivity : ComponentActivity() {
    
    companion object {
        @Volatile
        private var locationManagerInstance: LocationManager? = null
        
        fun getLocationManager(context: Context): LocationManager {
            return locationManagerInstance ?: synchronized(this) {
                locationManagerInstance ?: LocationManager(context.applicationContext).also { locationManagerInstance = it }
            }
        }
    }
    
    private lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PeekTransitConstants.TRANSIT_API_KEY = getTransitApiKey(applicationContext)

        getLocationManager(this)
        
        permissionManager = PermissionManager(this)
        
        MapSnapshotCache.initialize(applicationContext)

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val settingsManager = remember { SettingsManager.getInstance(context) }
            var currentTheme by remember { mutableStateOf(settingsManager.stopViewTheme) }
            
            LaunchedEffect(Unit) {
                while (true) {
                    currentTheme = settingsManager.stopViewTheme
                    kotlinx.coroutines.delay(100)
                }
            }
            
            val forceDarkTheme = currentTheme == com.aymanhki.peektransit.utils.StopViewTheme.CLASSIC
            
            PeekTransitTheme(forceDarkTheme = forceDarkTheme) {
                CompositionLocalProvider(LocalPermissionManager provides permissionManager) {
                    MainScreen()
                }
            }
        }
    }
}


@Composable
fun MainScreen() {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val mainViewModel: MainViewModel = viewModel()

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
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
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
                WidgetsScreen()
            }
            composable(BottomNavItem.More.route) {
                MoreScreen(
                    onNavigateToThemeSelection = { navController.navigate("theme_selection") },
                    onNavigateToAbout = { navController.navigate("about") },
                    onNavigateToCredits = { navController.navigate("credits") },
                    onNavigateToTermsAndPrivacy = { navController.navigate("terms_privacy") }
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
    }
}

fun getTransitApiKey(context: Context): String {
    val applicationInfo = context.packageManager
        .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)

    return applicationInfo.metaData.getString("TRANSIT_API_KEY") ?: ""
}