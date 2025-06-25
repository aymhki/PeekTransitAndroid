package com.aymanhki.peektransit.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.clip
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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveBusStopScreen(
    stopNumber: Int,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = WinnipegTransitAPI.getInstance()
    val savedStopsManager = remember { SavedStopsManager.getInstance(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    val sharedPreferences: SharedPreferences = remember { 
        context.getSharedPreferences("peek_transit_prefs", Context.MODE_PRIVATE) 
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
            } catch (e: Exception) {
                error = e.message ?: "Failed to load schedule"
            } finally {
                isLoading = false
            }
        }
    }
    
    // Network monitoring
    LaunchedEffect(Unit) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isNetworkAvailable = true
            }
            
            override fun onLost(network: Network) {
                isNetworkAvailable = false
                error = "No internet connection"
            }
            
            override fun onUnavailable() {
                isNetworkAvailable = false
                error = "No internet connection"
            }
        }
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()
        
        // Check initial network state
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        isNetworkAvailable = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                             networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        
        try {
            // Keep the callback alive
            kotlinx.coroutines.awaitCancellation()
        } finally {
            // Cleanup when LaunchedEffect is cancelled
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }
    
    // Load stop data on first launch
    LaunchedEffect(stopNumber) {
        fetchStopData()
    }
    
    // Clear network error when connection is restored
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
    
    // Live updates loop - only when app is active and network is available
    LaunchedEffect(isLiveUpdatesEnabled, stop, lifecycleState, isNetworkAvailable) {
        if (isLiveUpdatesEnabled && stop != null && lifecycleState.isAtLeast(Lifecycle.State.RESUMED) && isNetworkAvailable) {
            while (isLiveUpdatesEnabled && stop != null && lifecycleState.isAtLeast(Lifecycle.State.RESUMED) && isNetworkAvailable) {
                delay(60000) // 60 seconds
                if (isLiveUpdatesEnabled && stop != null && lifecycleState.isAtLeast(Lifecycle.State.RESUMED) && isNetworkAvailable) {
                    fetchScheduleData(isManual = false)
                }
            }
        }
    }
    
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top app bar
            TopAppBar(
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isLoading)
            
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = {
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
                    stop?.let { stopData ->
                        RealMapPreview(
                            latitude = stopData.centre.geographic.latitude,
                            longitude = stopData.centre.geographic.longitude,
                            direction = stopData.direction,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(horizontal = 16.dp)

                        )
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

                        items(scheduleData) { scheduleEntry ->
                            BusArrivalCard(scheduleEntry = scheduleEntry)
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
                MaterialTheme.colorScheme.surfaceVariant 
            else 
                MaterialTheme.colorScheme.primary
        ) {
            if (isRefreshCooldown || isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
fun BusArrivalCard(scheduleEntry: String) {
    val parts = scheduleEntry.split(PeekTransitConstants.SCHEDULE_STRING_SEPARATOR)
    if (parts.size >= 4) {
        val routeNumber = parts[0]
        val routeName = parts[1]
        val status = parts[2]
        val arrivalTime = parts[3]


        val columnWidths: List<Float>  = listOf(
            0.1f,
            0.38f,
            if (status == PeekTransitConstants.CANCELLED_STATUS_TEXT) 0.36f else 0.18f,
            if (status == PeekTransitConstants.CANCELLED_STATUS_TEXT) 0.0f else 0.34f
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        textAlign = TextAlign.Start,
                        text = routeNumber,
                        modifier = Modifier.fillMaxWidth(columnWidths[0]),
                    )

                    Text(
                        text = routeName,
                        modifier = Modifier.fillMaxWidth(columnWidths[1]),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (status != PeekTransitConstants.OK_STATUS_TEXT && status != PeekTransitConstants.DUE_STATUS_TEXT) {
                    Text(
                        text = status,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(columnWidths[2]),
                        color = when (status) {
                            PeekTransitConstants.LATE_STATUS_TEXT -> MaterialTheme.colorScheme.error
                            PeekTransitConstants.EARLY_STATUS_TEXT -> MaterialTheme.colorScheme.primary
                            PeekTransitConstants.CANCELLED_STATUS_TEXT -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                // Arrival time
                Text(
                    text = arrivalTime,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(columnWidths[3]),
                    color = when (status) {
                        PeekTransitConstants.DUE_STATUS_TEXT -> MaterialTheme.colorScheme.primary
                        PeekTransitConstants.CANCELLED_STATUS_TEXT -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
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