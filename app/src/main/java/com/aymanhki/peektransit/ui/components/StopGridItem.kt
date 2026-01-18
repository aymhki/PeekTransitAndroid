package com.aymanhki.peektransit.ui.components


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aymanhki.peektransit.data.models.SavedStopsViewMode
import com.aymanhki.peektransit.data.models.Stop
import com.aymanhki.peektransit.managers.SavedStopsManager
import com.aymanhki.peektransit.utils.PeekTransitConstants
import kotlin.times


@Composable
fun StopGridItem(
    stop: Stop,
    stopViewMode: SavedStopsViewMode,
    onNavigateToLiveStop: (Int) -> Unit,
) {
    val context = LocalContext.current
    val savedStopsManager = remember { SavedStopsManager.getInstance(context) }
    val isStopSaved = savedStopsManager.isStopSavedFlow(stop.number.toString()).collectAsState(false)
    val cardHeight = if (stopViewMode == SavedStopsViewMode.GRID_2) PeekTransitConstants.TOTAL_GRID_CARD_HEIGHT_DP_IN_2X2_GRID_DP.dp else PeekTransitConstants.TOTAL_GRID_CARD_HEIGHT_DP_IN_3X3_GRID_DP.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clickable { onNavigateToLiveStop(stop.number) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 0.dp, start = 12.dp, end = 12.dp, top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BoxWithConstraints {
                val density = LocalDensity.current
                val widthPx = with(density) { maxWidth.roundToPx() }

                MapPreview(
                    latitude = stop.centre.geographic.latitude,
                    longitude = stop.centre.geographic.longitude,
                    direction = stop.direction,
                    sizeWidth = widthPx,
                    sizeHeight = if (stopViewMode == SavedStopsViewMode.GRID_2) PeekTransitConstants.MAP_PREVIEW_HEIGHT_SIZE_DP_IN_2X2_GRID else PeekTransitConstants.MAP_PREVIEW_HEIGHT_SIZE_DP_IN_3X3_GRID,
                    renderWidth = widthPx,
                    renderHeight = if (stopViewMode == SavedStopsViewMode.GRID_2) PeekTransitConstants.MAP_PREVIEW_RENDER_HEIGHT_SIZE_DP_IN_2X2_GRID else PeekTransitConstants.MAP_PREVIEW_RENDER_HEIGHT_SIZE_DP_IN_3X3_GRID,
                    markerSize = if (stopViewMode == SavedStopsViewMode.GRID_2) PeekTransitConstants.MAP_PREVIEW_MARKER_SIZE_DP_IN_2X2_GRID else PeekTransitConstants.MAP_PREVIEW_MARKER_SIZE_DP_IN_3X3_GRID,
                    zoomLevel = if (stopViewMode == SavedStopsViewMode.GRID_2) PeekTransitConstants.MAP_PREVIEW_ZOOM_LEVEL_IN_2X2_GRID else PeekTransitConstants.MAP_PREVIEW_ZOOM_LEVEL_IN_3X3_GRID,

                    bottomBannerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    bottomBannerPercentage = if (stopViewMode == SavedStopsViewMode.GRID_2) PeekTransitConstants.MAP_PREVIEW_BOTTOM_BANNER_PERCENTAGE_IN_2X2_GRID else PeekTransitConstants.MAP_PREVIEW_BOTTOM_BANNER_PERCENTAGE_IN_3X3_GRID,
                    showBottomBanner = if (stopViewMode == SavedStopsViewMode.GRID_2) PeekTransitConstants.MAP_PREVIEW_SHOW_BOTTOM_BANNER_IN_2X2_GRID else PeekTransitConstants.MAP_PREVIEW_SHOW_BOTTOM_BANNER_IN_3X3_GRID,
                    bottomBannerOpacity = if (stopViewMode == SavedStopsViewMode.GRID_2) PeekTransitConstants.MAP_PREVIEW_BOTTOM_BANNER_OPACITY_IN_2X2_GRID else PeekTransitConstants.MAP_PREVIEW_BOTTOM_BANNER_OPACITY_IN_3X3_GRID,

                    stopViewMode = stopViewMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            if (stopViewMode == SavedStopsViewMode.GRID_2) {
                                val totalHeight = PeekTransitConstants.MAP_PREVIEW_HEIGHT_SIZE_DP_IN_2X2_GRID
                                if (PeekTransitConstants.MAP_PREVIEW_SHOW_BOTTOM_BANNER_IN_2X2_GRID) {
                                    (totalHeight - (totalHeight * PeekTransitConstants.MAP_PREVIEW_BOTTOM_BANNER_PERCENTAGE_IN_2X2_GRID)).dp
                                } else totalHeight.dp
                            } else {
                                val totalHeight = PeekTransitConstants.MAP_PREVIEW_HEIGHT_SIZE_DP_IN_3X3_GRID
                                if (PeekTransitConstants.MAP_PREVIEW_SHOW_BOTTOM_BANNER_IN_3X3_GRID) {
                                    (totalHeight - (totalHeight * PeekTransitConstants.MAP_PREVIEW_BOTTOM_BANNER_PERCENTAGE_IN_3X3_GRID)).dp
                                } else totalHeight.dp
                            }
                        )

                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stop.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Left
                    )


                    Spacer(modifier = Modifier.height(4.dp))

                    if (stop.variants.isNotEmpty()) {
                        val currentDate = java.util.Date()
                        val currentVariants = stop.variants.filter { variant ->
                            val effectiveFrom = variant.getEffectiveFromDate()
                            val effectiveTo = variant.getEffectiveToDate()
                            (effectiveFrom == null || currentDate >= effectiveFrom) &&
                                    (effectiveTo == null || currentDate <= effectiveTo)
                        }.distinctBy { it.key.split(PeekTransitConstants.VARIANT_KEY_SEPARATOR)[0] }

                        if (currentVariants.isNotEmpty()) {
                            Text(
                                text = "#${stop.number} - " + currentVariants.joinToString(", ") { it.getRouteKey() },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Left,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "#${stop.number}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Left
                            )
                        }
                    } else {
                        Text(
                            text = "#${stop.number}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Left
                        )
                    }
                }

                if (isStopSaved.value) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Saved",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}




