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
    preferredStops: List<Stop>
) {
    val defaultName = remember(selectedStops, selectedVariants, isClosestStop, preferredStops) {
        generateDefaultWidgetName(
            selectedStops = selectedStops,
            selectedVariants = selectedVariants,
            isClosestStop = isClosestStop,
            preferredStops = preferredStops
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
                    .padding(horizontal = 32.dp)
            ) {
                Text("Set the widget name to be the default name")
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
    preferredStops: List<Stop>
): String {
    return when {
        isClosestStop && preferredStops.isEmpty() -> {
            "Closest Stop"
        }
        isClosestStop && preferredStops.isNotEmpty() -> {
            val stopNames = preferredStops.take(2).map { it.number }
            "Closest (${stopNames.joinToString(", ")})"
        }
        selectedStops.size == 1 -> {
            val stop = selectedStops.first()
            val variants = selectedVariants[stop.number.toString()] ?: emptyList()
            
            if (variants.isEmpty()) {
                "${stop.number} - ${stop.name.take(20)}"
            } else {
                val routeNumbers = variants.map { it.getRouteKey() }.take(2)
                "${stop.number} (${routeNumbers.joinToString(", ")})"
            }
        }
        else -> {
            val stopNumbers = selectedStops.take(3).map { it.number }
            "Stops: ${stopNumbers.joinToString(", ")}"
        }
    }
}