package com.aymanhki.peektransit.ui.screens

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aymanhki.peektransit.MainActivity
import com.aymanhki.peektransit.ui.components.StopRow
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.location.LocationManager
import com.aymanhki.peektransit.viewmodel.MainViewModel
import com.aymanhki.peektransit.utils.permissions.rememberMultiplePermissionsState
import com.aymanhki.peektransit.ui.components.CustomPullToRefreshBox
import kotlinx.coroutines.launch


// No experimental APIs needed! Using stable Android permission management
@Composable
fun ListViewScreen(
    viewModel: MainViewModel,
    onNavigateToLiveStop: (Int) -> Unit = {},
    isCurrentDestination: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationManager = remember { MainActivity.getLocationManager(context) }
    
    // Permission state
    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    )
    
    // ViewModel states
    val stops by viewModel.stops.observeAsState(emptyList())
    val searchResults by viewModel.searchResults.observeAsState(emptyList())
    
    // Debug logging
    LaunchedEffect(stops) {
        println("ListViewScreen: Stops list updated with ${stops.size} stops")
        stops.forEach { stop ->
            println("ListViewScreen: Stop ${stop.number} has ${stop.variants.size} variants")
        }
    }
    val isLoading by viewModel.isLoading.observeAsState(false)
    val isSearching by viewModel.isSearching.observeAsState(false)
    val error by viewModel.error.observeAsState()
    val searchError by viewModel.searchError.observeAsState()
    
    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    
    // Location state for change detection
    var previousLocation by remember { mutableStateOf<android.location.Location?>(null) }
    var isInitialLoad by remember { mutableStateOf(true) }
    
    // Function to load stops with location change detection
    fun loadStopsWithLocationCheck(forceRefresh: Boolean = false) {
        println("ListViewScreen: loadStopsWithLocationCheck called with forceRefresh=$forceRefresh")
        scope.launch {
            locationManager.getCurrentLocation(forceRefresh)?.let { location ->
                println("ListViewScreen: Got location ${location.latitude}, ${location.longitude}")
                val shouldUpdate = if (previousLocation != null) {
                    val distance = previousLocation!!.distanceTo(location)
                    println("ListViewScreen: Distance from previous location: $distance")
                    distance > PeekTransitConstants.DISTANCE_CHANGE_ALLOWED_BEFORE_REFRESHING_STOPS // Use same threshold as MapView
                } else {
                    println("ListViewScreen: No previous location, should update = true")
                    true // First time
                }
                
                println("ListViewScreen: shouldUpdate=$shouldUpdate, forceRefresh=$forceRefresh, isInitialLoad=$isInitialLoad")
                if (shouldUpdate || forceRefresh) {
                    if (isInitialLoad && !forceRefresh) {
                        println("ListViewScreen: Calling viewModel.initializeWithLocation")
                        viewModel.initializeWithLocation(location)
                    } else {
                        println("ListViewScreen: Calling viewModel.loadStops")
                        viewModel.loadStops(location, forceRefresh = forceRefresh)
                    }
                } else {
                    println("ListViewScreen: Skipping update due to shouldUpdate=false and forceRefresh=false")
                }
                previousLocation = location
                isInitialLoad = false
            } ?: run {
                println("ListViewScreen: Failed to get location")
            }
        }
    }
    
    // Observe initialization state
    val isViewModelInitialized by viewModel.isInitialized.observeAsState(false)
    
    // Load stops when permission is granted and viewModel is not initialized
    LaunchedEffect(locationPermissionsState.allPermissionsGranted, isViewModelInitialized, isCurrentDestination) {
        if (locationPermissionsState.allPermissionsGranted && !isViewModelInitialized && isCurrentDestination) {
            println("ListViewScreen: Triggering loadStopsWithLocationCheck (current destination)")
            loadStopsWithLocationCheck()
        }
    }
    
    // Handle search
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            isSearchActive = true
            scope.launch {
                locationManager.getCurrentLocation()?.let { location ->
                    viewModel.searchForStops(searchQuery, location)
                }
            }
        } else {
            isSearchActive = false
            viewModel.clearSearchResults()
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Modern rounded search bar
        if (locationPermissionsState.allPermissionsGranted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { 
                        Text(
                            "Search stops, routes...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search, 
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
        
        when {
            !locationPermissionsState.allPermissionsGranted -> {
                // Permission request UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Location Permission Required",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "This app needs location access to show nearby bus stops.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { locationPermissionsState.launchMultiplePermissionRequest() }
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
            
            else -> {
                // Pull-to-refresh wraps everything
                CustomPullToRefreshBox(
                    isRefreshing = isLoading || isSearching,
                    onRefresh = {
                        if (isSearchActive) {
                            // Refresh search results
                            scope.launch {
                                locationManager.getCurrentLocation()?.let { location ->
                                    viewModel.searchForStops(searchQuery, location)
                                }
                            }
                        } else {
                            // Refresh nearby stops
                            loadStopsWithLocationCheck(forceRefresh = true)
                        }
                    }
                ) {
                    when {
                        isLoading || isSearching -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = if (isSearching) "Searching..." else "Loading nearby stops...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                        
                        else -> {
                            val stopsToShow = if (isSearchActive) searchResults else stops
                            val currentError = if (isSearchActive) searchError else error
                            
                            if (stopsToShow.isEmpty() && currentError == null) {
                                // Empty state
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = if (isSearchActive) "No stops found for \"$searchQuery\"" else "No nearby stops found",
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    if (!isSearchActive) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        Button(
                                            onClick = {
                                                loadStopsWithLocationCheck(forceRefresh = true)
                                            }
                                        ) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            } else {
                                // Stops list
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    items(stopsToShow, key = { stop -> 
                                        "${stop.number}_${stop.variants.size}_${stop.variants.hashCode()}"
                                    }) { stop ->
                                        StopRow(
                                            stop = stop,
                                            distance = stop.getDistance(),
                                            onNavigateToLiveStop = onNavigateToLiveStop
                                        )
                                    }
                                }
                            }
                            
                            // Error display
                            currentError?.let { transitError ->
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Snackbar(
                                        action = {
                                            TextButton(
                                                onClick = {
                                                    if (isSearchActive) {
                                                        viewModel.clearSearchError()
                                                    } else {
                                                        viewModel.clearError()
                                                    }
                                                }
                                            ) {
                                                Text("Dismiss")
                                            }
                                        }
                                    ) {
                                        Text(transitError.message)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
