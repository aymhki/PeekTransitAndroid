package com.aymanhki.peektransit.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import com.aymanhki.peektransit.managers.SettingsManager
import com.aymanhki.peektransit.utils.StopViewTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.aymanhki.peektransit.R
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.data.cache.MapSnapshotCache
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * MapPreview component using individual MapViews with crash-safe snapshots.
 * Each preview gets its own MapView instance to avoid the blurry/identical snapshot issues.
 * Both cropped and uncropped versions render identical content, just with different crop margins.
 */
@Composable
fun MapPreview(
    latitude: Double,
    longitude: Double,
    direction: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val currentTheme = settingsManager.stopViewTheme
    val systemDarkTheme = isSystemInDarkTheme()
    
    // Force dark theme for Classic theme, otherwise follow system theme for Modern
    val isDarkMode = when (currentTheme) {
        StopViewTheme.CLASSIC -> true
        StopViewTheme.MODERN -> systemDarkTheme
    }
    val scope = rememberCoroutineScope()
    
    var snapshotBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var isMapsInitialized by remember { mutableStateOf(false) }
    
    // Check cache first (both memory and disk)
    LaunchedEffect(latitude, longitude, direction, isDarkMode) {
        val cachedSnapshot = MapSnapshotCache.getCachedSnapshot(latitude, longitude, direction, isDarkMode)
        if (cachedSnapshot != null) {
            snapshotBitmap = cachedSnapshot
            isLoading = false
            hasError = false
            return@LaunchedEffect
        }
        
        // If not cached, initialize Maps SDK for generation
        try {
            MapsInitializer.initialize(context, MapsInitializer.Renderer.LATEST) { result ->
                isMapsInitialized = true
            }
        } catch (e: Exception) {
            hasError = true
            isLoading = false
            isMapsInitialized = true // Allow to proceed even if initialization fails
        }
    }
    
    // Function to take snapshot with crash prevention and crop out Google logo
    fun takeSnapshotSafely(googleMap: com.google.android.gms.maps.GoogleMap) {
        try {
            // 🔥 CRITICAL FIX: Use setOnMapLoadedCallback to prevent crashes
            // This is the exact fix from the GitHub issue - ensures tiles are loaded
            googleMap.setOnMapLoadedCallback {
                scope.launch {
                    // Add small delay to ensure everything is fully rendered
                    delay(200)
                    
                    googleMap.snapshot { bitmap ->
                        if (bitmap != null) {
                            snapshotBitmap = bitmap
                            hasError = false
                            // Cache the snapshot for future use (async)
                            scope.launch {
                                MapSnapshotCache.cacheSnapshot(latitude, longitude, direction, isDarkMode, bitmap)
                            }
                        } else {
                            hasError = true
                        }
                        isLoading = false
                    }
                }
            }
        } catch (e: Exception) {
            hasError = true
            isLoading = false
        }
    }
    
    Box(
        modifier = modifier
           .size(width = PeekTransitConstants.MAP_PREVIEW_WIDTH_SIZE_DP.dp, height = PeekTransitConstants.MAP_PREVIEW_HEIGHT_SIZE_DP.dp)
           .clip(RoundedCornerShape(8.dp))
    ) {
        when {
            snapshotBitmap != null -> {
                // Show the captured and cropped snapshot
                Image(
                    bitmap = snapshotBitmap!!.asImageBitmap(),
                    contentDescription = "Map preview for $direction",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            else -> {
                // Only create MapView if we need to generate a snapshot and maps is initialized
                if (isMapsInitialized) {
                    // Use a properly sized but off-screen MapView for high-quality snapshots
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Off-screen MapView for snapshot generation
                        AndroidView(
                            factory = { context ->
                                MapView(context).apply {
                                    onCreate(null)
                                    onResume()
                                    
                                    getMapAsync { googleMap ->
                                        try {
                                            // Configure map settings to hide all UI elements
                                            googleMap.uiSettings.apply {
                                                isMapToolbarEnabled = false
                                                isMyLocationButtonEnabled = false
                                                isZoomControlsEnabled = false
                                                isCompassEnabled = false
                                                isRotateGesturesEnabled = false
                                                isScrollGesturesEnabled = false
                                                isTiltGesturesEnabled = false
                                                isZoomGesturesEnabled = false
                                                isIndoorLevelPickerEnabled = false
                                            }
                                            
                                            // Set map type
                                            googleMap.mapType = com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL
                                            
                                            // Apply dark theme if needed
                                            if (isDarkMode) {
                                                try {
                                                    val darkStyle = MapStyleOptions.loadRawResourceStyle(
                                                        context, R.raw.map_style_dark
                                                    )
                                                    googleMap.setMapStyle(darkStyle)
                                                } catch (e: Exception) {
                                                    // Ignore style errors
                                                }
                                            }
                                            
                                            // Set camera position with zoom level based on crop mode
                                            val target = LatLng(latitude, longitude)
                                            val zoomLevel = PeekTransitConstants.MAP_PREVIEW_ZOOM_LEVEL
                                            val cameraPosition = CameraPosition.Builder()
                                                .target(target)
                                                .zoom(zoomLevel)
                                                .build()
                                            
                                            if (isMapsInitialized) {
                                                googleMap.moveCamera(
                                                    CameraUpdateFactory.newCameraPosition(cameraPosition)
                                                )
                                            }
                                            
                                            // Add marker
                                            googleMap.addMarker(
                                                MarkerOptions()
                                                    .position(target)
                                                    .icon(getCustomMarkerIconForPreview(context, direction))
                                                    .anchor(0.5f, 1.0f)
                                            )
                                            
                                            // Take snapshot with crash prevention and cropping
                                            takeSnapshotSafely(googleMap)
                                            
                                        } catch (e: Exception) {
                                            hasError = true
                                            isLoading = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(width = PeekTransitConstants.MAP_PREVIEW_RENDER_WIDTH_SIZE_DP.dp ,height = PeekTransitConstants.MAP_PREVIEW_RENDER_HEIGHT_SIZE_DP.dp)
                               // .offset(x = (-500).dp, y = (-500).dp) // Move far off-screen
                        )
                        
                        // Loading/Error overlay that covers the visible area
                        if (isLoading || hasError) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (hasError) Color(0xFFE0E0E0) else Color(0x88E8F5E8)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (hasError) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Map preview unavailable",
                                        tint = getDirectionColor(direction),
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = getDirectionColor(direction)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Show loading indicator while waiting for cache check or maps initialization
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x88E8F5E8)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = getDirectionColor(direction)
                        )
                    }
                }
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            // Cleanup is handled by MapView lifecycle
            // Note: Cached snapshots are retained for performance
        }
    }
}

/**
 * Get direction-based color for UI elements
 */
private fun getDirectionColor(direction: String): Color {
    return when (direction.lowercase()) {
        "northbound", "north" -> Color(0xFF4CAF50) // Green  
        "southbound", "south" -> Color(0xFFFF9800) // Orange
        "eastbound", "east" -> Color(0xFF2196F3) // Blue
        "westbound", "west" -> Color(0xFFE91E63) // Pink
        else -> Color(0xFF9E9E9E) // Gray
    }
}

/**
 * Get custom marker icon for preview with size based on cropping mode
 */
private fun getCustomMarkerIconForPreview(context: Context, direction: String): BitmapDescriptor {
    val drawableId = when (direction.lowercase()) {
        "southbound", "south" -> R.drawable.green_ball
        "northbound", "north" -> R.drawable.orange_ball
        "eastbound", "east" -> R.drawable.pink_ball
        "westbound", "west" -> R.drawable.blue_ball
        else -> R.drawable.default_ball
    }
    
    val drawable = ContextCompat.getDrawable(context, drawableId)
    drawable?.let {
        val markerSizeDp = PeekTransitConstants.MAP_PREVIEW_MARKER_SIZE_DP
        val targetSize = (markerSizeDp * context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        it.setBounds(0, 0, targetSize, targetSize)
        it.draw(canvas)
        
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
    
    return BitmapDescriptorFactory.defaultMarker()
}