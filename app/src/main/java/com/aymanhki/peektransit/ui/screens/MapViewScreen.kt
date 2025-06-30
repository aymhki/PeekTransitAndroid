
package com.aymanhki.peektransit.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import com.aymanhki.peektransit.ui.components.CustomModalBottomSheet
import com.aymanhki.peektransit.managers.SettingsManager
import com.aymanhki.peektransit.utils.StopViewTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aymanhki.peektransit.R
import com.aymanhki.peektransit.data.models.Stop
import com.aymanhki.peektransit.ui.components.StopRow
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.MainActivity
import com.aymanhki.peektransit.utils.location.LocationManager
import com.aymanhki.peektransit.viewmodel.MainViewModel
import com.aymanhki.peektransit.utils.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MapStyleOptions


// No experimental APIs needed! Using stable Android permission management
@Composable
fun MapViewScreen(
    viewModel: MainViewModel,
    onNavigateToLiveStop: (Int) -> Unit = {},
    isCurrentDestination: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationManager = remember { MainActivity.getLocationManager(context) }
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val currentTheme = settingsManager.stopViewTheme
    val systemDarkTheme = isSystemInDarkTheme()
    
    // Force dark theme for Classic theme, otherwise follow system theme for Modern
    val isDarkTheme = when (currentTheme) {
        StopViewTheme.CLASSIC -> true
        StopViewTheme.MODERN -> systemDarkTheme
    }
    
    // Permission state
    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    )
    
    // ViewModel states
    val stops by viewModel.stops.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val error by viewModel.error.observeAsState()
    
    // Map state
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var previousUserLocation by remember { mutableStateOf<LatLng?>(null) }
    var locationStatus by remember { mutableStateOf("Initializing...") }
    var isLocationLoading by remember { mutableStateOf(true) }
    var isInitialLoad by remember { mutableStateOf(true) }
    var showMap by remember { mutableStateOf(false) }
    var isMapsInitialized by remember { mutableStateOf(false) }
    var hasCameraInitializedToUserLocation by remember { mutableStateOf(false) }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(49.8951, -97.1384), // Start from Winnipeg (app location)
            PeekTransitConstants.DEFAULT_MAP_ZOOM
        )
    }
    
    // Selected stop for bottom sheet
    var selectedStop by remember { mutableStateOf<Stop?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    
    // Initialize Google Maps SDK
    LaunchedEffect(Unit) {
        try {
            MapsInitializer.initialize(context, MapsInitializer.Renderer.LATEST) { result ->
                when (result) {
                    MapsInitializer.Renderer.LATEST -> {
                        isMapsInitialized = true
                        locationStatus = "Maps initialized"
                    }
                    MapsInitializer.Renderer.LEGACY -> {
                        isMapsInitialized = true
                        locationStatus = "Maps initialized (legacy)"
                    }
                }
            }
        } catch (e: Exception) {
            locationStatus = "Maps initialization failed: ${e.message}"
            isMapsInitialized = true // Allow to proceed even if initialization fails
        }
    }
    
    // Function to fetch location and update map
    fun fetchLocationAndUpdateMap(forceRefresh: Boolean = false) {
        scope.launch {
            if (isInitialLoad) {
                // For initial load, keep loading screen visible
                isLocationLoading = true
                showMap = false
            } else if (!forceRefresh) {
                // For subsequent loads, show loading indicator on existing map
                isLocationLoading = true
            }
            locationStatus = "Fetching location..."
            
            try {
                val location = locationManager.getCurrentLocation(forceRefresh)
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    val previousLocation = userLocation
                    userLocation = latLng
                    
                    // Update ViewModel with initial location if not set
                    if (viewModel.currentLocation.value == null) {
                        viewModel.updateCurrentLocation(location)
                    }
                    
                    // Check if location changed significantly
                    val shouldUpdateStops = if (previousLocation != null) {
                        val distance = FloatArray(1)
                        android.location.Location.distanceBetween(
                            previousLocation.latitude, previousLocation.longitude,
                            latLng.latitude, latLng.longitude,
                            distance
                        )
                        distance[0] > PeekTransitConstants.DISTANCE_CHANGE_ALLOWED_BEFORE_REFRESHING_STOPS // Use constant from PeekTransitConstants
                    } else {
                        true // First time - always load stops
                    }
                    
                    if (isInitialLoad) {
                        // First time: show map and animate to user location
                        showMap = true
                        if (isMapsInitialized) {
                            try {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newCameraPosition(
                                        CameraPosition.fromLatLngZoom(latLng, PeekTransitConstants.DEFAULT_MAP_ZOOM)
                                    ),
                                    1500 // 1.5 second animation
                                )
                                hasCameraInitializedToUserLocation = true
                            } catch (e: Exception) {
                                // If animation fails, just move the camera directly
                                cameraPositionState.move(
                                    CameraUpdateFactory.newCameraPosition(
                                        CameraPosition.fromLatLngZoom(latLng, PeekTransitConstants.DEFAULT_MAP_ZOOM)
                                    )
                                )
                                hasCameraInitializedToUserLocation = true
                            }
                        }
                    } else if (forceRefresh && isMapsInitialized) {
                        // Manual refresh: animate to new location
                        try {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.fromLatLngZoom(latLng, PeekTransitConstants.DEFAULT_MAP_ZOOM)
                                )
                            )
                        } catch (e: Exception) {
                            // If animation fails, just move the camera directly
                            cameraPositionState.move(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.fromLatLngZoom(latLng, PeekTransitConstants.DEFAULT_MAP_ZOOM)
                                )
                            )
                        }
                    }
                    
                    // Update location status after camera positioning
                    locationStatus = "Location: ${"%.4f".format(location.latitude)}, ${"%.4f".format(location.longitude)}"
                    
                    // Load nearby stops if location changed significantly or forced refresh
                    if (shouldUpdateStops || forceRefresh) {
                        // Add a small delay after camera animation to ensure map is ready
                        if (isInitialLoad || forceRefresh) {
                            kotlinx.coroutines.delay(500) // Wait 500ms for animation to complete
                        }
                        if (isInitialLoad) {
                            viewModel.initializeWithLocation(location)
                        } else {
                            viewModel.loadStops(location, forceRefresh = forceRefresh)
                        }
                    }
                    
                    isInitialLoad = false
                    previousUserLocation = previousLocation
                } else {
                    locationStatus = "Could not get location"
                    if (isInitialLoad) {
                        showMap = true // Show map even if location failed
                    }
                }
            } catch (e: Exception) {
                locationStatus = "Location error: ${e.message}"
                if (isInitialLoad) {
                    showMap = true // Show map even if location failed
                }
            } finally {
                isLocationLoading = false
            }
        }
    }
    
    // Observe initialization state
    val isViewModelInitialized by viewModel.isInitialized.observeAsState(false)
    
    // Observe live location updates from ViewModel
    val liveLocation by viewModel.currentLocation.observeAsState()
    
    // Show map immediately if ViewModel is already initialized
    LaunchedEffect(isViewModelInitialized) {
        if (isViewModelInitialized) {
            showMap = true
            isInitialLoad = false
            isLocationLoading = false
        }
    }
    
    // Handle first-time map initialization when user switches to map tab
    // This ensures camera zooms to user location even if app was opened from another tab
    LaunchedEffect(showMap, isViewModelInitialized, isMapsInitialized, hasCameraInitializedToUserLocation) {
        if (showMap && isViewModelInitialized && isMapsInitialized && !hasCameraInitializedToUserLocation) {
            // User opened app from another tab and now switched to map tab
            // ViewModel is already initialized with location, so zoom to it
            liveLocation?.let { location ->
                val latLng = LatLng(location.latitude, location.longitude)
                userLocation = latLng
                locationStatus = "Location: ${"%.4f".format(location.latitude)}, ${"%.4f".format(location.longitude)}"
                
                try {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.fromLatLngZoom(latLng, PeekTransitConstants.DEFAULT_MAP_ZOOM)
                        ),
                        1500 // 1.5 second animation
                    )
                    hasCameraInitializedToUserLocation = true
                    println("MapViewScreen: Camera initialized to user location (switched from another tab)")
                } catch (e: Exception) {
                    // If animation fails, just move the camera directly
                    cameraPositionState.move(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.fromLatLngZoom(latLng, PeekTransitConstants.DEFAULT_MAP_ZOOM)
                        )
                    )
                    hasCameraInitializedToUserLocation = true
                    println("MapViewScreen: Camera moved to user location (fallback)")
                }
            }
        }
    }
    
    // Handle live location updates for camera panning and circle updates
    LaunchedEffect(liveLocation) {
        liveLocation?.let { location ->
            val newLatLng = LatLng(location.latitude, location.longitude)
            val previousLocation = userLocation
            
            // Update user location for circle
            userLocation = newLatLng
            
            // Auto-pan camera to new location if it's significantly different
            if (previousLocation != null && isMapsInitialized && showMap) {
                val distance = FloatArray(1)
                android.location.Location.distanceBetween(
                    previousLocation.latitude, previousLocation.longitude,
                    newLatLng.latitude, newLatLng.longitude,
                    distance
                )
                
                // Pan camera if moved more than 50 meters (less than stop reload threshold)
                if (distance[0] > 50.0f) {
                    try {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.fromLatLngZoom(newLatLng, cameraPositionState.position.zoom)
                            ),
                            1000 // 1 second smooth animation
                        )
                    } catch (e: Exception) {
                        // Fallback to direct move if animation fails
                        cameraPositionState.move(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.fromLatLngZoom(newLatLng, cameraPositionState.position.zoom)
                            )
                        )
                    }
                }
            }
        }
    }
    
    // Handle initial location fetch when map tab is the starting destination
    LaunchedEffect(locationPermissionsState.allPermissionsGranted, isMapsInitialized, isViewModelInitialized, isCurrentDestination) {
        if (locationPermissionsState.allPermissionsGranted && isMapsInitialized && !isViewModelInitialized && isCurrentDestination) {
            println("MapViewScreen: Triggering fetchLocationAndUpdateMap (app opened on map tab)")
            fetchLocationAndUpdateMap()
        } else if (!locationPermissionsState.allPermissionsGranted) {
            locationStatus = "Location permission required"
        } else if (!isMapsInitialized) {
            locationStatus = "Initializing maps..."
        }
    }
    
    // Map style for dark mode
    val mapStyle = if (isDarkTheme) {
        MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
    } else {
        null
    }
    
    // Extension function to format coordinates
    fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (locationPermissionsState.allPermissionsGranted) {
            if (!showMap && isInitialLoad && !isViewModelInitialized) {
                // Show loading screen for initial map load
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Loading Map...",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = locationStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { showBottomSheet = false },
                properties = MapProperties(
                    isMyLocationEnabled = locationPermissionsState.allPermissionsGranted,
                    mapStyleOptions = mapStyle
                ),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = false,
                    zoomControlsEnabled = false,
                    tiltGesturesEnabled = true,
                    rotationGesturesEnabled = true,
                    scrollGesturesEnabled = true,
                )
            ) {
                // Circle showing search radius around user location
                userLocation?.let { location ->
                    Circle(
                        center = location,
                        radius = PeekTransitConstants.STOPS_DISTANCE_RADIUS,
                        strokeColor = MaterialTheme.colorScheme.secondary,
                        fillColor = androidx.compose.ui.graphics.Color.Transparent, // Transparent fill
                        strokeWidth = 3f
                    )
                }
                
                // Bus stop markers
                stops.forEach { stop ->
                    val position = LatLng(
                        stop.centre.geographic.latitude,
                        stop.centre.geographic.longitude
                    )

                    Marker(
                        state = MarkerState(position = position),
                        title = stop.name,
                        snippet = "Stop #${stop.number} - ${stop.direction}",
                        anchor = Offset(0.5f, 1.0f),
                        icon = getCustomMarkerIcon(context, stop.direction),
                        zIndex = 1.0f,
                        onClick = {
                            selectedStop = stop
                            showBottomSheet = true
                            false
                        }
                    )
                }
            }
            }
            
            // Status indicator at the top
            if (locationPermissionsState.allPermissionsGranted) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLocationLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = locationStatus,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Floating action button for centering on user location
            FloatingActionButton(
                onClick = { fetchLocationAndUpdateMap(forceRefresh = true) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                if (isLocationLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Center on my location"
                    )
                }
            }
            
        } else {
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
                    text = "This app needs location access to show nearby bus stops on the map.",
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
        
        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        // Error display
        error?.let { transitError ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(transitError.message)
            }
        }
    }
    
    // Custom modal bottom sheet for selected stop
    if (showBottomSheet && selectedStop != null) {
        CustomModalBottomSheet(
            onDismissRequest = { showBottomSheet = false }
        ) {
            val scrollState = rememberScrollState()
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Bus Stop Details",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                StopRow(
                    stop = selectedStop!!,
                    distance = selectedStop!!.getDistance(),
                    onNavigateToLiveStop = onNavigateToLiveStop
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        onNavigateToLiveStop(selectedStop!!.number)
                        showBottomSheet = false 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("View Live Arrivals")
                }
                
                Spacer(modifier = Modifier.height(32.dp)) // Extra space for navigation bar
            }
        }
    }
}

private fun getCustomMarkerIcon(context: Context, direction: String): BitmapDescriptor {
    val drawableId = when (direction.lowercase()) {
        "southbound", "south" -> R.drawable.green_ball
        "northbound", "north" -> R.drawable.orange_ball
        "eastbound", "east" -> R.drawable.pink_ball
        "westbound", "west" -> R.drawable.blue_ball
        else -> R.drawable.default_ball
    }
    
    val drawable = ContextCompat.getDrawable(context, drawableId)
    drawable?.let {
        val targetSize = (PeekTransitConstants.STOP_MARKER_SIZE_DP * context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        it.setBounds(0, 0, targetSize, targetSize)
        it.draw(canvas)
        
        // Create a more precise hit area by ensuring the bitmap is properly sized
        val descriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
        return descriptor
    }
    
    return BitmapDescriptorFactory.defaultMarker()
}
