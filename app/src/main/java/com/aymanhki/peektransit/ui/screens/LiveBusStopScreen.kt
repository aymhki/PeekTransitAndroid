package com.aymanhki.peektransit.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import android.net.NetworkCapabilities
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
import com.aymanhki.peektransit.ui.components.CustomPullToRefreshBox
import com.aymanhki.peektransit.ui.components.CustomTopAppBar
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.aymanhki.peektransit.managers.SettingsManager
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
    
    var currentTheme by remember { mutableStateOf(settingsManager.stopViewTheme) }
    
    LaunchedEffect(lifecycleState) {
        if (lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) {
            currentTheme = settingsManager.stopViewTheme
        }
    }
    
    var stop by remember { mutableStateOf<Stop?>(null) }
    var isLiveUpdatesEnabled by remember { mutableStateOf(true) }
    var scheduleData by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingStop by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isBookmarked by remember { mutableStateOf(false) }
    var isRefreshCooldown by remember { mutableStateOf(false) }
    var isNetworkAvailable by remember { mutableStateOf(true) }
    
    val cooldownDuration = 1000L
    
    fun saveLiveUpdatesPreference(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean("live_updates_stop_$stopNumber", enabled)
            .apply()
    }
    
    fun loadLiveUpdatesPreference(): Boolean {
        return sharedPreferences.getBoolean("live_updates_stop_$stopNumber", true)
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = androidx.compose.animation.core.EaseInOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = androidx.compose.animation.core.EaseInOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )
    
    fun fetchStopData() {
        scope.launch {
            isLoadingStop = true
            error = null
            try {
                stop = api.getStop(stopNumber)
                if (stop == null) {
                    error = "Stop #$stopNumber not found"
                } else {
                    isBookmarked = savedStopsManager.isStopSaved(stop!!)
                }
            } catch (e: Exception) {
                error = e.message ?: "Failed to load stop data"
            } finally {
                isLoadingStop = false
            }
        }
    }
    
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

                error = null
            } catch (e: Exception) {
                if (isManual || error == null) {
                    error = when {
                        e.message?.contains("Network error") == true -> e.message
                        e.message?.contains("timeout") == true -> "Request timed out. Please try again."
                        e.message?.contains("Unable to resolve host") == true -> "Unable to connect to server. Check your internet connection."
                        else -> e.message ?: "Failed to load schedule"
                    }
                }
                
                println("LiveBusStopScreen: Error fetching schedule (manual=$isManual): ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
    
    LaunchedEffect(Unit) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        fun checkNetworkState() {
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            val hasInternet = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            val isValidated = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
            
            val wasNetworkAvailable = isNetworkAvailable
            isNetworkAvailable = hasInternet && isValidated
            
            if (wasNetworkAvailable && !isNetworkAvailable && error == null) {
                error = "No internet connection"
            }
        }
        
        checkNetworkState()
        
        while (true) {
            delay(10000)
            if (lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) {
                checkNetworkState()
            }
        }
    }
    
    LaunchedEffect(stopNumber) {
        fetchStopData()
    }
    
    LaunchedEffect(isNetworkAvailable) {
        if (isNetworkAvailable && error == "No internet connection") {
            error = null
            delay(cooldownDuration)
            
            if (stop != null) {
                fetchScheduleData()
            } else {
                fetchStopData()
            }
        }
    }
    
    LaunchedEffect(scheduleData, error) {
        if (scheduleData.isNotEmpty() && error == null) {
            isLiveUpdatesEnabled = loadLiveUpdatesPreference()
        } else if (error != null || scheduleData.isEmpty()) {
            isLiveUpdatesEnabled = false
        }
    }
    
    LaunchedEffect(stopNumber, stop) {
        if (stop != null) {
            fetchScheduleData()
        }
    }
    
    LaunchedEffect(isLiveUpdatesEnabled, stop, lifecycleState) {
        if (isLiveUpdatesEnabled && stop != null) {
            while (isLiveUpdatesEnabled && stop != null) {
                delay(60000)
                
                if (lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) {
                    try {
                        fetchScheduleData(isManual = false)
                    } catch (e: Exception) {
                        println("LiveBusStopScreen: Auto-refresh failed: ${e.message}")
                    }
                }
            }
        }
    }
    
    
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
                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLiveUpdatesEnabled) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
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
                        stop?.let { stopData ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            ) {
                                RealMapPreview(
                                    latitude = stopData.centre.geographic.latitude,
                                    longitude = stopData.centre.geographic.longitude,
                                    direction = stopData.direction,
                                    modifier = Modifier
                                        .fillMaxSize()
                                )
                            }
                        }
                    }


                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

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

                                Button(onClick = { 
                                    if (stop == null) {
                                        fetchStopData()
                                    } else {
                                        fetchScheduleData(isManual = true)
                                    }
                                }) {
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

//                        items(PeekTransitConstants.TEST_ENTRIES) { testEntry ->
//                            BusArrivalCard(
//                                scheduleEntry = testEntry,
//                                theme = currentTheme
//                            )
//                        }

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
        
        FloatingActionButton(
            onClick = { 
                if (!isRefreshCooldown) {
                    if (stop == null) {
                        fetchStopData()
                    } else {
                        fetchScheduleData(isManual = true)
                    }
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
        val fontSizeForBusArrivalCard = when (theme) {
            StopViewTheme.CLASSIC -> 14.sp
            StopViewTheme.MODERN -> 15.sp
        }

        val columnWidths: List<Float>  = listOf(
            0.12f,
            0.43f,
            if (status == PeekTransitConstants.CANCELLED_STATUS_TEXT) 0.44f else 0.20f,
            if (status == PeekTransitConstants.CANCELLED_STATUS_TEXT) 0.01f else 0.25f
        )

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


