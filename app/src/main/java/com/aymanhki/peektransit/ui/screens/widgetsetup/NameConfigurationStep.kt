package com.aymanhki.peektransit.ui.screens.widgetsetup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aymanhki.peektransit.data.models.Stop
import com.aymanhki.peektransit.data.models.Variant
import com.aymanhki.peektransit.utils.PeekTransitConstants

@Composable
fun NameConfigurationStep(
    widgetName: String,
    onWidgetNameChange: (String) -> Unit,
    isEditing: Boolean,
    selectedStops: List<Stop>,
    selectedVariants: Map<String, List<Variant>>,
    isClosestStop: Boolean,
    preferredStops: List<Stop>,
    widgetSize: String,
    timeFormat: String,
    multipleEntriesPerVariant: Boolean,
    showLastUpdatedStatus: Boolean,
    noSelectedVariants: Boolean,
    selectedPerferredStopsInClosestStops: Boolean
) {
    val defaultName = remember(selectedStops, selectedVariants, isClosestStop, preferredStops, widgetSize, timeFormat, multipleEntriesPerVariant, showLastUpdatedStatus, noSelectedVariants, selectedPerferredStopsInClosestStops) {
        generateDefaultWidgetName(
            selectedStops = selectedStops,
            selectedVariants = selectedVariants,
            isClosestStop = isClosestStop,
            preferredStops = preferredStops,
            widgetSize = widgetSize,
            timeFormat = timeFormat,
            multipleEntriesPerVariant = multipleEntriesPerVariant,
            showLastUpdatedStatus = showLastUpdatedStatus,
            noSelectedVariants = noSelectedVariants,
            selectedPerferredStopsInClosestStops = selectedPerferredStopsInClosestStops
        )
    }
    
    LaunchedEffect(defaultName) {
        if (!isEditing && widgetName.isEmpty()) {
            onWidgetNameChange(defaultName)
        }
    }
    
    val isDifferentFromDefault = widgetName != defaultName
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Name Your Widget",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Give your widget a name or use the default name generated for you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = widgetName,
            onValueChange = onWidgetNameChange,
            label = { Text("Widget Name") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isDifferentFromDefault) {
            Button(
                onClick = {
                    onWidgetNameChange(defaultName)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Set the widget name back to be the default name",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Text(
            text = "Default name: $defaultName",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}

private fun generateDefaultWidgetName(
    selectedStops: List<Stop>,
    selectedVariants: Map<String, List<Variant>>,
    isClosestStop: Boolean,
    preferredStops: List<Stop>,
    widgetSize: String,
    timeFormat: String,
    multipleEntriesPerVariant: Boolean,
    showLastUpdatedStatus: Boolean,
    noSelectedVariants: Boolean,
    selectedPerferredStopsInClosestStops: Boolean
): String {
    // Format time format display
    val timeFormatDisplay = if (multipleEntriesPerVariant) {
        "Mixed Time Format"
    } else {
        when (timeFormat) {
            "minutes" -> "Minutes"
            "clock" -> "Clock"
            "mixed" -> "Mixed Time Format"
            else -> "Default"
        }
    }
    
    // Format entries per variant display
    val entriesPerVariantDisplay = if (multipleEntriesPerVariant) {
        "Multiple entries per variant"
    } else {
        "Single entry per variant"
    }
    
    // Format last updated status display
    val lastUpdatedStatusDisplay = if (showLastUpdatedStatus) {
        "Show Last Updated Status"
    } else {
        "Don't Show Last Updated Status"
    }
    
    // Generate the main part of the name
    val mainName = when {
        isClosestStop -> {
            val preferredStopsText = if (selectedPerferredStopsInClosestStops) {
                " (With preferred stops)"
            } else {
                ""
            }
            "Closest Stops$preferredStopsText"
        }
        else -> {
            val stopNumbers = selectedStops.mapNotNull { stop ->
                if (stop.number != -1) "#${stop.number}" else null
            }.joinToString(", ")
            
            val variantKeys = if (!noSelectedVariants) {
                selectedVariants.values.flatten().map { it.getRouteKey() }.joinToString(", ")
            } else {
                "Up Coming Buses"
            }
            
            "$stopNumbers - $variantKeys"
        }
    }
    
    return "$mainName - $widgetSize - $timeFormatDisplay - $entriesPerVariantDisplay - $lastUpdatedStatusDisplay"
}