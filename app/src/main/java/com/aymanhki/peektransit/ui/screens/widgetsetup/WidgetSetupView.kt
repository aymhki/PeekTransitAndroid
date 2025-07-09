package com.aymanhki.peektransit.ui.screens.widgetsetup

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aymanhki.peektransit.data.models.*
import com.aymanhki.peektransit.data.repository.StopsDataStore
import com.aymanhki.peektransit.managers.SavedWidgetsManager
import com.aymanhki.peektransit.ui.components.CustomTopAppBar
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSetupView(
    editingWidget: WidgetModel? = null,
    onDismiss: () -> Unit,
    stopsDataStore: StopsDataStore
) {
    val context = LocalContext.current
    val savedWidgetsManager = remember { SavedWidgetsManager.getInstance(context) }
    
    // Parse existing widget data if editing
    val existingConfig = remember(editingWidget) {
        editingWidget?.let { WidgetModel.parseWidgetData(it.widgetData) }
    }
    
    // Widget configuration state
    var currentStep by remember { mutableStateOf(0) }
    var widgetSize by remember { mutableStateOf(existingConfig?.size ?: "medium") }
    var showLastUpdatedStatus by remember { mutableStateOf(existingConfig?.showLastUpdatedStatus ?: true) }
    var timeFormat by remember { mutableStateOf(existingConfig?.timeFormat ?: "mixed") }
    var multipleEntriesPerVariant by remember { mutableStateOf(existingConfig?.multipleEntriesPerVariant ?: true) }
    var isClosestStop by remember { mutableStateOf(existingConfig?.isClosestStop ?: false) }
    var selectedStops by remember { mutableStateOf(existingConfig?.stops ?: emptyList<Stop>()) }
    var preferredStops by remember { mutableStateOf(existingConfig?.preferredStops ?: emptyList<Stop>()) }
    var selectedPerferredStopsInClosestStops by remember { mutableStateOf(preferredStops.isNotEmpty()) }
    var noSelectedVariants by remember { mutableStateOf(existingConfig?.noSelectedVariants ?: false) }
    var selectedVariants by remember { mutableStateOf(existingConfig?.selectedVariants ?: emptyMap<String, List<Variant>>()) }
    var widgetName by remember { mutableStateOf(existingConfig?.name ?: "") }
    
    var showDuplicateNameDialog by remember { mutableStateOf(false) }
    var showNoServiceDialog by remember { mutableStateOf(false) }
    
    // Automatic time format selection based on multiple entries setting
    LaunchedEffect(multipleEntriesPerVariant) {
        timeFormat = if (multipleEntriesPerVariant) {
            "mixed"
        } else {
            "minutes"
        }
    }
    
    val totalSteps = if (isClosestStop && !selectedPerferredStopsInClosestStops) 3 else 4
    
    fun canProceedToNextStep(): Boolean {
        return when (currentStep) {
            0 -> true // Size selection always valid
            1 -> {
                when {
                    isClosestStop && !selectedPerferredStopsInClosestStops -> true // Closest stop without preferred stops
                    isClosestStop && selectedPerferredStopsInClosestStops -> preferredStops.isNotEmpty() // Closest stop with preferred stops requires stops
                    else -> selectedStops.isNotEmpty() // Manual selection requires at least one stop
                }
            }
            2 -> {
                // Variant selection - only show this step if there are stops to configure
                val stopsToCheck = if (isClosestStop && selectedPerferredStopsInClosestStops) preferredStops else selectedStops
                if (stopsToCheck.isEmpty()) return true
                if (noSelectedVariants) return true
                
                // Check if all selected stops have at least one variant selected
                stopsToCheck.all { stop ->
                    val stopKey = stop.number.toString()
                    selectedVariants[stopKey]?.isNotEmpty() == true
                }
            }
            3 -> widgetName.isNotBlank() // Name must not be empty
            else -> false
        }
    }
    
    fun handleSave() {
        // Check for duplicate name
        val isDuplicate = savedWidgetsManager.isNameUnique(
            widgetName,
            excludeId = editingWidget?.id
        ).not()
        
        if (isDuplicate) {
            showDuplicateNameDialog = true
            return
        }
        
        // Create widget configuration
        val configuration = WidgetConfiguration(
            size = widgetSize,
            name = widgetName,
            showLastUpdatedStatus = showLastUpdatedStatus,
            timeFormat = timeFormat,
            multipleEntriesPerVariant = multipleEntriesPerVariant,
            isClosestStop = isClosestStop,
            noSelectedVariants = noSelectedVariants,
            stops = if (!isClosestStop) selectedStops else emptyList(),
            preferredStops = if (isClosestStop) preferredStops else emptyList(),
            selectedVariants = if (!noSelectedVariants) selectedVariants else emptyMap()
        )
        
        val widgetData = configuration.toWidgetData()
        
        if (editingWidget != null) {
            // Update existing widget
            val updatedWidget = editingWidget.copy(widgetData = widgetData)
            savedWidgetsManager.updateWidget(editingWidget.id, updatedWidget)
        } else {
            // Create new widget
            val newWidget = WidgetModel(
                id = UUID.randomUUID().toString(),
                widgetData = widgetData
            )
            savedWidgetsManager.addWidget(newWidget)
        }
        
        onDismiss()
    }
    
    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (editingWidget != null) "Edit Widget" else "Create Widget",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (currentStep < totalSteps) {
                            Text(
                                text = "Step ${currentStep + 1} of $totalSteps",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (currentStep > 0) {
                        IconButton(onClick = { 
                            // Smart back navigation
                            when (currentStep) {
                                3 -> {
                                    // From name config, go back based on whether we have stops to configure variants for
                                    val hasStops = if (isClosestStop && selectedPerferredStopsInClosestStops) {
                                        preferredStops.isNotEmpty()
                                    } else if (!isClosestStop) {
                                        selectedStops.isNotEmpty()
                                    } else {
                                        false // Closest stop without preferred stops skips variant selection
                                    }
                                    currentStep = if (hasStops) 2 else 1
                                }
                                else -> currentStep--
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    } else {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { 
                                // Smart back navigation
                                when (currentStep) {
                                    3 -> {
                                        // From name config, go back based on whether we have stops to configure variants for
                                        val hasStops = if (isClosestStop && selectedPerferredStopsInClosestStops) {
                                            preferredStops.isNotEmpty()
                                        } else if (!isClosestStop) {
                                            selectedStops.isNotEmpty()
                                        } else {
                                            false // Closest stop without preferred stops skips variant selection
                                        }
                                        currentStep = if (hasStops) 2 else 1
                                    }
                                    else -> currentStep--
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back")
                        }
                    }
                    
                    Button(
                        onClick = {
                            if (currentStep < totalSteps - 1) {
                                currentStep++
                            } else {
                                handleSave()
                            }
                        },
                        enabled = canProceedToNextStep(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (currentStep < totalSteps - 1) "Continue" else "Save",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { step ->
            when (step) {
                0 -> SizeSelectionStep(
                    widgetSize = widgetSize,
                    onSizeChange = { widgetSize = it },
                    showLastUpdatedStatus = showLastUpdatedStatus,
                    onShowLastUpdatedStatusChange = { showLastUpdatedStatus = it },
                    timeFormat = timeFormat,
                    onTimeFormatChange = { timeFormat = it },
                    multipleEntriesPerVariant = multipleEntriesPerVariant,
                    onMultipleEntriesPerVariantChange = { 
                        multipleEntriesPerVariant = it
                        if (it && timeFormat == "default") {
                            timeFormat = "mixed"
                        }
                    }
                )
                
                1 -> StopSelectionStep(
                    widgetSize = widgetSize,
                    multipleEntriesPerVariant = multipleEntriesPerVariant,
                    isClosestStop = isClosestStop,
                    onIsClosestStopChange = { isClosestStop = it },
                    selectedStops = selectedStops,
                    onSelectedStopsChange = { selectedStops = it },
                    preferredStops = preferredStops,
                    onPreferredStopsChange = { preferredStops = it },
                    selectedPerferredStopsInClosestStops = selectedPerferredStopsInClosestStops,
                    onSelectedPerferredStopsInClosestStopsChange = { selectedPerferredStopsInClosestStops = it },
                    stopsDataStore = stopsDataStore
                )
                
                2 -> {
                    val stopsToShow = if (isClosestStop && selectedPerferredStopsInClosestStops) preferredStops else selectedStops
                    if (stopsToShow.isNotEmpty()) {
                        VariantSelectionStep(
                            widgetSize = widgetSize,
                            multipleEntriesPerVariant = multipleEntriesPerVariant,
                            selectedStops = stopsToShow,
                            noSelectedVariants = noSelectedVariants,
                            onNoSelectedVariantsChange = { noSelectedVariants = it },
                            selectedVariants = selectedVariants,
                            onSelectedVariantsChange = { selectedVariants = it }
                        )
                    } else {
                        // Skip to name configuration for closest stop with no preferred stops
                        LaunchedEffect(Unit) {
                            currentStep = 3
                        }
                    }
                }
                
                3 -> NameConfigurationStep(
                    widgetName = widgetName,
                    onWidgetNameChange = { widgetName = it },
                    isEditing = editingWidget != null,
                    selectedStops = if (isClosestStop && selectedPerferredStopsInClosestStops) preferredStops else selectedStops,
                    selectedVariants = selectedVariants,
                    isClosestStop = isClosestStop,
                    preferredStops = preferredStops
                )
            }
        }
    }
    
    // Duplicate name dialog
    if (showDuplicateNameDialog) {
        AlertDialog(
            onDismissRequest = { showDuplicateNameDialog = false },
            title = { Text("Duplicate Name") },
            text = { Text("A widget with this name already exists. Please choose a different name.") },
            confirmButton = {
                TextButton(onClick = { showDuplicateNameDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
    
    // No service dialog
    if (showNoServiceDialog) {
        AlertDialog(
            onDismissRequest = { showNoServiceDialog = false },
            title = { Text("No Service") },
            text = { Text("One or more selected stops have no active bus service.") },
            confirmButton = {
                TextButton(onClick = { showNoServiceDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}