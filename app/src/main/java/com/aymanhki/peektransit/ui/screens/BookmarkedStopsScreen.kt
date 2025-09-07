package com.aymanhki.peektransit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aymanhki.peektransit.data.models.SavedStopsViewMode
import com.aymanhki.peektransit.data.models.SavedStopsViewMode.GRID_2
import com.aymanhki.peektransit.data.models.SavedStopsViewMode.GRID_3
import com.aymanhki.peektransit.data.models.SavedStopsViewMode.LIST
import com.aymanhki.peektransit.data.models.Stop
import com.aymanhki.peektransit.ui.components.StopRow
import com.aymanhki.peektransit.managers.SavedStopsManager
import com.aymanhki.peektransit.ui.components.CircularCheckbox
import com.aymanhki.peektransit.ui.components.CreateFolderBottomSheet
import com.aymanhki.peektransit.ui.components.CustomPullToRefreshBox
import com.aymanhki.peektransit.ui.components.CustomTopAppBar
import com.aymanhki.peektransit.ui.components.FolderGridItem
import com.aymanhki.peektransit.ui.components.FolderListItem
import com.aymanhki.peektransit.ui.components.MapPreview
import com.aymanhki.peektransit.ui.components.MoveToFolderBottomSheet
import com.aymanhki.peektransit.ui.components.StopGridItem
import com.aymanhki.peektransit.ui.components.VariantBadge
import com.aymanhki.peektransit.utils.PeekTransitConstants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.text.split


