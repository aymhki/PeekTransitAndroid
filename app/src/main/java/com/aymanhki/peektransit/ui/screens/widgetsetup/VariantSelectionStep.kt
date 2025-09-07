package com.aymanhki.peektransit.ui.screens.widgetsetup

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aymanhki.peektransit.data.models.Stop
import com.aymanhki.peektransit.data.models.Variant
import com.aymanhki.peektransit.data.network.WinnipegTransitAPI
import com.aymanhki.peektransit.ui.components.CircularCheckbox
import com.aymanhki.peektransit.ui.components.VariantBadge
import com.aymanhki.peektransit.utils.PeekTransitConstants
import kotlinx.coroutines.launch

@Composable
fun VariantSelectionStep(
    widgetSize: String,
    multipleEntriesPerVariant: Boolean,
    selectedStops: List<Stop>,
    noSelectedVariants: Boolean,
    onNoSelectedVariantsChange: (Boolean) -> Unit,
    selectedVariants: Map<String, List<Variant>>,
    onSelectedVariantsChange: (Map<String, List<Variant>>) -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var stopVariants by remember { mutableStateOf<Map<String, Set<Variant>>>(emptyMap()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    val maxVariants = if (multipleEntriesPerVariant) {
        PeekTransitConstants.getMaxVariantsAllowedForMultipleEntries(widgetSize)
    } else {
        PeekTransitConstants.getMaxVariantsAllowed(widgetSize)
    }
    
    LaunchedEffect(selectedStops) {
        if (selectedStops.isNotEmpty()) {
            isLoading = true
            errorMessage = null
            
            coroutineScope.launch {
                try {
                    val api = WinnipegTransitAPI.getInstance()
                    val variants = mutableMapOf<String, Set<Variant>>()
                    
                    for (stop in selectedStops) {
                        try {
                            val stopKey = stop.number.toString()
                            val stopVariantsList = api.getOnlyVariantsForStop(stop)
                            val stopVariantsSet = convertVariantArrayToUniqueSet(stopVariantsList)
                            variants[stopKey] = stopVariantsSet
                        } catch (e: Exception) {

                        }
                    }
                    
                    stopVariants = variants
                } catch (e: Exception) {
                    errorMessage = "Failed to load bus routes"
                } finally {
                    isLoading = false
                }
            }
        }
    }
    
    fun toggleVariantSelection(stopNumber: Int, variant: Variant) {
        val stopId = stopNumber.toString()
        val currentVariants = selectedVariants[stopId]?.toMutableList() ?: mutableListOf()
        
        val existingIndex = currentVariants.indexOfFirst { 
            it.key == variant.key && it.name == variant.name 
        }
        
        if (existingIndex >= 0) {
            currentVariants.removeAt(existingIndex)
        } else if (currentVariants.size < maxVariants) {
            currentVariants.add(variant)
        }
        
        val newSelectedVariants = selectedVariants.toMutableMap()
        if (currentVariants.isEmpty()) {
            newSelectedVariants.remove(stopId)
        } else {
            newSelectedVariants[stopId] = currentVariants
        }
        
        onSelectedVariantsChange(newSelectedVariants)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Select the widget bus variants",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { 
                    onNoSelectedVariantsChange(!noSelectedVariants)
                    if (!noSelectedVariants) {
                        onSelectedVariantsChange(emptyMap())
                    }
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (noSelectedVariants) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = null,
                    tint = if (noSelectedVariants) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Automatically show the upcoming buses everytime",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "No need to select specific variant(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        AnimatedVisibility(
            visible = !noSelectedVariants,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Loading bus stops schedules...")
                            }
                        }
                    }
                    
                    errorMessage != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Error loading schedules",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    text = errorMessage!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        errorMessage = null
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Retry")
                                }
                            }
                        }
                    }
                    
                    else -> {
                        selectedStops.forEach { stop ->
                            val stopKey = stop.number.toString()
                            val variants = stopVariants[stopKey] ?: emptySet()
                            
                            if (variants.isNotEmpty()) {
                                StopScheduleSection(
                                    stop = stop,
                                    variants = variants.toList(),
                                    selectedVariants = selectedVariants[stopKey] ?: emptyList(),
                                    maxVariants = maxVariants,
                                    onVariantSelect = { variant ->
                                        toggleVariantSelection(stop.number, variant)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StopScheduleSection(
    stop: Stop,
    variants: List<Variant>,
    selectedVariants: List<Variant>,
    maxVariants: Int,
    onVariantSelect: (Variant) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "${stop.number} - ${stop.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Select up to $maxVariants bus routes for this stop:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                variants.forEach { variant ->
                    val isSelected = selectedVariants.any {
                        it.key == variant.key && it.name == variant.name
                    }
                    val canSelect = !isSelected && selectedVariants.size < maxVariants
                    val isEnabled = canSelect || isSelected

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = isEnabled) {
                                onVariantSelect(variant)
                            },
                        color = if (isEnabled) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .then(
                                    if (!isEnabled) {
                                        Modifier.alpha(0.5f)
                                    } else {
                                        Modifier
                                    }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularCheckbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    if (isEnabled) {
                                        onVariantSelect(variant)
                                    }
                                },
                                enabled = isEnabled
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = variant.key.split(PeekTransitConstants.VARIANT_KEY_SEPARATOR).first().replace("BLUE", "B"),
                                modifier = Modifier.weight(0.2f),
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isEnabled) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                }
                            )

                            Text (
                                text = variant.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(0.8f),
                                maxLines = 1,
                                color = if (isEnabled) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = "Selected: ${selectedVariants.size}/$maxVariants",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun convertVariantArrayToUniqueSet(variants: List<Variant>): Set<Variant> {
    val uniqueVariants = mutableSetOf<Variant>()
    
    for (variantRouteObjects in variants) {
        var key = variantRouteObjects.key
        
//        if (key.contains("-")) {
//            key = key.split(PeekTransitConstants.VARIANT_KEY_SEPARATOR).first()
//        }
//
//        if (key.contains("BLUE")) {
//            key = "B"
//        }
        
        uniqueVariants.add(Variant(
            key = key,
            name = variantRouteObjects.name
        ))
    }
    
    return uniqueVariants
}