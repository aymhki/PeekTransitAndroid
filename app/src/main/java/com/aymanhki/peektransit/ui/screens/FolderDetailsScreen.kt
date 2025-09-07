package com.aymanhki.peektransit.ui.screens

import androidx.compose.runtime.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aymanhki.peektransit.data.models.FolderCategory
import com.aymanhki.peektransit.data.models.SavedStopsViewMode
import com.aymanhki.peektransit.data.models.SavedStopsViewMode.GRID_2
import com.aymanhki.peektransit.data.models.SavedStopsViewMode.GRID_3
import com.aymanhki.peektransit.data.models.SavedStopsViewMode.LIST
import com.aymanhki.peektransit.ui.components.StopRow
import com.aymanhki.peektransit.managers.SavedStopsManager
import com.aymanhki.peektransit.ui.components.CustomPullToRefreshBox
import com.aymanhki.peektransit.ui.components.CustomTopAppBar
import com.aymanhki.peektransit.ui.components.MoveToFolderBottomSheet
import com.aymanhki.peektransit.ui.components.StopGridItem
import com.aymanhki.peektransit.utils.PeekTransitConstants
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun FolderDetailsScreen(
    folderId: String,
    onNavigateBack: () -> Unit,
    onNavigateToLiveStop: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedStopsManager = remember { SavedStopsManager.getInstance(context) }
    val thisFolderStops = savedStopsManager.getFolderCategoryStops(folderId = folderId).collectAsState(initial = emptyList())
    val thisFolderStopsState = savedStopsManager.getFolderCategoryStops(folderId = folderId).collectAsState(initial = null)
    val folderCategories = savedStopsManager.getAllSavedStopsFolderCategories().collectAsState(initial = emptyList())
    val isLoading by savedStopsManager.isLoading.collectAsState(true)
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedStops by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isInSelectionMode by remember { mutableStateOf(false) }
    var showMoveToFolderDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    val thisFolderStopsViewMode = savedStopsManager.getFolderSavedStopsScreenViewMode(folderId).collectAsState(initial = SavedStopsViewMode.DEFAULT)
    val uncategorizedStopsOutsideThisFolder = savedStopsManager.getUncategorizedStops().collectAsState(emptyList())


    val filterThisFolderStops = if (searchQuery.isEmpty()) {
        thisFolderStops.value.map { it.stopData }
    } else {
        thisFolderStops.value.filter { savedStop ->
            val stop = savedStop.stopData
            stop.name.contains(searchQuery.trim(), ignoreCase = true) ||
                    stop.number.toString().contains(searchQuery.trim()) ||
                    stop.street.name.contains(searchQuery.trim(), ignoreCase = true) ||
                    stop.variants.any { variant ->
                        variant.key.contains(searchQuery.trim(), ignoreCase = true)
                    }
        }.map { it.stopData }
    }


    if (showMoveToFolderDialog) {
        val localFolderCategories = mutableListOf<FolderCategory>()
        val uncategorizedStopIds = uncategorizedStopsOutsideThisFolder.value.map { it.id }
        localFolderCategories.add(0, FolderCategory( id = PeekTransitConstants.UNCATEGORIZED_FOLDER_ID, name = "Uncategorized", icons = listOf( "Folder"), stopOrder = uncategorizedStopIds ) )
        localFolderCategories.addAll(folderCategories.value)
        localFolderCategories.removeAll { it.id == folderId }
        MoveToFolderBottomSheet(
            onDismiss = { showMoveToFolderDialog = false },
            onMove = { folderIds ->
                if (folderIds.contains(PeekTransitConstants.UNCATEGORIZED_FOLDER_ID)) {
                    savedStopsManager.moveStops(selectedStops.toMutableList(), emptyList(), true, folderId)
                } else {
                    savedStopsManager.moveStops(selectedStops.toMutableList(), folderIds, false, folderId)
                }
                selectedStops = emptySet()
                isInSelectionMode = false
                showMoveToFolderDialog = false
            },
            folders = localFolderCategories
        )
    }

    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text("Confirm Deletion") },
            text = {
                Column {
                    val stopsText = if (selectedStops.isNotEmpty()) "${selectedStops.size} stop${if (selectedStops.size > 1) "s" else ""}" else ""
                    val message = "Are you sure you want to remove ${listOf(stopsText)} from this folder?"

                    Text(message)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        savedStopsManager.deleteSelectedSelectedStops(stopIds = selectedStops.toList(), folderId = folderId)
                        selectedStops = emptySet()
                        isInSelectionMode = false
                        showDeleteConfirmationDialog = false
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }


    val folderName = folderCategories.value.find { it.id == folderId }?.name ?: "Folder"

    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = { Text(if (isInSelectionMode) "${selectedStops.size} selected" else folderName) },
                navigationIcon = {
                    if (isInSelectionMode) {
                        IconButton(onClick = {
                            isInSelectionMode = false
                            selectedStops = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                        }
                    } else {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (isInSelectionMode) {
                        val allVisibleStops = filterThisFolderStops.map { it.number.toString() }.toSet()
                        val areAllSelected = (allVisibleStops.isNotEmpty() ) && selectedStops == allVisibleStops

                        IconButton(onClick = {
                            if (areAllSelected) {
                                selectedStops = emptySet()
                            } else {
                                selectedStops = allVisibleStops
                            }
                        }) {
                            Icon(
                                imageVector = if (areAllSelected) {
                                    Icons.Default.RemoveDone
                                } else {
                                    Icons.Default.DoneAll
                                },
                                contentDescription = if (areAllSelected) "Deselect All" else "Select All"
                            )
                        }

                        IconButton(
                            onClick = { showMoveToFolderDialog = true },
                            enabled = selectedStops.isNotEmpty()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "Move to folder")
                        }

                        IconButton(
                            onClick = { showDeleteConfirmationDialog = true },
                            enabled = selectedStops.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    } else {

                        IconButton(onClick = {
                            savedStopsManager.toggleFolderSavedStopsScreenViewMode(folderId)
                        }) {
                            Icon(
                                when (thisFolderStopsViewMode.value) {
                                    LIST -> Icons.Default.GridView
                                    GRID_2 -> Icons.Default.ViewModule
                                    GRID_3 -> Icons.AutoMirrored.Filled.ViewList
                                },
                                contentDescription = "Change view"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isInSelectionMode) {
                FloatingActionButton(
                    onClick = { isInSelectionMode = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit"
                    )
                }
            } else {
                FloatingActionButton(
                    onClick = {
                        isInSelectionMode = false
                        selectedStops = emptySet()
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done"
                    )
                }
            }
        },
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
                            kotlinx.coroutines.delay(100)
                        } finally {
                            isRefreshing = false
                        }
                    }
                }
            ) {
                val thisFolderStops = thisFolderStopsState.value
                val isDataLoading = thisFolderStops == null

                if (isDataLoading || isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
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
                } else if (thisFolderStops.isNullOrEmpty() ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No Saved Stops",
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                } else {
                    when (thisFolderStopsViewMode.value) {
                        LIST -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                item {
                                    SavedStopsSearBar(searchQuery) { searchQuery = it }
                                }

                                if (filterThisFolderStops.isEmpty()) {
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
                                } else {


                                    items(filterThisFolderStops, key = { it.number.toString() }) { savedStop ->
                                        Box() {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp),

                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (isInSelectionMode) {
                                                    Checkbox(
                                                        checked = selectedStops.contains(savedStop.number.toString()),
                                                        onCheckedChange = {
                                                            selectedStops = if (it) {
                                                                selectedStops + savedStop.number.toString()
                                                            } else {
                                                                selectedStops - savedStop.number.toString()
                                                            }
                                                        },
                                                    )
                                                }

                                                Box(modifier = Modifier.weight(1f)) {
                                                    if (isInSelectionMode) {
                                                        SelectableStopRow(
                                                            stop = savedStop,
                                                            enabled = true,
                                                            onClick = {
                                                                selectedStops = if (selectedStops.contains(savedStop.number.toString())) {
                                                                    selectedStops - savedStop.number.toString()
                                                                } else {
                                                                    selectedStops + savedStop.number.toString()
                                                                }
                                                            }
                                                        )
                                                    } else {
                                                        StopRow(
                                                            stop = savedStop,
                                                            distance = null,
                                                            onNavigateToLiveStop = {
                                                                if (!isInSelectionMode) {
                                                                    onNavigateToLiveStop(it)
                                                                } else {
                                                                    selectedStops = if (selectedStops.contains(savedStop.number.toString())) {
                                                                        selectedStops - savedStop.number.toString()
                                                                    } else {
                                                                        selectedStops + savedStop.number.toString()
                                                                    }
                                                                }
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

                        GRID_2, GRID_3 -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(thisFolderStopsViewMode.value.columns),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item(span = { GridItemSpan(thisFolderStopsViewMode.value.columns) }) {
                                    SavedStopsSearBar(searchQuery) { searchQuery = it }
                                }

                                if ( filterThisFolderStops.isEmpty()) {
                                    item(span = { GridItemSpan(thisFolderStopsViewMode.value.columns) }) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
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
                                } else {

                                    items(filterThisFolderStops, key = { it.number.toString() }) { savedStop ->
                                        Box() {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box() {
                                                    StopGridItem(
                                                        stop = savedStop,
                                                        stopViewMode = thisFolderStopsViewMode.value,
                                                        onNavigateToLiveStop = {
                                                            if (!isInSelectionMode) {
                                                                onNavigateToLiveStop(it)
                                                            } else {
                                                                selectedStops = if (selectedStops.contains(savedStop.number.toString())) {
                                                                    selectedStops - savedStop.number.toString()
                                                                } else {
                                                                    selectedStops + savedStop.number.toString()
                                                                }
                                                            }
                                                        }
                                                    )
                                                }

                                                if (isInSelectionMode) {
                                                    Checkbox(
                                                        checked = selectedStops.contains(savedStop.number.toString()),
                                                        onCheckedChange = {
                                                            selectedStops = if (it) {
                                                                selectedStops + savedStop.number.toString()
                                                            } else {
                                                                selectedStops - savedStop.toString()
                                                            }
                                                        },
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
    }
}
