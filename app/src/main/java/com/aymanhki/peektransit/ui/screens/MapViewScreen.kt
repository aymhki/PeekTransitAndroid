package com.aymanhki.peektransit.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aymanhki.peektransit.R
import com.aymanhki.peektransit.data.models.Stop
import com.aymanhki.peektransit.ui.components.StopRow
import com.aymanhki.peektransit.utils.PeekTransitConstants
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
import kotlinx.coroutines.delay
import androidx.core.graphics.createBitmap
import com.aymanhki.peektransit.ui.components.AddressSearchView
import com.aymanhki.peektransit.ui.components.DestinationSearchButton

@Composable
fun MapViewScreen(
    viewModel: MainViewModel,
    onNavigateToLiveStop: (Int) -> Unit = {},
    isCurrentDestination: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val currentTheme = settingsManager.stopViewTheme
    val systemDarkTheme = isSystemInDarkTheme()
    val isDarkTheme = when (currentTheme) {
        StopViewTheme.CLASSIC -> true
        StopViewTheme.MODERN -> systemDarkTheme
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    var isAppActive by remember { mutableStateOf(true) }

    LaunchedEffect(lifecycleState) {
        if (lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) {
            isAppActive = true
        } else {
            isAppActive = false
        }
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
    val isCameraPositioned by viewModel.isCameraPositioned.observeAsState(false)
    val hasInitialLocation by viewModel.hasInitialLocation.observeAsState(false)
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var locationStatus by remember { mutableStateOf("Initializing...") }
    var showMap by remember { mutableStateOf(false) }
    var isMapsInitialized by remember { mutableStateOf(false) }
    var isAnimatingCamera by remember { mutableStateOf(false) }
    val cameraLocation by viewModel.cameraLocation.observeAsState()
    val liveLocation by viewModel.currentLocation.observeAsState()
    val defaultLocation = LatLng(49.8951, -97.1384)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 11.0f)
    }
    var selectedStop by remember { mutableStateOf<Stop?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val isViewModelInitialized by viewModel.isInitialized.observeAsState(false)
    var isSearchingRoute by remember { mutableStateOf(false) }

    var highlightedStop by remember { mutableStateOf<Stop?>(null) }
    var showHighlightedStopCallout by remember { mutableStateOf(false) }

    var showInfoWindowForStop by remember { mutableStateOf<Int?>(null) }


    LaunchedEffect(Unit) {
        try {
            MapsInitializer.initialize(context, MapsInitializer.Renderer.LATEST) { result ->
                isMapsInitialized = true
                showMap = true
                locationStatus = "Map ready"
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

    LaunchedEffect(isCurrentDestination, locationPermissionsState.allPermissionsGranted, isMapsInitialized) {
        if (isCurrentDestination && locationPermissionsState.allPermissionsGranted && isMapsInitialized) {
            viewModel.fetchLocationForCamera()
        }
    }

    LaunchedEffect(cameraLocation, isMapsInitialized, showMap) {
        if (cameraLocation != null && isMapsInitialized && showMap && !isAnimatingCamera) {
            val newLatLng = LatLng(cameraLocation!!.latitude, cameraLocation!!.longitude)
            userLocation = newLatLng
            locationStatus = "Location: ${"%.4f".format(cameraLocation!!.latitude)}, ${"%.4f".format(cameraLocation!!.longitude)}"
            isAnimatingCamera = true
            try {
                val zoomLevel = if (hasInitialLocation) PeekTransitConstants.DEFAULT_MAP_ZOOM else 13.0f
                cameraPositionState.animate(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.fromLatLngZoom(newLatLng, zoomLevel)
                    ),
                    1000
                )
            } catch (e: Exception) {
                cameraPositionState.move(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.fromLatLngZoom(newLatLng, if (hasInitialLocation) PeekTransitConstants.DEFAULT_MAP_ZOOM else 13.0f)
                    )
                )
            } finally {
                isAnimatingCamera = false
            }
        }
    }

    LaunchedEffect(isMapsInitialized, showMap, hasInitialLocation, isLoadingLocation) {
        if (isMapsInitialized && showMap && !hasInitialLocation && !isLoadingLocation) {
            delay(PeekTransitConstants.CAMERA_DELAY_FOR_INITIAL_LOCATION_ZOOM_MS)
            if (!hasInitialLocation && cameraLocation == null) {
                locationStatus = "Showing Winnipeg area (location unavailable)"
                isAnimatingCamera = true
                try {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.fromLatLngZoom(defaultLocation, 11.0f)
                        ),
                        1000
                    )
                } catch (e: Exception) {
                    cameraPositionState.move(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.fromLatLngZoom(defaultLocation, 11.0f)
                        )
                    )
                } finally {
                    isAnimatingCamera = false
                }
            }
        }
    }

    LaunchedEffect(isLoadingLocation, isLoadingStops, hasInitialLocation) {
        when {
            isLoadingLocation -> locationStatus = "Getting your location..."
            isLoadingStops -> locationStatus = "Loading nearby stops..."
            !hasInitialLocation && locationPermissionsState.allPermissionsGranted ->
                locationStatus = "Location services unavailable"
        }
    }

    LaunchedEffect(locationPermissionsState.allPermissionsGranted, isMapsInitialized) {
        if (!locationPermissionsState.allPermissionsGranted) {
            locationStatus = "Location permission required"
        } else if (!isMapsInitialized) {
            locationStatus = "Initializing map..."
        }
    }

    val mapStyle = if (isDarkTheme) {
        MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
    } else {
        null
    }

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
                    onMapClick = {
                        showBottomSheet = false
                        if (isSearchingRoute) {
                            isSearchingRoute = false
                        }
                    },
                    properties = MapProperties(
                        isMyLocationEnabled = locationPermissionsState.allPermissionsGranted && hasInitialLocation,
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
                    if (hasInitialLocation && userLocation != null) {
                        Circle(
                            center = userLocation!!,
                            radius = PeekTransitConstants.STOPS_DISTANCE_RADIUS_IN_METERS,
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

                        val markerState = remember(stop.number) { MarkerState(position = position) }

                        LaunchedEffect(showInfoWindowForStop) {
                            if (showInfoWindowForStop == stop.number) {
                                markerState.showInfoWindow()
                            } else {
                                markerState.hideInfoWindow()
                            }
                        }

                        Marker(
                            state = markerState,
                            title = stop.name,
                            snippet = "Stop #${stop.number} - ${stop.direction}",
                            anchor = Offset(0.5f, 1.0f),
                            icon = getCustomMarkerIcon(context, stop.direction),
                            zIndex = 1.0f,
                            onClick = {
                                selectedStop = stop
                                showBottomSheet = true
                                false
                            },
                            onInfoWindowClick = {
                                selectedStop = stop
                                showBottomSheet = true
                                showInfoWindowForStop = null
                            }
                        )
                    }

                    highlightedStop?.let { stop ->
                        if (!stops.any { it.number == stop.number }) {
                            val position = LatLng(
                                stop.centre.geographic.latitude,
                                stop.centre.geographic.longitude
                            )

                            val markerState = remember(stop.number) { MarkerState(position = position) }

                            LaunchedEffect(showInfoWindowForStop) {
                                if (showInfoWindowForStop == stop.number) {
                                    markerState.showInfoWindow()
                                } else {
                                    markerState.hideInfoWindow()
                                }
                            }

                            Marker(
                                state = markerState,
                                title = stop.name,
                                snippet = "Stop #${stop.number} - ${stop.direction}",
                                anchor = Offset(0.5f, 1.0f),
                                icon = getCustomMarkerIcon(context, stop.direction),
                                zIndex = 2.0f,
                                onClick = {
                                    selectedStop = stop
                                    showBottomSheet = true
                                    false
                                },
                                onInfoWindowClick = {
                                    selectedStop = stop
                                    showBottomSheet = true
                                    showInfoWindowForStop = null
                                }
                            )
                        }
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AnimatedVisibility(
                        visible = !isSearchingRoute && error == null && !isLoadingStops && isAppActive && !isLoadingLocation,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
                    ) {
                        DestinationSearchButton(
                            onClick = { isSearchingRoute = true }
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    FloatingActionButton(
                        onClick = {
                            if (locationPermissionsState.allPermissionsGranted) {
                                viewModel.resetCameraPosition()
                                scope.launch {
                                    val currentLocation = viewModel.getCurrentLocationForCamera()
                                    if (currentLocation != null) {
                                        val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
                                        userLocation = latLng
                                        locationStatus = "Location: ${"%.4f".format(currentLocation.latitude)}, ${"%.4f".format(currentLocation.longitude)}"
                                        viewModel.updateCurrentLocation(currentLocation)
                                        isAnimatingCamera = true
                                        try {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newCameraPosition(
                                                    CameraPosition.fromLatLngZoom(latLng, PeekTransitConstants.DEFAULT_MAP_ZOOM)
                                                ),
                                                1000
                                            )
                                        } catch (e: Exception) {
                                            cameraPositionState.move(
                                                CameraUpdateFactory.newCameraPosition(
                                                    CameraPosition.fromLatLngZoom(latLng, PeekTransitConstants.DEFAULT_MAP_ZOOM)
                                                )
                                            )
                                        } finally {
                                            isAnimatingCamera = false
                                        }
                                    }
                                }
                            }
                            viewModel.retry()
                        },
                        modifier = Modifier,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                    ) {
                        if (isLoadingLocation || isLoadingStops || isAnimatingCamera) {
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
                }
            }

            AnimatedVisibility(
                visible = isSearchingRoute,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.5f))
                        .clickable { isSearchingRoute = false },
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp)
                    ) {
                        AddressSearchView(
                            onRouteSelected = { tripPlan, goToSchedule ->
                                val firstSegmentWithAStop = tripPlan.segments.first {
                                    it.fromStop != null && it.fromStop.key != -1 && it.fromStop.location != null ||
                                            it.toStop != null && it.toStop.key != -1 && it.toStop.location != null
                                }

                                if (firstSegmentWithAStop != null) {
                                    val targetStopNumber = if (firstSegmentWithAStop.fromStop != null && firstSegmentWithAStop.fromStop.key != -1) {
                                        firstSegmentWithAStop.fromStop.key
                                    } else if (firstSegmentWithAStop.toStop != null && firstSegmentWithAStop.toStop.key != -1) {
                                        firstSegmentWithAStop.toStop.key
                                    } else {
                                        -1
                                    }

                                    if (targetStopNumber != -1) {
                                        scope.launch {
                                            viewModel.getStop(targetStopNumber) { fetchedStop ->
                                                if (fetchedStop != null) {
                                                    scope.launch {
                                                        val isStopOnMap = stops.any { it.number == targetStopNumber }

                                                        if (!isStopOnMap) {
                                                            highlightedStop = fetchedStop
                                                            delay(500)
                                                        }

                                                        showInfoWindowForStop = targetStopNumber

                                                        val stopLatLng = LatLng(
                                                            fetchedStop.centre.geographic.latitude,
                                                            fetchedStop.centre.geographic.longitude
                                                        )

                                                        isAnimatingCamera = true

                                                        try {
                                                            cameraPositionState.animate(
                                                                CameraUpdateFactory.newCameraPosition(
                                                                    CameraPosition.fromLatLngZoom(stopLatLng, 20.0f)
                                                                ),
                                                                1500
                                                            )
                                                        } catch (e: Exception) {
                                                            cameraPositionState.move(
                                                                CameraUpdateFactory.newCameraPosition(
                                                                    CameraPosition.fromLatLngZoom(stopLatLng, 20.0f)
                                                                )
                                                            )
                                                        } finally {
                                                            isAnimatingCamera = false
                                                        }

                                                        selectedStop = fetchedStop
                                                        delay(500)

                                                        if (goToSchedule) {
                                                            onNavigateToLiveStop(targetStopNumber)
                                                        } else {
                                                            showBottomSheet = true
                                                            showInfoWindowForStop = null
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                } else {
                                    isSearchingRoute = false
                                }
                            },
                            userLocation = viewModel.currentLocation.value,
                            onClose = { isSearchingRoute = false }
                        )
                    }
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
        val bitmap = createBitmap(targetSize, targetSize)
        val canvas = Canvas(bitmap)
        it.setBounds(0, 0, targetSize, targetSize)
        it.draw(canvas)
        val descriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
        return descriptor
    }
    return BitmapDescriptorFactory.defaultMarker()
}

