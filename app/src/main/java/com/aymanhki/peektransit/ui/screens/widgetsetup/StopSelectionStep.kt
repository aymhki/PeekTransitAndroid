package com.aymanhki.peektransit.ui.screens.widgetsetup

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aymanhki.peektransit.data.models.Stop
import com.aymanhki.peektransit.data.repository.StopsDataStore
import com.aymanhki.peektransit.managers.SavedStopsManager
import com.aymanhki.peektransit.ui.components.CircularCheckbox
import com.aymanhki.peektransit.ui.components.ErrorView
import com.aymanhki.peektransit.ui.components.MapPreview
import com.aymanhki.peektransit.ui.components.StopRow
import com.aymanhki.peektransit.ui.components.VariantBadge
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.location.LocationManagerProvider
import com.aymanhki.peektransit.utils.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun StopSelectionStep(
    widgetSize: String,
    multipleEntriesPerVariant: Boolean,
    isClosestStop: Boolean,
    onIsClosestStopChange: (Boolean) -> Unit,
    selectedStops: List<Stop>,
    onSelectedStopsChange: (List<Stop>) -> Unit,
    preferredStops: List<Stop>,
    onPreferredStopsChange: (List<Stop>) -> Unit,
    selectedPerferredStopsInClosestStops: Boolean,
    onSelectedPerferredStopsInClosestStopsChange: (Boolean) -> Unit,
    stopsDataStore: StopsDataStore,
    mainViewModel: com.aymanhki.peektransit.viewmodel.MainViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationManager = remember { LocationManagerProvider.getInstance(context) }
    
    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    )
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var viewState by remember { mutableStateOf(ViewState.READY) }
    
    val savedStopsManager = remember { SavedStopsManager.getInstance(context) }
    
    val combinedStops by stopsDataStore.stops.observeAsState(emptyList())
    val searchResults by stopsDataStore.searchResults.observeAsState(emptyList())
    val isLoading by stopsDataStore.isLoading.observeAsState(false)
    val isSearching by stopsDataStore.isSearching.observeAsState(false)
    val error by stopsDataStore.error.observeAsState()
    val bookmarkedStops by savedStopsManager.savedStops.collectAsState()
    
    val isLoadingStops by mainViewModel.isLoadingStops.observeAsState(false)
    val isLoadingLocation by mainViewModel.isLoadingLocation.observeAsState(false)
    val locationError by mainViewModel.locationError.observeAsState()
    val isViewModelInitialized by mainViewModel.isInitialized.observeAsState(false)
    
    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            mainViewModel.initializeGlobal()
        }
    }
    
    val maxPerferredStopsInClosestStops = PeekTransitConstants.getMaxPerferredstopsInClosestStops()
    val maxStops = if (selectedPerferredStopsInClosestStops) {
        maxPerferredStopsInClosestStops
    } else {
        if (multipleEntriesPerVariant) {
            PeekTransitConstants.getMaxStopsAllowedForMultipleEntries(widgetSize)
        } else {
            PeekTransitConstants.getMaxStopsAllowed(widgetSize)
        }
    }
    
    val allStops = remember(combinedStops, searchResults) {
        val combined = combinedStops.toMutableList()
        val existingStopNumbers = combined.map { it.number }.toSet()
        
        searchResults.forEach { stop ->
            if (stop.number !in existingStopNumbers) {
                combined.add(stop)
            }
        }
        combined
    }
    
    val currentFilteredStops = remember(allStops, bookmarkedStops, selectedTab, searchQuery) {
        val stopsToFilter = when (selectedTab) {
            0 -> allStops
            else -> bookmarkedStops.map { it.stopData }
        }
        
        if (searchQuery.isEmpty()) {
            stopsToFilter
        } else {
            stopsToFilter.filter { stop ->
                stop.name.contains(searchQuery, ignoreCase = true) ||
                stop.number.toString().contains(searchQuery) ||
                stop.variants.any { variant ->
                    variant.key.contains(searchQuery, ignoreCase = true)
                }
            }
        }
    }
    
    fun handleOptionToggle() {
        viewState = ViewState.TRANSITIONING
        onIsClosestStopChange(!isClosestStop)
        
        if (!isClosestStop) {
            onSelectedStopsChange(emptyList())
        } else {
            onSelectedPerferredStopsInClosestStopsChange(false)
        }
    }
    
    fun handlePreferredToggle() {
        viewState = ViewState.TRANSITIONING
        onSelectedPerferredStopsInClosestStopsChange(!selectedPerferredStopsInClosestStops)
    }
    
    LaunchedEffect(viewState) {
        if (viewState == ViewState.TRANSITIONING) {
            delay(300)
            viewState = ViewState.READY
        }
    }
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (viewState == ViewState.TRANSITIONING) 0f else 1f,
        label = "contentAlpha"
    )
    
    when {
        !locationPermissionsState.allPermissionsGranted -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Location Permission Required",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "This app needs location access to show nearby bus stops for your widget.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { locationPermissionsState.launchMultiplePermissionRequest() }
                ) {
                    Text("Grant Permission")
                }
            }
        }
        
        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { 
                                Text(
                                    "All Stops",
                                    color = MaterialTheme.colorScheme.onSurface
                                ) 
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { 
                                Text(
                                    "Bookmarked",
                                    color = MaterialTheme.colorScheme.onSurface
                                ) 
                            }
                        )
                    }
                }
                
                item {
                    Text(
                        text = "Select the widget bus stops",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (selectedTab == 0) {
                    item {
                        Text(
                            text = "Hint: Use the search bar to search for and select stops that are not near your location.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = viewState != ViewState.TRANSITIONING) { 
                                    handleOptionToggle() 
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
                                    imageVector = if (isClosestStop) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    tint = if (isClosestStop) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Auto select closest stops everytime",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Automatically use closest stops based on your location everytime you look at the widget",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    if (isClosestStop) {
                        item {
                            AnimatedVisibility(
                                visible = isClosestStop,
                                enter = slideInVertically() + fadeIn(),
                                exit = slideOutVertically() + fadeOut()
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = viewState != ViewState.TRANSITIONING) { 
                                            handlePreferredToggle() 
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
                                            imageVector = if (selectedPerferredStopsInClosestStops) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                            contentDescription = null,
                                            tint = if (selectedPerferredStopsInClosestStops) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Select preferred stops",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "Allow you to select which stop(s) to display on the widget regardless of distance from your location",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (!isClosestStop || selectedPerferredStopsInClosestStops) {
                    item {
                        AnimatedVisibility(
                            visible = !isClosestStop || selectedPerferredStopsInClosestStops,
                            enter = slideInVertically() + fadeIn(),
                            exit = slideOutVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .alpha(contentAlpha)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Selected stops: ${
                                        if (isClosestStop && selectedPerferredStopsInClosestStops) preferredStops.size else selectedStops.size
                                    }/$maxStops",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    shape = RoundedCornerShape(28.dp)
                                ) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { query ->
                                            searchQuery = query
                                            scope.launch {
                                                stopsDataStore.searchForStops(query)
                                            }
                                        },
                                        placeholder = {
                                            Text(
                                                "Search stops, routes...",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Search,
                                                contentDescription = "Search",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = { 
                                                    searchQuery = ""
                                                    scope.launch {
                                                        stopsDataStore.searchForStops("")
                                                    }
                                                }) {
                                                    Icon(
                                                        Icons.Default.Clear,
                                                        contentDescription = "Clear search",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(28.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )
                                }
                            }
                        }
                    }
                    
                    when {
                        isLoadingStops || isLoadingLocation || isLoading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator()
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = when {
                                                isLoadingLocation -> "Getting your location..."
                                                isLoadingStops || isLoading -> "Loading stops..."
                                                else -> "Loading..."
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        error != null || locationError != null -> {
                            item {
                                val currentError = error ?: locationError
                                if (currentError != null) {
                                    ErrorView(
                                        error = currentError,
                                        title = "Error Loading Stops",
                                        onRetry = {
                                            stopsDataStore.clearError()
                                            mainViewModel.clearLocationError()
                                            mainViewModel.retry()
                                        },
                                        retryButtonText = when {
                                            locationError != null -> "Retry Location"
                                            else -> "Retry"
                                        },
                                        modifier = Modifier.padding(vertical = 16.dp)
                                    )
                                }
                            }
                        }
                        
                        currentFilteredStops.isEmpty() && isSearching -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator()
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Searching...")
                                    }
                                }
                            }
                        }
                        
                        currentFilteredStops.isEmpty() -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = if (searchQuery.isNotEmpty()) {
                                                "No stops found for \"$searchQuery\""
                                            } else {
                                                "No stops found nearby"
                                            },
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        Button(
                                            onClick = {
                                                mainViewModel.retry()
                                            }
                                        ) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            }
                        }
                        
                        else -> {
                            items(currentFilteredStops) { stop ->
                                val currentSelectedStops = if (isClosestStop && selectedPerferredStopsInClosestStops) preferredStops else selectedStops
                                val isSelected = currentSelectedStops.any { it.number == stop.number }
                                val canSelect = !isSelected && currentSelectedStops.size < maxStops
                                
                                Column {
                                    SelectableStopRow(
                                        stop = stop,
                                        isSelected = isSelected,
                                        enabled = canSelect || isSelected,
                                        onClick = {
                                            val currentList = if (isClosestStop && selectedPerferredStopsInClosestStops) preferredStops else selectedStops
                                            val newList = if (isSelected) {
                                                currentList.filter { it.number != stop.number }
                                            } else if (currentList.size < maxStops) {
                                                currentList + stop
                                            } else {
                                                currentList
                                            }
                                            
                                            
                                            
                                            if (isClosestStop && selectedPerferredStopsInClosestStops) {
                                                onPreferredStopsChange(newList)
                                            } else {
                                                onSelectedStopsChange(newList)
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                            
                            if (isSearching) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Text(
                                                "Loading more results...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
}

private enum class ViewState {
    LOADING, READY, TRANSITIONING
}

@Composable
private fun SelectableStopRow(
    stop: Stop,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(enabled = enabled) { onClick() }
            .alpha(if (enabled) 1f else 0.5f),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularCheckbox(
                checked = isSelected,
                onCheckedChange = { if (enabled) onClick() },
                enabled = enabled
            )
            Spacer(modifier = Modifier.width(12.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MapPreview(
                    latitude = stop.centre.geographic.latitude,
                    longitude = stop.centre.geographic.longitude,
                    direction = stop.direction,
                    modifier = Modifier
                       .size(width = PeekTransitConstants.MAP_PREVIEW_WIDTH_SIZE_DP.dp, height = PeekTransitConstants.MAP_PREVIEW_HEIGHT_SIZE_DP.dp)
                        .clip(RoundedCornerShape(8.dp))
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

                        val distance = if (stop.distances.direct != Double.POSITIVE_INFINITY) stop.distances.direct else null
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
                        }.distinctBy { it.key.split("-")[0] }
                        
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
                    }
                }
                
                val context = LocalContext.current
                val savedStopsManager = remember { SavedStopsManager.getInstance(context) }
                val isStopSaved = savedStopsManager.isStopSaved(stop)
                
                if (isStopSaved) {
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
}

private fun formatDistance(distanceInMeters: Double): String {
    return when {
        distanceInMeters < 1000 -> "${distanceInMeters.roundToInt()} meters away"
        else -> "${(distanceInMeters / 1000).let { "%.1f".format(it) }}km away"
    }
}