@Composable
fun BookmarkedStopsScreen(
    onNavigateToLiveStop: (Int) -> Unit = {},
    onNavigateToFolder: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedStopsManager = remember { SavedStopsManager.getInstance(context) }

    val uncategorizedStops = savedStopsManager.getUncategorizedStops().collectAsState(initial = emptyList())
    val folderCategories = savedStopsManager.getAllSavedStopsFolderCategories().collectAsState(initial = emptyList())
    val uncategorizedStopsState = savedStopsManager.getUncategorizedStops().collectAsState(initial = null)
    val folderCategoriesState = savedStopsManager.getAllSavedStopsFolderCategories().collectAsState(initial = null)
    val isLoading by savedStopsManager.isLoading.collectAsState(true)
    var searchQuery by rememberSaveable { mutableStateOf("") }

    var selectedStops by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedFolders by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isInSelectionMode by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showMoveToFolderDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    val mainSavedStopsViewMode = savedStopsManager.getMainSavedStopsScreenViewMode().collectAsState(initial = SavedStopsViewMode.DEFAULT)


    val filteredUncategorizedStops = if (searchQuery.isEmpty()) {
        uncategorizedStops.value.map { it.stopData }
    } else {
        uncategorizedStops.value.filter { savedStop ->
            val stop = savedStop.stopData
            stop.name.contains(searchQuery.trim(), ignoreCase = true) ||
                    stop.number.toString().contains(searchQuery.trim()) ||
                    stop.street.name.contains(searchQuery.trim(), ignoreCase = true) ||
                    stop.variants.any { variant ->
                        variant.key.contains(searchQuery.trim(), ignoreCase = true)
                    }
        }.map { it.stopData }
    }

    val filteredFolders = if (searchQuery.isEmpty()) {
        folderCategories.value
    } else {
        folderCategories.value.filter { folder ->
            folder.name.contains(searchQuery.trim(), ignoreCase = true)
        }
    }

    if (showCreateFolderDialog) {
        CreateFolderBottomSheet(
            onDismiss = { showCreateFolderDialog = false },
            onCreateFolder = { name, icons ->
                savedStopsManager.createSavedStopsFolderCategory(name, icons)
            },
        )
    }

    if (showMoveToFolderDialog) {
        MoveToFolderBottomSheet(
            onDismiss = { showMoveToFolderDialog = false },
            onMove = { folderIds ->
                savedStopsManager.moveUncategorizedStopsToFolders(selectedStops.toMutableList(), folderIds)
                selectedStops = emptySet()
                selectedFolders = emptySet()
                isInSelectionMode = false
                showMoveToFolderDialog = false
            },
            folders = folderCategories.value
        )
    }

    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text("Confirm Deletion") },
            text = {
                Column {
                    val stopsText = if (selectedStops.isNotEmpty()) "${selectedStops.size} stop${if (selectedStops.size > 1) "s" else ""}" else ""
                    val foldersText = if (selectedFolders.isNotEmpty()) "${selectedFolders.size} folder${if (selectedFolders.size > 1) "s" else ""}" else ""
                    val message = "Are you sure you want to remove ${listOf(stopsText, foldersText).filter { it.isNotBlank() }.joinToString(" and ")}?"

                    Text(message)

                    if (selectedFolders.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "All the stops saved in the folders will be removed if they are not saved in other folder(s) nor saved outside the folders.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        savedStopsManager.deleteSelectedFolders( selectedFolders.toList())
                        savedStopsManager.deleteUncategorizedSelectedStops(selectedStops.toList())
                        selectedStops = emptySet()
                        selectedFolders = emptySet()
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


    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = { Text(if (isInSelectionMode) "${selectedStops.size} selected" else "Saved Stops") },
                navigationIcon = if (isInSelectionMode) {
                    {
                        IconButton(onClick = {
                            isInSelectionMode = false
                            selectedStops = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                        }
                    }
                } else null,
                actions = {
                    if (isInSelectionMode) {
                        val allVisibleStops = filteredUncategorizedStops.map { it.number.toString() }.toSet()
                        val allVisibleFolders = filteredFolders.map { it.id }.toSet()
                        val areAllSelected = (allVisibleStops.isNotEmpty() || allVisibleFolders.isNotEmpty()) && selectedStops == allVisibleStops && selectedFolders == allVisibleFolders

                        IconButton(onClick = {
                            if (areAllSelected) {
                                selectedStops = emptySet()
                                selectedFolders = emptySet()
                            } else {
                                selectedStops = allVisibleStops
                                selectedFolders = allVisibleFolders
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
                            enabled = selectedStops.isNotEmpty() && selectedFolders.isEmpty() && folderCategories.value.isNotEmpty()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "Move to folder")
                        }

                        IconButton(
                            onClick = { showDeleteConfirmationDialog = true },
                            enabled = selectedStops.isNotEmpty() || selectedFolders.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    } else {

                        IconButton(onClick = {
                            savedStopsManager.toggleMainSavedStopsScreenViewMode()
                        }) {
                            Icon(
                                when (mainSavedStopsViewMode.value) {
                                    LIST -> Icons.Default.GridView
                                    GRID_2 -> Icons.Default.ViewModule
                                    GRID_3 -> Icons.AutoMirrored.Filled.ViewList
                                },
                                contentDescription = "Change view"
                            )
                        }

                        IconButton(onClick = { showCreateFolderDialog = true }) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "Create folder")
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
                        selectedFolders = emptySet()
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
                            delay(100)
                        } finally {
                            isRefreshing = false
                        }
                    }
                }
            ) {
                val uncategorizedStops = uncategorizedStopsState.value
                val folderCategories = folderCategoriesState.value
                val isDataLoading = uncategorizedStops == null || folderCategories == null

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
                } else if (uncategorizedStops.isNullOrEmpty() && folderCategories.isNullOrEmpty()) {
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
                    when (mainSavedStopsViewMode.value) {
                        LIST -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                item {
                                    SavedStopsSearBar(searchQuery) { searchQuery = it }
                                }

                                if (filteredFolders.isEmpty() && filteredUncategorizedStops.isEmpty()) {
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

                                    items(filteredFolders, key = { it.id }) { folder ->
                                        Box() {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (isInSelectionMode) {
                                                    Checkbox(
                                                        checked = selectedFolders.contains(folder.id),
                                                        onCheckedChange = {
                                                            selectedFolders = if (it) {
                                                                selectedFolders + folder.id
                                                            } else {
                                                                selectedFolders - folder.id
                                                            }
                                                        },
                                                    )
                                                }

                                                Box(modifier = Modifier.weight(1f)) {
                                                    FolderListItem(
                                                        folder = folder,
                                                        onClick = {
                                                            if (isInSelectionMode) {
                                                                selectedFolders = if (selectedFolders.contains(folder.id)) {
                                                                    selectedFolders - folder.id
                                                                } else {
                                                                    selectedFolders + folder.id
                                                                }
                                                            } else {
                                                                onNavigateToFolder(folder.id)
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    items(filteredUncategorizedStops, key = { it.number.toString() }) { savedStop ->
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
                                                        SelectableStopRow (
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
                                                                onNavigateToLiveStop(it)
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
                                columns = GridCells.Fixed(mainSavedStopsViewMode.value.columns),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item(span = { GridItemSpan(mainSavedStopsViewMode.value.columns) }) {
                                    SavedStopsSearBar(searchQuery) { searchQuery = it }
                                }

                                if (filteredFolders.isEmpty() && filteredUncategorizedStops.isEmpty()) {
                                    item(span = { GridItemSpan(mainSavedStopsViewMode.value.columns) }) {
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

                                    items(filteredFolders, key = { it.id }) { folder ->
                                        Box() {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box() {
                                                    FolderGridItem(
                                                        folder = folder,
                                                        stopViewMode = mainSavedStopsViewMode.value,
                                                        onClick = {
                                                            if (isInSelectionMode) {
                                                                selectedFolders = if (selectedFolders.contains(folder.id)) {
                                                                    selectedFolders - folder.id
                                                                } else {
                                                                    selectedFolders + folder.id
                                                                }
                                                            } else {
                                                                onNavigateToFolder(folder.id)
                                                            }
                                                        }
                                                    )
                                                }

                                                if (isInSelectionMode) {
                                                    Checkbox(
                                                        checked = selectedFolders.contains(folder.id),
                                                        onCheckedChange = {
                                                            selectedFolders = if (it) {
                                                                selectedFolders + folder.id
                                                            } else {
                                                                selectedFolders - folder.id
                                                            }
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    items(filteredUncategorizedStops, key = { it.number.toString() }) { savedStop ->
                                        Box() {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box() {
                                                    StopGridItem(
                                                        stop = savedStop,
                                                        stopViewMode = mainSavedStopsViewMode.value,
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

@Composable
fun SavedStopsSearBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {

    val focusManager = LocalFocusManager.current


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { onSearchQueryChange(it) },
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
                    IconButton(onClick = { onSearchQueryChange("") }) {
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
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}


@Composable
fun SelectableStopRow(
    stop: Stop,
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
                    sizeWidth = PeekTransitConstants.MAP_PREVIEW_WIDTH_SIZE_DP_IN_LIST,
                    sizeHeight = PeekTransitConstants.MAP_PREVIEW_HEIGHT_SIZE_DP_IN_LIST,
                    renderWidth = PeekTransitConstants.MAP_PREVIEW_RENDER_WIDTH_SIZE_DP_IN_LIST,
                    renderHeight = PeekTransitConstants.MAP_PREVIEW_RENDER_HEIGHT_SIZE_DP_IN_LIST,
                    markerSize = PeekTransitConstants.MAP_PREVIEW_MARKER_SIZE_DP_IN_LIST,
                    zoomLevel = PeekTransitConstants.MAP_PREVIEW_ZOOM_LEVEL_IN_LIST,
                    stopViewMode = SavedStopsViewMode.LIST,
                    bottomBannerPercentage = PeekTransitConstants.MAP_PREVIEW_BOTTOM_BANNER_PERCENTAGE_IN_LIST,
                    bottomBannerColor = MaterialTheme.colorScheme.surface,
                    bottomBannerOpacity = PeekTransitConstants.MAP_PREVIEW_BOTTOM_BANNER_OPACITY_IN_LIST,
                    showBottomBanner = PeekTransitConstants.MAP_PREVIEW_SHOW_BOTTOM_BANNER_IN_LIST,
                    modifier = Modifier
                        .width(PeekTransitConstants.MAP_PREVIEW_WIDTH_SIZE_DP_IN_LIST.dp)
                        .height(
                            if (PeekTransitConstants.MAP_PREVIEW_SHOW_BOTTOM_BANNER_IN_LIST) {
                                (PeekTransitConstants.MAP_PREVIEW_HEIGHT_SIZE_DP_IN_LIST - (PeekTransitConstants.MAP_PREVIEW_HEIGHT_SIZE_DP_IN_LIST * PeekTransitConstants.MAP_PREVIEW_BOTTOM_BANNER_PERCENTAGE_IN_LIST)).dp
                            } else PeekTransitConstants.MAP_PREVIEW_HEIGHT_SIZE_DP_IN_LIST.dp)
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
                                text = " ● ${PeekTransitConstants.formatDistance(distance)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (stop.variants.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val currentDate = Date()
                            val currentVariants = stop.variants.filter { variant ->
                                val effectiveFrom = variant.getEffectiveFromDate()
                                val effectiveTo = variant.getEffectiveToDate()
                                (effectiveFrom == null || currentDate >= effectiveFrom) &&
                                        (effectiveTo == null || currentDate <= effectiveTo)
                            }.distinctBy { it.key.split(PeekTransitConstants.VARIANT_KEY_SEPARATOR)[0] }

                            if (currentVariants.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    maxItemsInEachRow = 4
                                ) {
                                    currentVariants.forEach { variant ->
                                        VariantBadge(
                                            variant = variant,
                                            modifier = Modifier
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                val context = LocalContext.current
                val savedStopsManager = remember { SavedStopsManager.getInstance(context) }
                val isStopSaved = savedStopsManager.isStopSavedFlow(stop.number.toString()).collectAsState(false)

                if (isStopSaved.value) {
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
