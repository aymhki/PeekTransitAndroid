package com.aymanhki.peektransit.ui.screens.widgetsetup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aymanhki.peektransit.ui.components.CircularCheckbox
import com.aymanhki.peektransit.ui.components.WidgetPreviewView
import com.aymanhki.peektransit.ui.theme.PeekTransitTheme
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.WidgetPreviewHelper

@Composable
fun SizeSelectionStep(
    widgetSize: String,
    onSizeChange: (String) -> Unit,
    showLastUpdatedStatus: Boolean,
    onShowLastUpdatedStatusChange: (Boolean) -> Unit,
    timeFormat: String,
    onTimeFormatChange: (String) -> Unit,
    multipleEntriesPerVariant: Boolean,
    onMultipleEntriesPerVariantChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = "Select the widget configuration options",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        WidgetSizeSelector(
            selectedSize = widgetSize,
            onSizeChange = onSizeChange
        )
        
        // Widget Preview
        WidgetPreviewCard(
            widgetSize = widgetSize,
            showLastUpdatedStatus = showLastUpdatedStatus,
            timeFormat = timeFormat,
            multipleEntriesPerVariant = multipleEntriesPerVariant
        )
        
        
        // Bus Variants Per Stop Section
        Text(
            text = "Bus Variants Per Stop",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Multiple arrival times option
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMultipleEntriesPerVariantChange(true) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularCheckbox(
                        checked = multipleEntriesPerVariant == true,
                        onCheckedChange = { onMultipleEntriesPerVariantChange(true) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Multiple arrival times for a single bus variant in each bus stop",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Single arrival time option
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMultipleEntriesPerVariantChange(false) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularCheckbox(
                        checked = multipleEntriesPerVariant == false,
                        onCheckedChange = { onMultipleEntriesPerVariantChange(false) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Single arrival time for multiple bus variants in each bus stop",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Time Format Section  
        Text(
            text = "Time Format",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Mixed format option (only available for multiple entries)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = multipleEntriesPerVariant) {
                        if (multipleEntriesPerVariant) {
                            onTimeFormatChange("mixed")
                        }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (multipleEntriesPerVariant) 
                        MaterialTheme.colorScheme.surfaceContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularCheckbox(
                        checked = multipleEntriesPerVariant && timeFormat == "mixed",
                        onCheckedChange = { 
                            if (multipleEntriesPerVariant) {
                                onTimeFormatChange("mixed")
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mixed format, one entry in minutes and one in clock format (Available only for the 'Multiple Arrivals' option)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (multipleEntriesPerVariant) 
                            MaterialTheme.colorScheme.onSurface 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Clock time format
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !multipleEntriesPerVariant) {
                        if (!multipleEntriesPerVariant) {
                            onTimeFormatChange("clock")
                        }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (!multipleEntriesPerVariant) 
                        MaterialTheme.colorScheme.surfaceContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularCheckbox(
                        checked = !multipleEntriesPerVariant && timeFormat == "clock",
                        onCheckedChange = { 
                            if (!multipleEntriesPerVariant) {
                                onTimeFormatChange("clock")
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Always clock Time (HH:MM AM/PM) without Late (L.) and Early (E.) prefix",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (!multipleEntriesPerVariant) 
                            MaterialTheme.colorScheme.onSurface 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Minutes remaining format
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !multipleEntriesPerVariant) {
                        if (!multipleEntriesPerVariant) {
                            onTimeFormatChange("minutes")
                        }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (!multipleEntriesPerVariant) 
                        MaterialTheme.colorScheme.surfaceContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularCheckbox(
                        checked = !multipleEntriesPerVariant && timeFormat == "minutes",
                        onCheckedChange = { 
                            if (!multipleEntriesPerVariant) {
                                onTimeFormatChange("minutes")
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "X(X) Minutes remaining when the bus is within 15 minutes with Late (L.) and Early (E.) prefix",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (!multipleEntriesPerVariant) 
                            MaterialTheme.colorScheme.onSurface 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Last Updated Status Section
        Text(
            text = "Last Updated Status",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Show last updated option
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShowLastUpdatedStatusChange(true) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularCheckbox(
                        checked = showLastUpdatedStatus == true,
                        onCheckedChange = { onShowLastUpdatedStatusChange(true) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Show Last Updated Status",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Don't show last updated option
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShowLastUpdatedStatusChange(false) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularCheckbox(
                        checked = showLastUpdatedStatus == false,
                        onCheckedChange = { onShowLastUpdatedStatusChange(false) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Don't show Last Updated Status",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetSizeSelector(
    selectedSize: String,
    onSizeChange: (String) -> Unit
) {
    val sizes = listOf(
        "small" to "Small",
        "medium" to "Medium",
        "large" to "Large"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sizes.forEach { (value, label) ->
            SizeOption(
                label = label,
                selected = selectedSize == value,
                onClick = { onSizeChange(value) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun SizeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}


@Composable
private fun WidgetPreviewCard(
    widgetSize: String,
    showLastUpdatedStatus: Boolean,
    timeFormat: String,
    multipleEntriesPerVariant: Boolean
) {
    // Create a temporary widget configuration for preview (like iOS noConfig: true)
    val tempWidgetData = mapOf(
        "size" to widgetSize,
        "name" to "Preview Widget",
        "isClosestStop" to true,
        "noSelectedVariants" to false,
        "multipleEntriesPerVariant" to multipleEntriesPerVariant,
        "showLastUpdatedStatus" to showLastUpdatedStatus,
        "timeFormat" to timeFormat,
        "selectedStops" to emptyList<Any>(),
        "preferredStops" to emptyList<Any>(),
        "selectedVariants" to emptyMap<String, List<Any>>(),
        "stops" to emptyList<Any>()
    )
    
    // Generate preview data using WidgetPreviewHelper (matching iOS behavior)
    val previewResult = WidgetPreviewHelper.generatePreviewSchedule(
        widgetData = tempWidgetData,
        noConfig = true, // This matches iOS SizeSelectionStep behavior
        timeFormat = timeFormat,
        showLastUpdatedStatus = showLastUpdatedStatus,
        multipleEntriesPerVariant = multipleEntriesPerVariant,
        showLateTextStatus = widgetSize == "small" || widgetSize == "lockscreen"
    )
    
    val finalWidgetData = previewResult?.widgetData ?: tempWidgetData
    val finalScheduleData = previewResult?.scheduleData ?: emptyList()
    
    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        WidgetPreviewView(
            widgetData = finalWidgetData,
            scheduleData = finalScheduleData,
            modifier = Modifier
        )
    }
}