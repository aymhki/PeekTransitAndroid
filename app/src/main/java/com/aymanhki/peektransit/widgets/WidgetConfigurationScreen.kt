package com.aymanhki.peektransit.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aymanhki.peektransit.data.models.WidgetModel
import com.aymanhki.peektransit.ui.components.CustomTopAppBar
import com.aymanhki.peektransit.ui.theme.AccentBlue
import com.aymanhki.peektransit.ui.theme.PeekTransitTheme


@Composable
fun WidgetConfigurationScreen(
    title: String,
    widgetsToShow: List<WidgetModel>,
    onWidgetSelected: (WidgetModel) -> Unit,
    onCloseWidgetConfigurationScreen: () -> Unit,
    widgetSize: String
) {
    PeekTransitTheme {
        val showNoConfigsDialog = remember { mutableStateOf(widgetsToShow.isEmpty()) }

        if (showNoConfigsDialog.value) {
            AlertDialog(
                onDismissRequest = {
                    showNoConfigsDialog.value = false
                    onCloseWidgetConfigurationScreen()
                },
                title = { Text("No Widget Configurations") },
                text = { Text("There are no widget configurations for this widget size ($widgetSize). Go back to the app and create one then come back here to get started.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showNoConfigsDialog.value = false
                            onCloseWidgetConfigurationScreen()
                        }
                    ) {
                        Text("Okay")
                    }
                }
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
            topBar = {
                    CustomTopAppBar(
                        title = {
                            Text(
                                text = title,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = onCloseWidgetConfigurationScreen,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close"
                                )
                            }
                        }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.padding(innerPadding)
            ) {
                items(widgetsToShow.size) { index ->
                    val widget = widgetsToShow[index]

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onClick = { onWidgetSelected(widget) }
                    ) {
                        Text(
                            text = widget.widgetData["name"] as? String ?: "Unnamed Widget",
                            maxLines = 5,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp),
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                            fontWeight = FontWeight.Normal
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (widget.widgetData["isClosestStop"] as? Boolean == true) {
                            Text(
                                text = "Requires location access to show closest stop",
                                color = AccentBlue,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp),
                                fontSize =  MaterialTheme.typography.bodyMedium.fontSize,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

        }
    }
}


