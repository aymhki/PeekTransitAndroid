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
import com.aymanhki.peektransit.R
import com.aymanhki.peektransit.data.models.Stop
import com.aymanhki.peektransit.ui.components.StopRow
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.location.LocationManagerProvider
import com.aymanhki.peektransit.viewmodel.MainViewModel
import com.aymanhki.peektransit.utils.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MapStyleOptions


@Composable
fun MapViewScreen(
    viewModel: MainViewModel,
    onNavigateToLiveStop: (Int) -> Unit = {},
    isCurrentDestination: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationManager = remember { LocationManagerProvider.getInstance(context) }
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val currentTheme = settingsManager.stopViewTheme
    val systemDarkTheme = isSystemInDarkTheme()
    
    val isDarkTheme = when (currentTheme) {
        StopViewTheme.CLASSIC -> true
        StopViewTheme.MODERN -> systemDarkTheme
    }
    
    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    )
    
    val stops by viewModel.stops.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val error by viewModel.error.observeAsState()
    
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
            LatLng(49.8951, -97.1384),
            PeekTransitConstants.DEFAULT_MAP_ZOOM
        )
    }
    
    var selectedStop by remember { mutableStateOf<Stop?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        try {
            MapsInitializer.initialize(context, MapsInitializer.Renderer.LATEST) { result ->
                when (result) {
                    MapsInitializer.Renderer.LATEST -> {
                        isMapsInitialized = true
                        showMap = true // Show map immediately after initialization
                        locationStatus = "Maps initialized"
                    }
                    MapsInitializer.Renderer.LEGACY -> {
                        isMapsInitialized = true
                        showMap = true // Show map immediately after initialization
                        locationStatus = "Maps initialized (legacy)"
                    }
                }
            }
        } catch (e: Exception) {
            locationStatus = "Maps initialization failed: ${e.message}"
            isMapsInitialized = true
            showMap = true // Show map even if initialization failed
        }
    }
    
    fun fetchLocationAndUpdateMap(forceRefresh: Boolean = false) {
        scope.launch {
            isLocationLoading = true
            locationStatus = "Fetching location..."
            
            try {
                val location = locationManager.getCurrentLocation(forceRefresh)
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    val previousLocation = userLocation
                    userLocation = latLng
                    
                    if (viewModel.currentLocation.value == null) {
                        viewModel.updateCurrentLocation(location)
                    }
                    
                    val shouldUpdateStops = if (previousLocation != null) {
                        val distance = FloatArray(1)
                        android.location.Location.distanceBetween(
                            previousLocation.latitude, previousLocation.longitude,
                            latLng.latitude, latLng.longitude,
                            distance
                        )
                        distance[0] > PeekTransitConstants.DISTANCE_CHANGE_ALLOWED_BEFORE_REFRESHING_STOPS
                    } else {
                        true
                    }
                    
                    if (isInitialLoad && isMapsInitialized) {
                        try {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.fromLatLngZoom(latLng, PeekTransitConstants.DEFAULT_MAP_ZOOM)
                                ),
                                1500
                            )
                            hasCameraInitializedToUserLocation = true
                        } catch (e: Exception) {
                            cameraPositionState.move(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.fromLatLngZoom(latLng, PeekTransitConstants.DEFAULT_MAP_ZOOM)
                                )
                            )
                            hasCameraInitializedToUserLocation = true
                        }
                    } else if (forceRefresh && isMapsInitialized) {
                        try {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.fromLatLngZoom(latLng, PeekTransitConstants.DEFAULT_MAP_ZOOM)
                                )
                            )
                        } catch (e: Exception) {
                            cameraPositionState.move(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.fromLatLngZoom(latLng, PeekTransitConstants.DEFAULT_MAP_ZOOM)
                                )
                            )
                        }
                    }
                    
                    locationStatus = "Location: ${"%.4f".format(location.latitude)}, ${"%.4f".format(location.longitude)}"
                    
                    if (shouldUpdateStops || forceRefresh) {
                        // Removed unnecessary 500ms delay for better performance
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
                }
            } catch (e: Exception) {
                locationStatus = "Location error: ${e.message}"
            } finally {
                isLocationLoading = false
            }
        }
    }
    
    val isViewModelInitialized by viewModel.isInitialized.observeAsState(false)
    
    val liveLocation by viewModel.currentLocation.observeAsState()
    
    LaunchedEffect(isViewModelInitialized) {
        if (isViewModelInitialized) {
            isInitialLoad = false
            isLocationLoading = false
        }
    }

    LaunchedEffect(showMap, isViewModelInitialized, isMapsInitialized, hasCameraInitializedToUserLocation) {
        if (showMap && isViewModelInitialized && isMapsInitialized && !hasCameraInitializedToUserLocation) {
            liveLocation?.let { location ->
                val latLng = LatLng(location.latitude, location.longitude)
                userLocation = latLng
                locationStatus = "Location: ${"%.4f".format(location.latitude)}, ${"%.4f".format(location.longitude)}"
                
                try {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.fromLatLngZoom(latLng, PeekTransitConstants.DEFAULT_MAP_ZOOM)
                        ),
                        1500
                    )
                    hasCameraInitializedToUserLocation = true
                    println("MapViewScreen: Camera initialized to user location (switched from another tab)")
                } catch (e: Exception) {
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
    
    LaunchedEffect(liveLocation) {
        liveLocation?.let { location ->
            val newLatLng = LatLng(location.latitude, location.longitude)
            val previousLocation = userLocation
            
            userLocation = newLatLng
            
            if (previousLocation != null && isMapsInitialized && showMap) {
                val distance = FloatArray(1)
                android.location.Location.distanceBetween(
                    previousLocation.latitude, previousLocation.longitude,
                    newLatLng.latitude, newLatLng.longitude,
                    distance
                )
                
                if (distance[0] > 50.0f) {
                    try {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.fromLatLngZoom(newLatLng, cameraPositionState.position.zoom)
                            ),
                            1000
                        )
                    } catch (e: Exception) {
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
    
    LaunchedEffect(locationPermissionsState.allPermissionsGranted, isMapsInitialized, isCurrentDestination) {
        if (locationPermissionsState.allPermissionsGranted && isMapsInitialized && isCurrentDestination) {
            println("MapViewScreen: Triggering fetchLocationAndUpdateMap (app opened on map tab)")
            fetchLocationAndUpdateMap()
        } else if (!locationPermissionsState.allPermissionsGranted) {
            locationStatus = "Location permission required"
        } else if (!isMapsInitialized) {
            locationStatus = "Initializing maps..."
        }
    }
    
    val mapStyle = if (isDarkTheme) {
        MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
    } else {
        null
    }
    
    fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (locationPermissionsState.allPermissionsGranted) {
            if (!showMap) {
                // Simplified loading screen - only show while maps aren't initialized
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
                userLocation?.let { location ->
                    Circle(
                        center = location,
                        radius = PeekTransitConstants.STOPS_DISTANCE_RADIUS,
                        strokeColor = MaterialTheme.colorScheme.secondary,
                        fillColor = androidx.compose.ui.graphics.Color.Transparent,
                        strokeWidth = 3f
                    )
                }
                
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
                
                Spacer(modifier = Modifier.height(32.dp))
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
        
        val descriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
        return descriptor
    }
    
    return BitmapDescriptorFactory.defaultMarker()
}
