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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.aymanhki.peektransit.R
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.data.cache.MapSnapshotCache
import com.aymanhki.peektransit.utils.PeekTransitConstants.ACCENT_COLOR_IN_ALL_THEMES
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.graphics.createBitmap
import com.aymanhki.peektransit.data.models.SavedStopsViewMode

@Composable
fun MapPreview(
    latitude: Double,
    longitude: Double,
    direction: String,
    modifier: Modifier = Modifier,
    stopViewMode: SavedStopsViewMode,
    sizeWidth: Int,
    sizeHeight: Int,
    renderWidth: Int,
    renderHeight: Int,
    markerSize: Int,
    zoomLevel: Float,
    bottomBannerPercentage: Float,
    bottomBannerColor: Color,
    bottomBannerOpacity: Float,
    showBottomBanner: Boolean
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val currentTheme = settingsManager.stopViewTheme
    val systemDarkTheme = isSystemInDarkTheme()

    val isDarkMode = when (currentTheme) {
        StopViewTheme.CLASSIC -> true
        StopViewTheme.MODERN -> systemDarkTheme
    }

    val key = "$latitude-$longitude-$direction-$isDarkMode-$stopViewMode"
    val scope = rememberCoroutineScope()
    var snapshotBitmap by remember(key) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(key) { mutableStateOf(true) }
    var hasError by remember(key) { mutableStateOf(false) }
    var isMapsInitialized by remember { mutableStateOf(false) }
    
    LaunchedEffect(latitude, longitude, direction, isDarkMode, stopViewMode) {
        val cachedSnapshot = MapSnapshotCache.getCachedSnapshot(latitude, longitude, direction, isDarkMode, stopViewMode)
        if (cachedSnapshot != null) {
            snapshotBitmap = if (showBottomBanner) {
                cropBitmapFromBottom(cachedSnapshot, bottomBannerPercentage)
            } else {
                cachedSnapshot
            }
            isLoading = false
            hasError = false
            return@LaunchedEffect
        }
        
        try {
            MapsInitializer.initialize(context, MapsInitializer.Renderer.LATEST) { result ->
                isMapsInitialized = true
            }
        } catch (e: Exception) {
            hasError = true
            isLoading = false
            isMapsInitialized = true
        }
    }
    
    fun takeSnapshotSafely(googleMap: com.google.android.gms.maps.GoogleMap) {
        try {
            googleMap.setOnMapLoadedCallback {
                scope.launch {
                    delay(200)
                    
                    googleMap.snapshot { bitmap ->
                        if (bitmap != null) {
                            snapshotBitmap = if (showBottomBanner) {
                                cropBitmapFromBottom(bitmap, bottomBannerPercentage)
                            } else {
                                bitmap
                            }
                            hasError = false
                            scope.launch {
                                MapSnapshotCache.cacheSnapshot(latitude, longitude, direction, isDarkMode, stopViewMode, bitmap)
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
           .size(width = sizeWidth.dp, height = sizeHeight.dp)
           .clip(RoundedCornerShape(8.dp))
    ) {
        when {
            snapshotBitmap != null -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        bitmap = snapshotBitmap!!.asImageBitmap(),
                        contentDescription = "Map preview for $direction",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                }
            }
            
            else -> {
                if (isMapsInitialized) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AndroidView(
                            factory = { context ->
                                MapView(context).apply {
                                    onCreate(null)
                                    onResume()
                                    
                                    getMapAsync { googleMap ->
                                        try {
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

                                            val styleResId = if (isDarkMode) {
                                                R.raw.map_style_dark
                                            } else {
                                                R.raw.map_style_light
                                            }
                                            
                                            googleMap.mapType = com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL

                                            try {
                                                val mapStyle = MapStyleOptions.loadRawResourceStyle(context, styleResId)
                                                googleMap.setMapStyle(mapStyle)
                                            } catch (e: Exception) {
                                                println("Failed to load map style: ${e.message}")
                                            }
                                            
                                            val target = LatLng(latitude, longitude)
                                            val zoomLevel = zoomLevel
                                            val cameraPosition = CameraPosition.Builder()
                                                .target(target)
                                                .zoom(zoomLevel)
                                                .build()
                                            
                                            if (isMapsInitialized) {
                                                googleMap.moveCamera(
                                                    CameraUpdateFactory.newCameraPosition(cameraPosition)
                                                )
                                            }
                                            
                                            googleMap.addMarker(
                                                MarkerOptions()
                                                    .position(target)
                                                    .icon(getCustomMarkerIconForPreview(context, direction, markerSize))
                                                    .anchor(PeekTransitConstants.MAP_PREVIEW_MARKER_ANCHOR_X_OFFSET, PeekTransitConstants.MAP_PREVIEW_MARKER_ANCHOR_Y_OFFSET)

                                            )
                                            
                                            takeSnapshotSafely(googleMap)
                                            
                                        } catch (e: Exception) {
                                            hasError = true
                                            isLoading = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(width = renderWidth.dp, height = renderHeight.dp)
                        )
                        
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

//                        if (showBottomBanner) {
//                            Box(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .fillMaxHeight(bottomBannerPercentage)
//                                    .background(bottomBannerColor.copy(alpha = bottomBannerOpacity))
//                                    .align(Alignment.BottomCenter)
//                            )
//                        }
                    }
                } else {
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

        }
    }
}

private fun getDirectionColor(direction: String): Color {
    return ACCENT_COLOR_IN_ALL_THEMES
}

private fun getCustomMarkerIconForPreview(context: Context, direction: String, markerSize: Int): BitmapDescriptor {
    val drawableId = when (direction.lowercase()) {
        "southbound", "south" -> R.drawable.green_ball
        "northbound", "north" -> R.drawable.orange_ball
        "eastbound", "east" -> R.drawable.pink_ball
        "westbound", "west" -> R.drawable.blue_ball
        else -> R.drawable.default_ball
    }
    
    val drawable = ContextCompat.getDrawable(context, drawableId)
    drawable?.let {
        val targetSize = (markerSize * context.resources.displayMetrics.density).toInt()
        val bitmap = createBitmap(targetSize, targetSize)
        val canvas = Canvas(bitmap)
        it.setBounds(0, 0, targetSize, targetSize)
        it.draw(canvas)
        
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
    
    return BitmapDescriptorFactory.defaultMarker()
}


private fun cropBitmapFromBottom(sourceBitmap: Bitmap, percentageToCrop: Float): Bitmap {
    if (percentageToCrop <= 0f || percentageToCrop >= 1f) {
        return sourceBitmap
    }
    val width = sourceBitmap.width
    val originalHeight = sourceBitmap.height
    val newHeight = (originalHeight * (1 - percentageToCrop)).toInt()

    if (newHeight <= 0) {
        return createBitmap(1, 1)
    }
    return Bitmap.createBitmap(sourceBitmap, 0, 0, width, newHeight)
}
