package com.aymanhki.peektransit.ui.screens

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aymanhki.peektransit.data.models.Stop
import com.aymanhki.peektransit.ui.components.StopRow
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.location.LocationManagerProvider
import com.aymanhki.peektransit.viewmodel.MainViewModel
import com.aymanhki.peektransit.utils.permissions.rememberMultiplePermissionsState
import com.aymanhki.peektransit.ui.components.CustomPullToRefreshBox
import com.aymanhki.peektransit.ui.components.CustomTopAppBar
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay


@Composable
fun ListViewScreen(
    viewModel: MainViewModel,
    onNavigateToLiveStop: (Int) -> Unit = {},
    isCurrentDestination: Boolean = false
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

    val stops by viewModel.stops.observeAsState(emptyList())
    val searchResults by viewModel.searchResults.observeAsState(emptyList())

    LaunchedEffect(stops) {
        println("ListViewScreen: Stops list updated with ${stops.size} stops")
    }
    val isLoadingStops by viewModel.isLoadingStops.observeAsState(false)
    val isLoadingLocation by viewModel.isLoadingLocation.observeAsState(false)
    val isLoading by viewModel.isLoading.observeAsState(false)
    val isSearching by viewModel.isSearching.observeAsState(false)
    val error by viewModel.error.observeAsState()
    val locationError by viewModel.locationError.observeAsState()
    val searchError by viewModel.searchError.observeAsState()

    val viewModelSearchQuery by viewModel.searchQuery.observeAsState("")
    val lastSearchedQuery by viewModel.lastSearchedQuery.observeAsState("")
    
    var localSearchQuery by remember { mutableStateOf("") }
    var isDebouncing by remember { mutableStateOf(false) }
    var isUserInput by remember { mutableStateOf(false) }
    
    LaunchedEffect(viewModelSearchQuery) {
        if (localSearchQuery != viewModelSearchQuery) {
            isUserInput = false
            localSearchQuery = viewModelSearchQuery
        }
    }

    val isViewModelInitialized by viewModel.isInitialized.observeAsState(false)

    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            viewModel.initializeGlobal()
        }
    }

    LaunchedEffect(localSearchQuery, isUserInput) {
        if (!isUserInput) {
            viewModel.updateSearchQuery(localSearchQuery)
            return@LaunchedEffect
        }
        
        if (localSearchQuery.isNotEmpty()) {
            isDebouncing = true
        } else {
            isDebouncing = false
            viewModel.updateSearchQuery("")
            if (lastSearchedQuery.isNotEmpty()) {
                viewModel.searchForStops("", null)
            }
            isUserInput = false
            return@LaunchedEffect
        }
        
        delay(PeekTransitConstants.SEARCH_DEBOUNCE_DELAY_MS)
        isDebouncing = false
        viewModel.updateSearchQuery(localSearchQuery)
        
        if (localSearchQuery != lastSearchedQuery) {
            val location = locationManager.getCurrentLocation()
            viewModel.searchForStops(localSearchQuery, location)
        }
        
        isUserInput = false
    }

    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = { Text("Nearby Stops") }
            )
        }
    ) { paddingValues ->
        when {
            !locationPermissionsState.allPermissionsGranted -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
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
                        text = "This app needs location access to show nearby bus stops.",
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
                Box(modifier = Modifier.fillMaxSize()) {
                    CustomPullToRefreshBox(
                        modifier = Modifier.padding(paddingValues),
                        isRefreshing = isLoadingStops || isLoadingLocation || isLoading || isSearching || isDebouncing,
                        onRefresh = {
                            if (localSearchQuery.isNotEmpty()) {
                                scope.launch {
                                    val location = locationManager.getCurrentLocation()
                                    viewModel.searchForStops(localSearchQuery, location)
                                }
                            } else {
                                viewModel.retry()
                            }
                        }
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    shape = RoundedCornerShape(28.dp)
                                ) {
                                    OutlinedTextField(
                                        value = localSearchQuery,
                                        onValueChange = { 
                                            isUserInput = true
                                            localSearchQuery = it 
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
                                            if (localSearchQuery.isNotEmpty()) {
                                                IconButton(onClick = { 
                                                    isUserInput = true
                                                    localSearchQuery = ""
                                                    viewModel.clearSearchQuery()
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
                            when {
                                isLoadingStops || isLoadingLocation || isLoading || isSearching || isDebouncing -> {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillParentMaxHeight(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                CircularProgressIndicator()
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Text(
                                                    text = when {
                                                        isSearching || isDebouncing -> "Searching..."
                                                        isLoadingLocation -> "Getting your location..."
                                                        isLoadingStops || isLoading -> "Loading nearby stops..."
                                                        else -> "Loading..."
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }

                                else -> {
                                    val combinedStops = mutableListOf<Stop>().apply {
                                        addAll(stops)
                                        val existingStopNumbers = stops.map { it.number }.toSet()
                                        for (stop in searchResults) {
                                            if (stop.number != -1 && !existingStopNumbers.contains(stop.number)) {
                                                add(stop)
                                            }
                                        }
                                    }

                                    val filteredStops = if (localSearchQuery.isEmpty()) {
                                        combinedStops
                                    } else {
                                        combinedStops.filter { stop ->
                                            stop.name.contains(localSearchQuery, ignoreCase = true) ||
                                            stop.number.toString().contains(localSearchQuery) ||
                                            stop.variants.any { variant ->
                                                variant.key.contains(localSearchQuery, ignoreCase = true) ||
                                                variant.name.contains(localSearchQuery, ignoreCase = true)
                                            }
                                        }
                                    }

                                    val localFilteredStops = if (localSearchQuery.isEmpty()) {
                                        stops
                                    } else {
                                        stops.filter { stop ->
                                            stop.name.contains(localSearchQuery, ignoreCase = true) ||
                                            stop.number.toString().contains(localSearchQuery) ||
                                            stop.variants.any { variant ->
                                                variant.key.contains(localSearchQuery, ignoreCase = true) ||
                                                variant.name.contains(localSearchQuery, ignoreCase = true)
                                            }
                                        }
                                    }

                                    val currentError = if (localSearchQuery.isNotEmpty()) searchError else (error ?: locationError)
                                    val hasLocalResults = localFilteredStops.isNotEmpty()
                                    val isSearchActive = localSearchQuery.isNotEmpty()

                                    when {
                                        isSearchActive && hasLocalResults -> {
                                            items(filteredStops, key = { stop ->
                                                "${stop.number}_${stop.variants.size}_${stop.variants.hashCode()}"
                                            }) { stop ->
                                                StopRow(
                                                    stop = stop,
                                                    distance = stop.getDistance(),
                                                    onNavigateToLiveStop = onNavigateToLiveStop
                                                )
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
                                                                "Finding more stops...",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        
                                        isSearchActive && !hasLocalResults && !isSearching && currentError == null -> {
                                            item {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .fillParentMaxHeight(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "No stops found for \"$localSearchQuery\"",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                        
                                        isSearchActive && !hasLocalResults && isSearching -> {
                                            item {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .fillParentMaxHeight(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        modifier = Modifier.padding(16.dp)
                                                    ) {
                                                        CircularProgressIndicator()
                                                        Spacer(modifier = Modifier.height(16.dp))
                                                        Text(
                                                            text = "Searching...",
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        
                                        !isSearchActive && filteredStops.isNotEmpty() -> {
                                            items(filteredStops, key = { stop ->
                                                "${stop.number}_${stop.variants.size}_${stop.variants.hashCode()}"
                                            }) { stop ->
                                                StopRow(
                                                    stop = stop,
                                                    distance = stop.getDistance(),
                                                    onNavigateToLiveStop = onNavigateToLiveStop
                                                )
                                            }
                                        }
                                        
                                        !isSearchActive && filteredStops.isEmpty() && currentError == null -> {
                                            item {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .fillParentMaxHeight(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(16.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(
                                                            text = "No nearby stops found",
                                                            style = MaterialTheme.typography.bodyLarge,
                                                            textAlign = TextAlign.Center
                                                        )

                                                        Spacer(modifier = Modifier.height(16.dp))

                                                        Button(
                                                            onClick = {
                                                                viewModel.retry()
                                                            }
                                                        ) {
                                                            Text("Retry")
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

                    val currentError = if (localSearchQuery.isNotEmpty()) searchError else (error ?: locationError)
                    currentError?.let { transitError ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Snackbar(
                                action = {
                                    TextButton(
                                        onClick = {
                                            if (localSearchQuery.isNotEmpty()) {
                                                viewModel.clearSearchError()
                                            } else {
                                                viewModel.clearError()
                                                viewModel.clearLocationError()
                                            }
                                        }
                                    ) {
                                        Text("Dismiss")
                                    }
                                }
                            ) {
                                Text(
                                    text = if (transitError.message.contains("outside Winnipeg")) {
                                        "This app only works in Winnipeg, MB. ${transitError.message}"
                                    } else {
                                        transitError.message
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
