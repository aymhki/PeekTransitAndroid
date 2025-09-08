package com.aymanhki.peektransit.ui.components


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aymanhki.peektransit.data.models.FolderCategory
import com.aymanhki.peektransit.data.models.SavedStopsViewMode
import com.aymanhki.peektransit.utils.PeekTransitConstants

@Composable
fun FolderGridItem(
    folder: FolderCategory,
    stopViewMode: SavedStopsViewMode,
    onClick: () -> Unit
) {
    val cardHeight = if (stopViewMode == SavedStopsViewMode.GRID_2) PeekTransitConstants.TOTAL_GRID_CARD_HEIGHT_DP_IN_2X2_GRID_DP.dp else PeekTransitConstants.TOTAL_GRID_CARD_HEIGHT_DP_IN_3X3_GRID_DP.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 0.dp, start = 12.dp, end = 12.dp, top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val iconBoxHeight = if (stopViewMode == SavedStopsViewMode.GRID_2) PeekTransitConstants.MAP_PREVIEW_HEIGHT_SIZE_DP_IN_2X2_GRID.dp else PeekTransitConstants.MAP_PREVIEW_HEIGHT_SIZE_DP_IN_3X3_GRID.dp
            val bannerHeightPercentage = if (stopViewMode == SavedStopsViewMode.GRID_2) PeekTransitConstants.MAP_PREVIEW_BOTTOM_BANNER_PERCENTAGE_IN_2X2_GRID  else PeekTransitConstants.MAP_PREVIEW_BOTTOM_BANNER_PERCENTAGE_IN_3X3_GRID
            val isBannerShowing = if (stopViewMode == SavedStopsViewMode.GRID_2) PeekTransitConstants.MAP_PREVIEW_SHOW_BOTTOM_BANNER_IN_2X2_GRID else PeekTransitConstants.MAP_PREVIEW_SHOW_BOTTOM_BANNER_IN_3X3_GRID
            val finalBoxHeight = if (isBannerShowing) {
                (iconBoxHeight - (iconBoxHeight * bannerHeightPercentage))
            }
            else {
                (iconBoxHeight)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(finalBoxHeight)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (folder.icons.isEmpty()) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(if (stopViewMode == SavedStopsViewMode.GRID_2) 0.3f else 0.5f),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else if (folder.icons.size == 1) {
                    Icon(
                        PeekTransitConstants.getIconByName(folder.icons[0]) ?: Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(if (stopViewMode == SavedStopsViewMode.GRID_2) 0.3f else 0.5f),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .fillMaxHeight(if (stopViewMode == SavedStopsViewMode.GRID_2) 0.3f else 0.5f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        folder.icons.take(2).forEach { iconName ->
                            Icon(
                                PeekTransitConstants.getIconByName(iconName) ?: Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Left,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${folder.stopOrder.size} stops",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Left,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


