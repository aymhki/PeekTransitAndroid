package com.aymanhki.peektransit.ui.components

import android.location.Location
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aymanhki.peektransit.data.models.TripPlan
import com.aymanhki.peektransit.data.network.WinnipegTransitAPI
import com.aymanhki.peektransit.services.PlacesApiService
import com.aymanhki.peektransit.utils.AddressSearchHandler
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.SegmentType
import com.google.android.libraries.places.api.model.AutocompletePrediction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun AddressSearchView(
    onRouteSelected: (TripPlan, Boolean) -> Unit,
    userLocation: Location?,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val searchHandler = remember { AddressSearchHandler(context) }
    val placesApiService = remember { PlacesApiService.getInstance(context) }
    val transitApi = remember { WinnipegTransitAPI.getInstance() }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    var searchQuery by remember { mutableStateOf("") }
    val searchResults by searchHandler.searchResults.observeAsState(emptyList())
    val isSearchingPredictions by searchHandler.isSearching.observeAsState(false)
    val error by searchHandler.error.observeAsState()

    var showingRouteDetails by remember { mutableStateOf(false) }
    var isLoadingRoutes by remember { mutableStateOf(false) }
    var routePlans by remember { mutableStateOf<List<TripPlan>>(emptyList()) }
    var topRecommendedRoutes by remember { mutableStateOf<List<TripPlan>>(emptyList()) }
    var routeError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            searchHandler.onCleared()
        }
    }

    LaunchedEffect(Unit) {
        placesApiService.completeSession()
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = if (error != null || routeError != null || searchResults.isNotEmpty() || isSearchingPredictions) {
                RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
            } else {
                RoundedCornerShape(8.dp)
            },
            elevation = CardDefaults.cardElevation(defaultElevation = 100.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        searchHandler.updateSearchQuery(it)
                        routeError = null

                        if (showingRouteDetails && it.isNotEmpty()) {
                            showingRouteDetails = false
                            routeError = null
                            topRecommendedRoutes = emptyList()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = { Text("Type your destination address") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, autoCorrect = false),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController?.hide()
                    }),
                    enabled = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                IconButton(onClick = {
                    searchQuery = ""
                    placesApiService.completeSession()
                    onClose()
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        }

        if (showingRouteDetails || error != null || routeError != null || searchResults.isNotEmpty() || isSearchingPredictions || isLoadingRoutes || routePlans.isNotEmpty()) {

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 64.dp)
                        .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    when {
                        isSearchingPredictions || isLoadingRoutes -> {
                            LoadingView()
                        }

                        (error != null || routeError != null) -> {
                            ErrorViewForSearchDestination(message = routeError ?: error?.message ?: "Unknown error") {
                                searchHandler.clearError()
                                routeError = null
                            }
                        }

                        showingRouteDetails && topRecommendedRoutes.isNotEmpty() -> {
                            RouteDetailsView(
                                routes = topRecommendedRoutes,
                                onRouteSelected = onRouteSelected,
                                onClose = onClose
                            )
                        }

                        searchQuery.isNotEmpty() && !showingRouteDetails -> {
                            if (searchResults.isEmpty() && !isSearchingPredictions && searchQuery.length > PeekTransitConstants.NUM_CHARS_TO_UPDATE_ADDRESS_SEARCH_QUERY_AFTER) {
                                NoResultsView()
                            } else {
                                SearchResultsView(
                                    results = searchResults,
                                    onResultSelected = { prediction ->
                                        handlePlaceSelection(
                                            prediction = prediction,
                                            placesApiService = placesApiService,
                                            transitApi = transitApi,
                                            userLocation = userLocation,
                                            onRoutesLoaded = { routes ->
                                                routePlans = routes
                                                if (routes.isNotEmpty()) {
                                                    topRecommendedRoutes = getTopRecommendedRoutes(routes)
                                                    showingRouteDetails = true
                                                } else {
                                                    routeError = "No transit routes available to this destination right now"
                                                }
                                                isLoadingRoutes = false
                                            },
                                            onError = { error ->
                                                routeError = error
                                                isLoadingRoutes = false
                                            },
                                            onLoadingStarted = {
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                                searchQuery = prediction.getFullText(null).toString()
                                                isLoadingRoutes = true
                                                routeError = null
                                            },
                                            scope = scope
                                        )
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

private fun handlePlaceSelection(
    prediction: AutocompletePrediction,
    placesApiService: PlacesApiService,
    transitApi: WinnipegTransitAPI,
    userLocation: Location?,
    onRoutesLoaded: (List<TripPlan>) -> Unit,
    onError: (String) -> Unit,
    onLoadingStarted: () -> Unit,
    scope: CoroutineScope
) {
    onLoadingStarted()

    if (userLocation == null) {
        onError("Could not determine your location")
        return
    }

    scope.launch {
        try {
            val place = placesApiService.getPlaceDetails(prediction.placeId)
            val placeLatLng = place.location

            if (placeLatLng == null) {
                onError("Location details not available")
                return@launch
            }

            val origin = Location("").apply {
                latitude = userLocation.latitude
                longitude = userLocation.longitude
            }

            val destination = Location("").apply {
                latitude = placeLatLng.latitude
                longitude = placeLatLng.longitude
            }

            val originLocationKey = transitApi.getLocationKey(origin.latitude, origin.longitude)
            val destinationLocationKey = transitApi.getLocationKey(destination.latitude, destination.longitude)

            if (originLocationKey == null || destinationLocationKey == null) {
                val routes = transitApi.findTrip(origin, destination)
                onRoutesLoaded(routes)
            } else {
                val routes = transitApi.findTripWithLocationKey(originLocationKey, destinationLocationKey)
                onRoutesLoaded(routes)
            }

            placesApiService.completeSession()
        } catch (e: Exception) {
            placesApiService.completeSession()
            if (e is CancellationException) throw e
            onError("Failed to find transit routes: ${e.localizedMessage ?: e.message}")
        }
    }
}

private fun getTopRecommendedRoutes(availableRoutes: List<TripPlan>): List<TripPlan> {
    return when {
        availableRoutes.size >= 5 -> TripPlan.getTopRecommendedRoutes(availableRoutes, 5)
        availableRoutes.size >= 3 -> TripPlan.getTopRecommendedRoutes(availableRoutes, 3)
        else -> TripPlan.getTopRecommendedRoutes(availableRoutes, availableRoutes.size)
    }
}

@Composable
fun LoadingView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(bottom = 64.dp),

        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorViewForSearchDestination(message: String, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onDismiss) {
            Text("Dismiss")
        }
    }
}

@Composable
fun NoResultsView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("No results found", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun SearchResultsView(
    results: List<AutocompletePrediction>,
    onResultSelected: (AutocompletePrediction) -> Unit
) {
    LazyColumn {
        items(results) { prediction ->
            SearchResultItem(prediction = prediction, onClick = { onResultSelected(prediction) })

            if (prediction != results.last()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = DividerDefaults.Thickness, color = DividerDefaults.color)
            }
        }
    }
}

@Composable
fun SearchResultItem(prediction: AutocompletePrediction, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = prediction.getPrimaryText(null).toString(),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = prediction.getSecondaryText(null).toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RouteDetailsView(
    routes: List<TripPlan>,
    onRouteSelected: (TripPlan, Boolean) -> Unit,
    onClose: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        items(routes.size) { index ->
            val route = routes[index]
            RouteItem(
                route = route,
                index = index + 1,
                totalRoutes = routes.size,
                onRouteSelected = onRouteSelected,
                onClose = onClose
            )

            if (index < routes.size - 1) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = DividerDefaults.Thickness, color = DividerDefaults.color)
            }
        }
    }
}

@Composable
fun RouteItem(
    route: TripPlan,
    index: Int,
    totalRoutes: Int,
    onRouteSelected: (TripPlan, Boolean) -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)

        ) {
            Text(
                text = "Route $index/$totalRoutes",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Total trip: ${route.duration} minutes",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Start: ${route.startTimeString}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "End: ${route.endTimeString}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            route.segments.forEachIndexed { index, segment ->

                if (index != 0 ){
                    Spacer(modifier = Modifier.height(16.dp))
                }

                when (segment.type) {
                    SegmentType.WALK -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Column(
                                modifier = Modifier.weight(1f)
                                .padding(start = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)

                            ) {
                                Text(
                                    text = "Walk ${(segment.duration)} minutes",
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                if (segment.fromStop != null) {
                                    Text(
                                        text = "From: ${segment.fromStop.name} (${segment.startTimeStr})",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                if (segment.toStop != null) {
                                    Text(
                                        text = "To: ${segment.toStop.name} (${segment.endTimeStr})",
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                }
                            }
                        }
                    }
                    SegmentType.RIDE -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBus,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Column(
                                modifier = Modifier.weight(1f)
                                .padding(start = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (segment.variantKey == null || segment.variantName == null) {
                                    var textToDisplay: String = ""

                                    if (segment.variantKey != null) {
                                        textToDisplay += segment.variantKey.split("-")[0]
                                    }

                                    if (segment.variantName != null && !segment.variantName.contains(textToDisplay)) {
                                        textToDisplay += " ${segment.variantName}"
                                    } else if (segment.variantName != null && segment.variantName.contains(textToDisplay)) {
                                        textToDisplay += " ${segment.variantName.replace(textToDisplay, "")}"
                                    }

                                    Text(
                                        text = textToDisplay,
                                        style = MaterialTheme.typography.bodyLarge
                                    )

                                } else {
                                    Text(
                                        text = "${segment.variantKey.split("-")[0]} ${segment.variantName.replace(segment.variantKey.split("-")[0], "")}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }

                                if (segment.fromStop != null) {
                                    Text(
                                        text = "Board at: ${segment.fromStop.name} (${segment.startTimeStr})",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                if (segment.toStop != null) {
                                    Text(
                                        text = "Exit at: ${segment.toStop.name} (${segment.endTimeStr})",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Text(
                                    text = "Ride ${segment.duration} minutes",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                            }
                        }
                    }
                    SegmentType.TRANSFER -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Column(
                                modifier = Modifier.weight(1f)
                                    .padding(start = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {

                                Text(
                                    text = "Transfer",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }

                if (index < route.segments.size - 1) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = DividerDefaults.Thickness, color = DividerDefaults.color)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onClose()
                        onRouteSelected(route, false)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Show bus stop on map")
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onClose()
                        onRouteSelected(route, true)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Show bus stop schedule")
                }
            }
        }
    }
}

@Composable
fun DestinationSearchButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 3.dp
        ),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 12.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsBus,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Don't know which bus to take?",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 12.sp,
                textAlign = TextAlign.Start,
                maxLines = 2,
            )
        }
    }
}


