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
import com.aymanhki.peektransit.ui.components.ErrorSnackbar
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
    val isLoadingStops by viewModel.isLoadingStops.observeAsState(false)
    val isLoadingLocation by viewModel.isLoadingLocation.observeAsState(false)
    val isLoading by viewModel.isLoading.observeAsState(false)
    val error by viewModel.error.observeAsState()
    val locationError by viewModel.locationError.observeAsState()
    
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var locationStatus by remember { mutableStateOf("Initializing...") }
    var showMap by remember { mutableStateOf(false) }
    var isMapsInitialized by remember { mutableStateOf(false) }
    var hasCameraInitializedToUserLocation by remember { mutableStateOf(false) }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(49.8951, -97.1384),
            0f
        )
    }
    
    var selectedStop by remember { mutableStateOf<Stop?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    
    val isViewModelInitialized by viewModel.isInitialized.observeAsState(false)
    val liveLocation by viewModel.currentLocation.observeAsState()
    
    LaunchedEffect(Unit) {
        try {
            MapsInitializer.initialize(context, MapsInitializer.Renderer.LATEST) { result ->
                when (result) {
                    MapsInitializer.Renderer.LATEST -> {
                        isMapsInitialized = true
                        showMap = true
                        locationStatus = "Maps initialized"
                    }
                    MapsInitializer.Renderer.LEGACY -> {
                        isMapsInitialized = true
                        showMap = true
                        locationStatus = "Maps initialized (legacy)"
                    }
                }
            }
        } catch (e: Exception) {
            locationStatus = "Maps initialization failed: ${e.message}"
            isMapsInitialized = true
            showMap = true
        }
    }
    
    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            viewModel.initializeGlobal()
        }
    }
    
    LaunchedEffect(locationPermissionsState.allPermissionsGranted, showMap, isMapsInitialized, liveLocation, hasCameraInitializedToUserLocation) {
        if (locationPermissionsState.allPermissionsGranted && showMap && isMapsInitialized && !hasCameraInitializedToUserLocation) {

            try {
                val currentLocation = liveLocation ?: viewModel.getCurrentLocationForCamera()
                
                if (currentLocation != null) {
                    val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
                    userLocation = latLng
                    locationStatus = "Location: ${"%.4f".format(currentLocation.latitude)}, ${"%.4f".format(currentLocation.longitude)}"
                    
                    if (liveLocation == null) {
                        viewModel.updateCurrentLocation(currentLocation)
                    }
                    
                    try {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.fromLatLngZoom(latLng, PeekTransitConstants.DEFAULT_MAP_ZOOM)
                            ),
                            1500
                        )
                        println("MapViewScreen: Camera animated to user location successfully")
                    } catch (animationException: Exception) {
                        println("MapViewScreen: Animation failed, using immediate move: ${animationException.message}")
                        cameraPositionState.move(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.fromLatLngZoom(latLng, PeekTransitConstants.DEFAULT_MAP_ZOOM)
                            )
                        )
                        println("MapViewScreen: Camera moved to user location (fallback)")
                    }
                    
                    hasCameraInitializedToUserLocation = true
                    println("MapViewScreen: Camera positioning completed successfully")
                    
                } else {
                    println("MapViewScreen: No location available for camera positioning")
                    locationStatus = "Unable to get location"
                }
            } catch (e: Exception) {
                println("MapViewScreen: Failed to position camera: ${e.message}")
                locationStatus = "Location error: ${e.message}"
            }
        }
    }

    LaunchedEffect(isCurrentDestination) {
        if (isCurrentDestination && locationPermissionsState.allPermissionsGranted && showMap && isMapsInitialized && !hasCameraInitializedToUserLocation) {
            try {
                val currentLocation = liveLocation ?: viewModel.getCurrentLocationForCamera()
                
                if (currentLocation != null) {
                    val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
                    userLocation = latLng
                    locationStatus = "Location: ${"%.4f".format(currentLocation.latitude)}, ${"%.4f".format(currentLocation.longitude)}"
                    
                    if (liveLocation == null) {
                        viewModel.updateCurrentLocation(currentLocation)
                    }
                    
                    try {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.fromLatLngZoom(latLng, PeekTransitConstants.DEFAULT_MAP_ZOOM)
                            ),
                            1500
                        )
                        println("MapViewScreen: Fallback camera animated to user location successfully")
                    } catch (animationException: Exception) {
                        cameraPositionState.move(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.fromLatLngZoom(latLng, PeekTransitConstants.DEFAULT_MAP_ZOOM)
                            )
                        )
                        println("MapViewScreen: Fallback camera moved to user location (immediate)")
                    }
                    
                    hasCameraInitializedToUserLocation = true
                    println("MapViewScreen: Fallback camera positioning completed successfully")
                }
            } catch (e: Exception) {
                println("MapViewScreen: Fallback camera positioning failed: ${e.message}")
            }
        }
    }
    
    LaunchedEffect(locationPermissionsState.allPermissionsGranted, showMap, isMapsInitialized) {
        if (locationPermissionsState.allPermissionsGranted && showMap && isMapsInitialized && !hasCameraInitializedToUserLocation) {

            kotlinx.coroutines.delay(5000)
            
            if (liveLocation == null && !hasCameraInitializedToUserLocation) {

                val defaultLatLng = LatLng(49.8951, -97.1384)
                locationStatus = "Using default location (Winnipeg)"
                
                try {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.fromLatLngZoom(defaultLatLng, 11.0f)
                        ),
                        1500
                    )
                    println("MapViewScreen: Camera positioned to default location successfully")
                } catch (animationException: Exception) {
                    cameraPositionState.move(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.fromLatLngZoom(defaultLatLng, 11.0f)
                        )
                    )
                    println("MapViewScreen: Camera moved to default location (immediate)")
                }
                
                hasCameraInitializedToUserLocation = true
                println("MapViewScreen: Timeout fallback camera positioning completed")
            }
        }
    }
    
    LaunchedEffect(liveLocation, hasCameraInitializedToUserLocation) {
        if (hasCameraInitializedToUserLocation && liveLocation != null) {
            val newLatLng = LatLng(liveLocation!!.latitude, liveLocation!!.longitude)
            val previousLocation = userLocation
            
            userLocation = newLatLng
            locationStatus = "Location: ${"%.4f".format(liveLocation!!.latitude)}, ${"%.4f".format(liveLocation!!.longitude)}"
            
            if (previousLocation != null && isMapsInitialized && showMap) {
                val distance = FloatArray(1)
                android.location.Location.distanceBetween(
                    previousLocation.latitude, previousLocation.longitude,
                    newLatLng.latitude, newLatLng.longitude,
                    distance
                )
                
                if (distance[0] > PeekTransitConstants.MAP_CAMERA_UPDATE_THRESHOLD_METERS) {
                    try {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.fromLatLngZoom(newLatLng, cameraPositionState.position.zoom)
                            ),
                            PeekTransitConstants.MAP_CAMERA_ANIMATION_DURATION_MS
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
    
    LaunchedEffect(isLoadingLocation, isLoadingStops) {
        when {
            isLoadingLocation -> locationStatus = "Getting your location..."
            isLoadingStops -> locationStatus = "Loading nearby stops..."
        }
    }
    
    LaunchedEffect(locationPermissionsState.allPermissionsGranted, isMapsInitialized) {
        if (!locationPermissionsState.allPermissionsGranted) {
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
                        text = if (locationStatus.contains("Initializing") || locationStatus.contains("Maps")) {
                            "Loading Map..."
                        } else {
                            "Getting Your Location..."
                        },
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
                        if (isLoadingLocation || isLoadingStops) {
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
                onClick = {
                    if (locationPermissionsState.allPermissionsGranted) {
                        scope.launch {
                            try {
                                val currentLocation = viewModel.getCurrentLocationForCamera()
                                if (currentLocation != null) {
                                    val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
                                    userLocation = latLng
                                    locationStatus = "Location: ${"%.4f".format(currentLocation.latitude)}, ${"%.4f".format(currentLocation.longitude)}"
                                    viewModel.updateCurrentLocation(currentLocation)
                                    
                                    try {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newCameraPosition(
                                                CameraPosition.fromLatLngZoom(latLng, PeekTransitConstants.DEFAULT_MAP_ZOOM)
                                            ),
                                            1500
                                        )
                                    } catch (e: Exception) {
                                        cameraPositionState.move(
                                            CameraUpdateFactory.newCameraPosition(
                                                CameraPosition.fromLatLngZoom(latLng, PeekTransitConstants.DEFAULT_MAP_ZOOM)
                                            )
                                        )
                                    }
                                    hasCameraInitializedToUserLocation = true
                                    println("MapViewScreen: Camera reset to user location")
                                } else {
                                    println("MapViewScreen: No location available for camera reset")
                                }
                            } catch (e: Exception) {
                                println("MapViewScreen: Failed to get location for camera reset: ${e.message}")
                            }
                        }
                    }
                    viewModel.retry()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                if (isLoadingLocation || isLoadingStops) {
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
        
        if (isLoadingStops || isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        val currentError = error ?: locationError
        currentError?.let { transitError ->
            ErrorSnackbar(
                error = transitError,
                onRetry = {
                    viewModel.clearError()
                    viewModel.clearLocationError()
                    viewModel.retry()
                },
                onDismiss = {
                    viewModel.clearError()
                    viewModel.clearLocationError()
                },
                retryButtonText = when {
                    locationError != null -> "Retry Location"
                    else -> "Retry"
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 88.dp)
            )
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
