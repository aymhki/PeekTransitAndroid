package com.aymanhki.peektransit.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aymanhki.peektransit.data.models.WidgetModel
import com.aymanhki.peektransit.data.repository.StopsDataStore
import com.aymanhki.peektransit.managers.SavedWidgetsManager
import com.aymanhki.peektransit.ui.components.CustomPullToRefreshBox
import com.aymanhki.peektransit.ui.components.CustomTopAppBar
import com.aymanhki.peektransit.ui.components.WidgetRowView
import com.aymanhki.peektransit.ui.screens.widgetsetup.WidgetSetupView
import kotlinx.coroutines.launch



@Composable
fun WidgetsScreen(
    stopsDataStore: StopsDataStore,
    mainViewModel: com.aymanhki.peektransit.viewmodel.MainViewModel
) {
    val context = LocalContext.current
    val savedWidgetsManager = remember { SavedWidgetsManager.getInstance(context) }
    val savedWidgets by savedWidgetsManager.savedWidgets.collectAsState()
    
    var isEditing by remember { mutableStateOf(false) }
    var selectedWidgets by remember { mutableStateOf(setOf<String>()) }
    var showSetupView by remember { mutableStateOf(false) }
    var editingWidget by remember { mutableStateOf<WidgetModel?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        savedWidgetsManager.loadSavedWidgets()
        isLoading = false
    }
    
    fun deleteSelectedWidgets() {
        if (selectedWidgets.isNotEmpty()) {
            savedWidgetsManager.deleteWidgets(selectedWidgets)
            selectedWidgets = emptySet()
            isEditing = false
        }
    }
    
    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = { Text("Widgets") },
                actions = {
                    if (savedWidgets.isNotEmpty()) {
                        if (isEditing) {
                            TextButton(
                                onClick = {
                                    isEditing = false
                                    selectedWidgets = emptySet()
                                }
                            ) {
                                Text("Cancel")
                            }
                        } else {
                            TextButton(
                                onClick = { isEditing = true }
                            ) {
                                Text("Select")
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isEditing) {
                FloatingActionButton(
                    onClick = {
                        editingWidget = null
                        showSetupView = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Widget"
                    )
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isEditing && selectedWidgets.isNotEmpty(),
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedWidgets.size} selected",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        
                        TextButton(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        CustomPullToRefreshBox(
            modifier = Modifier.padding(paddingValues),
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    isRefreshing = true
                    try {
                        savedWidgetsManager.loadSavedWidgets()
                        kotlinx.coroutines.delay(100)
                    } finally {
                        isRefreshing = false
                    }
                }
            }
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                savedWidgets.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No saved Widgets",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Tap the + button to create your first widget",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(savedWidgets) { widget ->
                            WidgetRowView(
                                widget = widget,
                                isSelected = widget.id in selectedWidgets,
                                isSelectionMode = isEditing,
                                onEditClick = {
                                    editingWidget = widget
                                    showSetupView = true
                                },
                                onDeleteClick = {
                                    selectedWidgets = setOf(widget.id)
                                    showDeleteDialog = true
                                },
                                onSelectionToggle = {
                                    selectedWidgets = if (widget.id in selectedWidgets) {
                                        selectedWidgets - widget.id
                                    } else {
                                        selectedWidgets + widget.id
                                    }
                                }
                            )
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            }
        }
    }
    
    if (showSetupView) {
        WidgetSetupView(
            editingWidget = editingWidget,
            onDismiss = {
                showSetupView = false
                editingWidget = null
                coroutineScope.launch {
                    savedWidgetsManager.loadSavedWidgets()
                }
            },
            stopsDataStore = stopsDataStore,
            mainViewModel = mainViewModel
        )
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { 
                Text(
                    text = if (selectedWidgets.size == 1) {
                        "Delete Widget?"
                    } else {
                        "Delete ${selectedWidgets.size} Widgets?"
                    }
                )
            },
            text = {
                Text(
                    text = if (selectedWidgets.size == 1) {
                        "This widget configuration will be permanently deleted."
                    } else {
                        "These widget configurations will be permanently deleted."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteSelectedWidgets()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}