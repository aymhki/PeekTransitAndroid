package com.aymanhki.peektransit.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aymanhki.peektransit.data.models.SavedStopsViewMode
import com.aymanhki.peektransit.data.models.Stop
import com.aymanhki.peektransit.managers.SavedStopsManager
import com.aymanhki.peektransit.utils.PeekTransitConstants
import kotlin.math.roundToInt


@Composable
fun StopRow(
    stop: Stop,
    distance: Double? = null,
    onNavigateToLiveStop: (Int) -> Unit = {},
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onNavigateToLiveStop(stop.number) },

        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 16.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MapPreview(
                latitude = stop.centre.geographic.latitude,
                longitude = stop.centre.geographic.longitude,
                direction = stop.direction,
                sizeWidth = PeekTransitConstants.MAP_PREVIEW_WIDTH_SIZE_DP_IN_LIST,
                sizeHeight = PeekTransitConstants.MAP_PREVIEW_HEIGHT_SIZE_DP_IN_LIST,
                renderWidth = PeekTransitConstants.MAP_PREVIEW_RENDER_WIDTH_SIZE_DP_IN_LIST,
                renderHeight = PeekTransitConstants.MAP_PREVIEW_RENDER_HEIGHT_SIZE_DP_IN_LIST,
                markerSize = PeekTransitConstants.MAP_PREVIEW_MARKER_SIZE_DP_IN_LIST,
                zoomLevel = PeekTransitConstants.MAP_PREVIEW_ZOOM_LEVEL_IN_LIST,
                stopViewMode = SavedStopsViewMode.LIST,
                bottomBannerPercentage = PeekTransitConstants.MAP_PREVIEW_BOTTOM_BANNER_PERCENTAGE_IN_LIST,
                bottomBannerColor = MaterialTheme.colorScheme.surface,
                bottomBannerOpacity = PeekTransitConstants.MAP_PREVIEW_BOTTOM_BANNER_OPACITY_IN_LIST,
                showBottomBanner = PeekTransitConstants.MAP_PREVIEW_SHOW_BOTTOM_BANNER_IN_LIST,
                modifier = Modifier
                    .width(PeekTransitConstants.MAP_PREVIEW_WIDTH_SIZE_DP_IN_LIST.dp)
                   .height(
                       if (PeekTransitConstants.MAP_PREVIEW_SHOW_BOTTOM_BANNER_IN_LIST) {
                       (PeekTransitConstants.MAP_PREVIEW_HEIGHT_SIZE_DP_IN_LIST - (PeekTransitConstants.MAP_PREVIEW_HEIGHT_SIZE_DP_IN_LIST * PeekTransitConstants.MAP_PREVIEW_BOTTOM_BANNER_PERCENTAGE_IN_LIST)).dp
                   } else PeekTransitConstants.MAP_PREVIEW_HEIGHT_SIZE_DP_IN_LIST.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "${stop.name}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#${stop.number}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (distance != null && distance.isFinite()) {
                        Text(
                            text = " ● ${formatDistance(distance)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (stop.variants.isNotEmpty()) {

                    val currentDate = java.util.Date()
                    val currentVariants = stop.variants.filter { variant ->
                        val effectiveFrom = variant.getEffectiveFromDate()
                        val effectiveTo = variant.getEffectiveToDate()
                        (effectiveFrom == null || currentDate >= effectiveFrom) &&
                                (effectiveTo == null || currentDate <= effectiveTo)
                    }.distinctBy { it.key.split(PeekTransitConstants.VARIANT_KEY_SEPARATOR)[0] }
                    
                    val futureVariants = stop.variants.filter { variant ->
                        val effectiveFrom = variant.getEffectiveFromDate()
                        (effectiveFrom != null && effectiveFrom > currentDate)
                    }.distinctBy { it.key.split(PeekTransitConstants.VARIANT_KEY_SEPARATOR)[0] }

                    var theyAreBothTheSame = true

                    if (futureVariants.size == currentVariants.size) {
                        for (i in currentVariants.indices) {
                            if (currentVariants[i].getRouteKey() != futureVariants[i].getRouteKey()) {
                                theyAreBothTheSame = false
                                break
                            }
                        }
                    }
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (currentVariants.isNotEmpty()) {
                            val chunkedCurrentVariants = currentVariants.chunked(4)
                            chunkedCurrentVariants.forEach { rowVariants ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowVariants.forEach { variant ->
                                        VariantBadge(variant = variant)
                                    }
                                }
                            }
                        }
                        
                        if (futureVariants.isNotEmpty() && !theyAreBothTheSame) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val groupedFutureVariants = futureVariants.groupBy { variant ->
                                    val effectiveFrom = variant.getEffectiveFromDate()
                                    val calendar = java.util.Calendar.getInstance()
                                    calendar.time = effectiveFrom ?: java.util.Date()
                                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    calendar.set(java.util.Calendar.MINUTE, 0)
                                    calendar.set(java.util.Calendar.SECOND, 0)
                                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                                    calendar.time
                                }.toSortedMap()

                                groupedFutureVariants.forEach { (effectiveDate, variants) ->
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val dateFormat = java.text.SimpleDateFormat("MMM d, hh:mm a", java.util.Locale.getDefault())
                                        Text(
                                            text = "Effective From ${dateFormat.format(effectiveDate)}:",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        val chunkedVariants = variants.chunked(4)
                                        chunkedVariants.forEachIndexed { index, rowVariants ->
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = if (index == chunkedVariants.size - 1) {
                                                    Modifier //.padding(bottom = 8.dp)
                                                } else {
                                                    Modifier
                                                }
                                            ) {
                                                rowVariants.forEach { variant ->
                                                    VariantBadge(
                                                        variant = variant,
                                                        modifier = Modifier
                                                    )
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
            
            val context = LocalContext.current
            val savedStopsManager = remember { SavedStopsManager.getInstance(context) }
            val isStopSaved = savedStopsManager.isStopSavedFlow(stop.number.toString()).collectAsState(false)
            
            if (isStopSaved.value) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "Saved stop",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
        }
    }
}


private fun formatDistance(distanceInMeters: Double): String {
    return when {
        distanceInMeters < 1000 -> "${distanceInMeters.roundToInt()} meters away"
        else -> "${(distanceInMeters / 1000).let { "%.1f".format(it) }}km away"
    }
}
