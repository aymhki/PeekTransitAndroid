package com.aymanhki.peektransit.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.aymanhki.peektransit.ui.components.RealMapPreview
import com.aymanhki.peektransit.managers.SavedStopsManager
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import com.aymanhki.peektransit.data.models.Stop
import com.aymanhki.peektransit.data.network.WinnipegTransitAPI
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.TimeFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.aymanhki.peektransit.ui.components.CustomPullToRefreshBox
import com.aymanhki.peektransit.ui.components.CustomTopAppBar
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.aymanhki.peektransit.managers.SettingsManager
import com.aymanhki.peektransit.ui.theme.PeekTransitTheme
import com.aymanhki.peektransit.utils.StopViewTheme
import com.aymanhki.peektransit.utils.FontUtils
@Composable
fun LiveBusStopScreen(
    stopNumber: Int,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = WinnipegTransitAPI.getInstance()
    val savedStopsManager = remember { SavedStopsManager.getInstance(context) }
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    val sharedPreferences: SharedPreferences = remember { 
        context.getSharedPreferences("peek_transit_prefs", Context.MODE_PRIVATE) 
    }
    
    // Theme state
    var currentTheme by remember { mutableStateOf(settingsManager.stopViewTheme) }
    
    // Update theme state when app resumes (to catch theme changes from settings)
    LaunchedEffect(lifecycleState) {
        if (lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) {
            currentTheme = settingsManager.stopViewTheme
        }
    }
    
    // State for stop data and live updates
    var stop by remember { mutableStateOf<Stop?>(null) }
    var isLiveUpdatesEnabled by remember { mutableStateOf(true) }
    var scheduleData by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingStop by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastUpdated by remember { mutableStateOf<Long?>(null) }
    var isBookmarked by remember { mutableStateOf(false) }
    var isRefreshCooldown by remember { mutableStateOf(false) }
    var isNetworkAvailable by remember { mutableStateOf(true) }
    
    // Cooldown duration (1 second like iOS)
    val cooldownDuration = 1000L
    
    // Functions for live updates preference management
    fun saveLiveUpdatesPreference(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean("live_updates_stop_$stopNumber", enabled)
            .apply()
    }
    
    fun loadLiveUpdatesPreference(): Boolean {
        return sharedPreferences.getBoolean("live_updates_stop_$stopNumber", true)
    }
    
    fun shouldEnableLiveUpdates(): Boolean {
        // Disable if there's an error, no internet, or empty schedule
        return if (error != null || scheduleData.isEmpty()) {
            false
        } else {
            loadLiveUpdatesPreference()
        }
    }
    
    // Smooth pulsating animation with easing
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 4f, // Much bigger radius
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = androidx.compose.animation.core.EaseInOut), // Slower with smooth easing
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = androidx.compose.animation.core.EaseInOut), // Slower with smooth easing
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )
    
    // Function to fetch stop data
    fun fetchStopData() {
        scope.launch {
            isLoadingStop = true
            error = null
            try {
                stop = api.getStop(stopNumber)
                if (stop == null) {
                    error = "Stop #$stopNumber not found"
                } else {
                    // Update bookmark status
                    isBookmarked = savedStopsManager.isStopSaved(stop!!)
                }
            } catch (e: Exception) {
                error = e.message ?: "Failed to load stop data"
            } finally {
                isLoadingStop = false
            }
        }
    }
    
    // Function to fetch schedule data
    fun fetchScheduleData(isManual: Boolean = false) {
        scope.launch {
            if (isManual) {
                if (isRefreshCooldown) return@launch
                isRefreshCooldown = true
                delay(cooldownDuration)
                isRefreshCooldown = false
                isLoading = true
            }
            

            error = null
            try {
                val schedule = api.getStopSchedule(stopNumber)
                val cleanedSchedule = api.cleanStopSchedule(schedule, TimeFormat.MIXED)
                scheduleData = cleanedSchedule
                lastUpdated = System.currentTimeMillis()
                
                // Clear any previous error on successful fetch
                error = null
            } catch (e: Exception) {
                // Only update error for manual refreshes or if there's no existing error
                if (isManual || error == null) {
                    error = when {
                        e.message?.contains("Network error") == true -> e.message
                        e.message?.contains("timeout") == true -> "Request timed out. Please try again."
                        e.message?.contains("Unable to resolve host") == true -> "Unable to connect to server. Check your internet connection."
                        else -> e.message ?: "Failed to load schedule"
                    }
                }
                
                // Log the error for debugging
                println("LiveBusStopScreen: Error fetching schedule (manual=$isManual): ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
    
    // Simplified network monitoring - less aggressive, more reliable
    LaunchedEffect(Unit) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        // Check network state periodically instead of using callbacks (less prone to false positives)
        fun checkNetworkState() {
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            val hasInternet = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            val isValidated = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
            
            val wasNetworkAvailable = isNetworkAvailable
            isNetworkAvailable = hasInternet && isValidated
            
            // Only update error if network state actually changed and went from available to unavailable
            if (wasNetworkAvailable && !isNetworkAvailable && error == null) {
                error = "No internet connection"
            }
        }
        
        // Initial check
        checkNetworkState()
        
        // Periodic checks (every 10 seconds) - less aggressive than callbacks
        while (true) {
            delay(10000) // Check every 10 seconds
            if (lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) {
                checkNetworkState()
            }
        }
    }
    
    // Load stop data on first launch
    LaunchedEffect(stopNumber) {
        fetchStopData()
    }
    
    // Clear network error when connection is restored and retry data fetch
    LaunchedEffect(isNetworkAvailable) {
        if (isNetworkAvailable && error == "No internet connection") {
            error = null
            // Automatically retry fetching data when network comes back
            delay(cooldownDuration)
            
            if (stop != null) {
                fetchScheduleData()
            }
        }
    }
    
    // Load live updates preference after successful data fetch
    LaunchedEffect(scheduleData, error) {
        // Only update preference when we have data or after errors are resolved
        if (scheduleData.isNotEmpty() && error == null) {
            isLiveUpdatesEnabled = loadLiveUpdatesPreference()
        } else if (error != null || scheduleData.isEmpty()) {
            isLiveUpdatesEnabled = false
        }
    }
    
    // Auto-refresh logic for schedule - initial load
    LaunchedEffect(stopNumber, stop) {
        if (stop != null) {
            fetchScheduleData()
        }
    }
    
    // Live updates loop - simplified and more reliable
    LaunchedEffect(isLiveUpdatesEnabled, stop, lifecycleState) {
        if (isLiveUpdatesEnabled && stop != null) {
            while (isLiveUpdatesEnabled && stop != null) {
                delay(60000) // 60 seconds
                
                // Only fetch if app is in foreground
                if (lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) {
                    try {
                        fetchScheduleData(isManual = false)
                    } catch (e: Exception) {
                        // Log error but don't break the loop
                        println("LiveBusStopScreen: Auto-refresh failed: ${e.message}")
                    }
                }
            }
        }
    }
    
    
    // Apply theme-based background
    val backgroundColor = when (currentTheme) {
        StopViewTheme.CLASSIC -> Color.Black
        StopViewTheme.MODERN -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top app bar
            CustomTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "#$stopNumber",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stop?.name ?: "Stop #$stopNumber",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Bookmark toggle icon
                    stop?.let { stopData ->
                        IconButton(
                            onClick = {
                                savedStopsManager.toggleSavedStatus(stopData)
                                isBookmarked = savedStopsManager.isStopSaved(stopData)
                            }
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (isBookmarked) "Remove saved stop" else "Save stop",
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )

            // Add pull-to-refresh for live bus stop data
            var isRefreshing by remember { mutableStateOf(false) }
            
            LaunchedEffect(isLoading) {
                isRefreshing = isLoading
            }
            
            CustomPullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    fetchScheduleData(isManual = true)
                }
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Updates",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Live indicator with pulsating animation
                            Box(
                                modifier = Modifier.size(48.dp), // Larger container for bigger pulse
                                contentAlignment = Alignment.Center
                            ) {
                                // Pulsating outer ring - only visible when live updates enabled
                                if (isLiveUpdatesEnabled) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp) // Base size for scaling
                                            .graphicsLayer {
                                                scaleX = pulseScale
                                                scaleY = pulseScale
                                                alpha = pulseAlpha
                                            }
                                            .background(
                                                Color.Red.copy(alpha = 0.7f),
                                                CircleShape
                                            )
                                    )
                                }

                                // Main indicator dot
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (isLiveUpdatesEnabled) Color.Red else Color.Gray,
                                            CircleShape
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Switch(
                                checked = isLiveUpdatesEnabled,
                                onCheckedChange = { enabled ->
                                    isLiveUpdatesEnabled = enabled
                                    saveLiveUpdatesPreference(enabled)
                                }
                            )
                        }
                    }
                }

                    item {
                        // Stop map preview with real map that scrolls with content
                        // Force the exact height and ensure it takes full width
                        stop?.let { stopData ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp) // Enforce exact height
                            ) {
                                RealMapPreview(
                                    latitude = stopData.centre.geographic.latitude,
                                    longitude = stopData.centre.geographic.longitude,
                                    direction = stopData.direction,
                                    modifier = Modifier
                                        .fillMaxSize() // Fill the entire Box
                                )
                            }
                        }
                    }


                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Schedule list
                when {
                    isLoadingStop -> {
                        item {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Loading stop information...")
                                }
                            }
                        }
                    }

                    isLoading -> {
                        item {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Loading arrivals...")
                                }
                            }
                        }
                    }

                    error != null -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Error loading arrivals",
                                    style = MaterialTheme.typography.headlineSmall
                                )

                                Text(
                                    text = error!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 8.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(onClick = { fetchScheduleData(isManual = true) }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }

                    scheduleData.isEmpty() -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "No upcoming arrivals",
                                    style = MaterialTheme.typography.headlineSmall,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "Check back later for bus arrival times.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }

                    else -> {

                        items(PeekTransitConstants.TEST_ENTRIES) { testEntry ->
                            BusArrivalCard(
                                scheduleEntry = testEntry,
                                theme = currentTheme
                            )
                        }

                        items(scheduleData) { scheduleEntry ->
                            BusArrivalCard(
                                scheduleEntry = scheduleEntry,
                                theme = currentTheme
                            )
                        }

                    }


                }
            }
        }

        }
        
        // Floating refresh button matching iOS design
        FloatingActionButton(
            onClick = { 
                if (!isRefreshCooldown) {
                    fetchScheduleData(isManual = true)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = if (isRefreshCooldown || isLoading) 
                MaterialTheme.colorScheme.primary
            else 
                MaterialTheme.colorScheme.primary
        ) {
            if (isRefreshCooldown || isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun BusArrivalCard(
    scheduleEntry: String,
    theme: StopViewTheme = StopViewTheme.MODERN
) {
    val parts = scheduleEntry.split(PeekTransitConstants.SCHEDULE_STRING_SEPARATOR)
    if (parts.size >= 4) {
        val routeNumber = parts[0]
        val routeName = parts[1]
        val status = parts[2]
        val arrivalTime = parts[3]
        val fontSizeForBusArrivalCard = 15.sp

        val columnWidths: List<Float>  = listOf(
            0.12f,
            0.43f,
            if (status == PeekTransitConstants.CANCELLED_STATUS_TEXT) 0.44f else 0.20f,
            if (status == PeekTransitConstants.CANCELLED_STATUS_TEXT) 0.01f else 0.25f
        )

        // Theme-based styling
        val backgroundColor = when (theme) {
            StopViewTheme.CLASSIC -> Color.Black
            StopViewTheme.MODERN -> Color.Transparent
        }
        
        val textColor = when (theme) {
            StopViewTheme.CLASSIC -> PeekTransitConstants.CLASSIC_THEM_TEXT_COLOR
            StopViewTheme.MODERN -> MaterialTheme.colorScheme.onSurface
        }
        
        val fontFamily = when (theme) {
            StopViewTheme.CLASSIC -> FontUtils.LCDDotFontFamily
            StopViewTheme.MODERN -> FontUtils.CourierFontFamily
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSizeForBusArrivalCard,
                    fontFamily = fontFamily
                )
            ) {

                Text(
                    textAlign = TextAlign.Start,
                    text = routeNumber,
                    modifier = Modifier.weight(columnWidths[0]),
                    color = textColor
                )

                Text(
                    textAlign = TextAlign.Start,
                    text = routeName,
                    modifier = Modifier.weight(columnWidths[1]),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor
                )


                Text(
                    text =  if (status != PeekTransitConstants.OK_STATUS_TEXT &&
                        status != PeekTransitConstants.DUE_STATUS_TEXT) status else "",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(columnWidths[2]),
                    color = when (theme) {
                        StopViewTheme.CLASSIC -> textColor
                        StopViewTheme.MODERN -> when (status) {
                            PeekTransitConstants.LATE_STATUS_TEXT -> MaterialTheme.colorScheme.error
                            PeekTransitConstants.EARLY_STATUS_TEXT -> MaterialTheme.colorScheme.primary
                            PeekTransitConstants.CANCELLED_STATUS_TEXT -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    }
                )


                // Arrival time
                Text(
                    text = if (status != PeekTransitConstants.CANCELLED_STATUS_TEXT) arrivalTime else "",
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(columnWidths[3]),
                    color = when (theme) {
                        StopViewTheme.CLASSIC -> textColor
                        StopViewTheme.MODERN -> when (arrivalTime) {
                            PeekTransitConstants.DUE_STATUS_TEXT -> MaterialTheme.colorScheme.primary
                            PeekTransitConstants.CANCELLED_STATUS_TEXT -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun VariantChip(variant: String) {
    Surface(
        modifier = Modifier.padding(2.dp),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = variant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

private fun getDirectionColor(direction: String): Color {
    return when (direction.lowercase()) {
        "northbound", "north" -> Color(0xFF4CAF50)
        "southbound", "south" -> Color(0xFFFF9800)
        "eastbound", "east" -> Color(0xFF2196F3)
        "westbound", "west" -> Color(0xFFE91E63)
        else -> Color(0xFF9E9E9E)
    }
}

private fun getMarkerColor(direction: String): Float {
    return when (direction.lowercase()) {
        "northbound", "north" -> BitmapDescriptorFactory.HUE_GREEN
        "southbound", "south" -> BitmapDescriptorFactory.HUE_ORANGE
        "eastbound", "east" -> BitmapDescriptorFactory.HUE_BLUE
        "westbound", "west" -> BitmapDescriptorFactory.HUE_MAGENTA
        else -> BitmapDescriptorFactory.HUE_RED
    }
}