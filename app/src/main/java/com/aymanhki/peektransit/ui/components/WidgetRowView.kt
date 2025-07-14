package com.aymanhki.peektransit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aymanhki.peektransit.data.models.WidgetModel
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.WidgetPreviewHelper

@Composable
fun WidgetRowView(
    widget: WidgetModel,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSelectionToggle: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val widgetConfig = WidgetModel.parseWidgetData(widget.widgetData)
    val widgetSize = widgetConfig.size
    val widgetName = widgetConfig.name
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelectionMode && onSelectionToggle != null) {
                    Modifier.clickable { onSelectionToggle() }
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(PeekTransitConstants.getWidgetPreviewWidthForSize(widgetSize, context).dp)
                    .height(PeekTransitConstants.getWidgetPreviewHeightForSize(widgetSize, context).dp)
                    .clip(RoundedCornerShape(12.dp))
                    .align(Alignment.CenterHorizontally)
            ) {
                val previewResult = WidgetPreviewHelper.generatePreviewSchedule(
                    widgetData = widget.widgetData,
                    noConfig = false,
                    timeFormat = widgetConfig.timeFormat,
                    showLastUpdatedStatus = widgetConfig.showLastUpdatedStatus,
                    multipleEntriesPerVariant = widgetConfig.multipleEntriesPerVariant,
                    showLateTextStatus = true
                )
                
                val finalWidgetData = previewResult?.widgetData ?: widget.widgetData
                val finalScheduleData = previewResult?.scheduleData ?: emptyList()
                
                WidgetPreviewView(
                    widgetData = finalWidgetData,
                    scheduleData = finalScheduleData,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = widgetName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                }
                            )
                            .then(
                                if (!isSelected) {
                                    Modifier.background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        CircleShape
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularIconButton(
                            icon = Icons.Default.Delete,
                            contentDescription = "Delete widget",
                            onClick = onDeleteClick,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                        
                        CircularIconButton(
                            icon = Icons.Default.Edit,
                            contentDescription = "Edit widget",
                            onClick = onEditClick,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CircularIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(containerColor)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
    }
}