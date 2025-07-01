package com.aymanhki.peektransit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aymanhki.peektransit.data.models.Stop
import com.aymanhki.peektransit.ui.components.StopRow
import com.aymanhki.peektransit.managers.SavedStopsManager
import com.aymanhki.peektransit.ui.components.CustomPullToRefreshBox
import com.aymanhki.peektransit.ui.components.CustomTopAppBar
import kotlinx.coroutines.launch
@Composable
fun BookmarkedStopsScreen(
    onNavigateToLiveStop: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedStopsManager = remember { SavedStopsManager.getInstance(context) }
    val savedStops by savedStopsManager.savedStops.collectAsState()
    val isLoading by savedStopsManager.isLoading.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredStops = if (searchQuery.isEmpty()) {
        savedStops.map { it.stopData }
    } else {
        savedStops.filter { savedStop ->
            val stop = savedStop.stopData
            stop.name.contains(searchQuery, ignoreCase = true) ||
            stop.number.toString().contains(searchQuery) ||
            stop.street.name.contains(searchQuery, ignoreCase = true) ||
            stop.variants.any { variant ->
                variant.key.contains(searchQuery, ignoreCase = true)
            }
        }.map { it.stopData }
    }
    
    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = { Text("Saved Stops") }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            var isRefreshing by remember { mutableStateOf(false) }
            
            CustomPullToRefreshBox(
                modifier = Modifier.padding(paddingValues),
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        try {
                            savedStopsManager.loadSavedStops()
                            kotlinx.coroutines.delay(100)
                        } finally {
                            isRefreshing = false
                        }
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
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        "Search saved stops...",
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
                                        IconButton(onClick = { searchQuery = "" }) {
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
                        isLoading -> {
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
                                        CircularProgressIndicator()
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Loading saved stops...")
                                    }
                                }
                            }
                        }
                        
                        savedStops.isEmpty() -> {
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
                                        Icon(
                                            imageVector = Icons.Default.BookmarkBorder,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        Text(
                                            text = "No Saved Stops",
                                            style = MaterialTheme.typography.headlineSmall,
                                            textAlign = TextAlign.Center
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Text(
                                            text = "Save your favorite bus stops from the Map or Stops tab to see them here.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                        
                        filteredStops.isEmpty() && searchQuery.isNotEmpty() -> {
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
                                            text = "No saved stops found for \"$searchQuery\"",
                                            style = MaterialTheme.typography.bodyLarge,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                        
                        else -> {
                            items(filteredStops, key = { stop ->
                                "${stop.number}_${stop.variants.size}_${stop.variants.hashCode()}"
                            }) { stop ->
                                StopRow(
                                    stop = stop,
                                    distance = null,
                                    onNavigateToLiveStop = onNavigateToLiveStop
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}