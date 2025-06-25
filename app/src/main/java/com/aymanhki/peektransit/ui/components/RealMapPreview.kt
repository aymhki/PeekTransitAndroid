package com.aymanhki.peektransit.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.aymanhki.peektransit.R
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*

@Composable
fun RealMapPreview(
    latitude: Double,
    longitude: Double,
    direction: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val position = LatLng(latitude, longitude)
    var isMapsInitialized by remember { mutableStateOf(false) }
    
    // Initialize Google Maps SDK
    LaunchedEffect(Unit) {
        try {
            MapsInitializer.initialize(context, MapsInitializer.Renderer.LATEST) { result ->
                isMapsInitialized = true
            }
        } catch (e: Exception) {
            isMapsInitialized = true // Allow to proceed even if initialization fails
        }
    }
    
    val cameraPositionState = rememberCameraPositionState {
        this.position = CameraPosition.fromLatLngZoom(position, 17f)
    }
    
    // Dark mode map style
    val mapStyle = if (isDarkTheme) {
        MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
    } else {
        null
    }
    
    Box(
        modifier = modifier.clip(RoundedCornerShape(8.dp))
    ) {
        if (isMapsInitialized) {
            GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapStyleOptions = mapStyle,
                isMyLocationEnabled = false
            ),
            uiSettings = MapUiSettings(
                compassEnabled = false,
                indoorLevelPickerEnabled = false,
                mapToolbarEnabled = false,
                myLocationButtonEnabled = false,
                rotationGesturesEnabled = false,
                scrollGesturesEnabled = false,
                tiltGesturesEnabled = false,
                zoomControlsEnabled = false,
                zoomGesturesEnabled = false
            )
        ) {
            // Add marker for the bus stop
            Marker(
                state = MarkerState(position = position),
                title = "Bus Stop",
                snippet = direction,
                icon = getCustomMarkerIcon(context, direction),
                anchor = Offset(0.5f, 1.0f)
            )
        }
        }
    }
}

private fun getCustomMarkerIcon(context: Context, direction: String): com.google.android.gms.maps.model.BitmapDescriptor {
    val drawableId = when (direction.lowercase()) {
        "southbound", "south" -> R.drawable.green_ball
        "northbound", "north" -> R.drawable.orange_ball
        "eastbound", "east" -> R.drawable.pink_ball
        "westbound", "west" -> R.drawable.blue_ball
        else -> R.drawable.default_ball
    }
    
    val drawable = ContextCompat.getDrawable(context, drawableId)
    drawable?.let {
        val targetSize =  (PeekTransitConstants.STOP_MARKER_SIZE_DP * context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        it.setBounds(0, 0, targetSize, targetSize)
        it.draw(canvas)
        return com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(bitmap)
    }
    
    return com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker()
}