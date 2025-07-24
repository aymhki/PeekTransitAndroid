package com.aymanhki.peektransit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aymanhki.peektransit.managers.SettingsManager
import com.aymanhki.peektransit.ui.components.CustomTopAppBar
import com.aymanhki.peektransit.ui.components.SettingsRow
import com.aymanhki.peektransit.utils.DefaultTab
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.height
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

data class SettingsSection(
    val title: String,
    val items: List<SettingsItem>
)

data class SettingsItem(
    val icon: ImageVector,
    val iconColor: Color,
    val text: String,
    val action: SettingsAction,
    val endContent: @Composable (() -> Unit)? = null
)

data class WidgetUpdateMode(val isManual: Boolean)

sealed class SettingsAction {
    object ThemeSelection : SettingsAction()
    object About : SettingsAction()
    object Credits : SettingsAction()
    object TermsAndPrivacy : SettingsAction()
}

@Composable
fun MoreScreen(
    onNavigateToThemeSelection: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToCredits: () -> Unit = {},
    onNavigateToTermsAndPrivacy: () -> Unit = {}
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    var selectedDefaultTab by remember { mutableStateOf(settingsManager.defaultTab) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val settingsSections = listOf(
        SettingsSection(
            title = "Preferences",
            items = listOf(
                SettingsItem(
                    icon = Icons.Default.Palette,
                    iconColor = Color(0xFF9C27B0),
                    text = "Change App & Widget Theme",
                    action = SettingsAction.ThemeSelection
                ),
                SettingsItem(
                    icon = Icons.Default.Apps,
                    iconColor = Color(0xFF2196F3),
                    text = "Default Tab",
                    action = SettingsAction.ThemeSelection,
                    endContent = {
                        var expanded by remember { mutableStateOf(false) }

                        Box {
                            OutlinedButton(
                                onClick = { expanded = !expanded },
                                modifier = Modifier.width(140.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = selectedDefaultTab.displayName,
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (expanded) "Collapse" else "Expand",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DefaultTab.entries.forEach { tab ->
                                    DropdownMenuItem(
                                        text = { Text(tab.displayName) },
                                        onClick = {
                                            selectedDefaultTab = tab
                                            settingsManager.defaultTab = tab
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                ),
                SettingsItem(
                    icon = Icons.Default.Update,
                    iconColor = Color(0xFF4CAF50),
                    text = "Widget Updates",
                    action = SettingsAction.ThemeSelection
                )
            )
        ),
        SettingsSection(
            title = "Legal",
            items = listOf(
                SettingsItem(
                    icon = Icons.Default.Description,
                    iconColor = Color(0xFF2196F3),
                    text = "Terms of Service & Privacy",
                    action = SettingsAction.TermsAndPrivacy
                ),
                SettingsItem(
                    icon = Icons.Default.People,
                    iconColor = Color(0xFF673AB7),
                    text = "Credits",
                    action = SettingsAction.Credits
                )
            )
        ),
        SettingsSection(
            title = "Info",
            items = listOf(
                SettingsItem(
                    icon = Icons.Default.Info,
                    iconColor = Color(0xFFF44336),
                    text = "About",
                    action = SettingsAction.About
                )
            )
        )
    )
    
    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = {
                    Text(
                        text = "More",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(settingsSections) { section ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = section.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        section.items.forEach { item ->
                            if (item.text.contains("Widget Updates")) {
                                var expanded by remember { mutableStateOf(false) }
                                var widgetUpdateMode by remember {
                                    mutableStateOf(WidgetUpdateMode(settingsManager.userOptedInForManualWidgetUpdates))
                                }
                                var updateInterval by remember { mutableStateOf(settingsManager.widgetManualUpdateMinutes.toString()) }
                                var lowPowerMode by remember { mutableStateOf(settingsManager.userOptedInForManualWidgetUpdatesInLowPower) }

                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { expanded = !expanded }
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SettingsRow(
                                            icon = item.icon,
                                            iconColor = item.iconColor,
                                            text = item.text,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Icon(
                                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (expanded) "Collapse" else "Expand",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = expanded,
                                        enter = expandVertically(),
                                        exit = shrinkVertically()
                                    ) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    FilterChip(
                                                        onClick = {
                                                            widgetUpdateMode = WidgetUpdateMode(false)
                                                            settingsManager.userOptedInForManualWidgetUpdates = false
                                                        },
                                                        label = {
                                                            Text(
                                                                "Auto",
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth(),
                                                                textAlign = TextAlign.Center
                                                            )
                                                        },
                                                        selected = !widgetUpdateMode.isManual,
                                                        modifier = Modifier.weight(1f).height(48.dp)
                                                    )
                                                    FilterChip(
                                                        onClick = {
                                                            widgetUpdateMode = WidgetUpdateMode(true)
                                                            settingsManager.userOptedInForManualWidgetUpdates = true
                                                        },
                                                        label = {
                                                            Text(
                                                                "Manual",
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth(),
                                                                textAlign = TextAlign.Center
                                                            )
                                                        },
                                                        selected = widgetUpdateMode.isManual,
                                                        modifier = Modifier.weight(1f).height(48.dp)
                                                    )
                                                }

                                                if (!widgetUpdateMode.isManual) {
                                                    Text(
                                                        text = "Widget would update every 15 minutes automatically.",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                AnimatedVisibility(
                                                    visible = widgetUpdateMode.isManual,
                                                    enter = expandVertically(),
                                                    exit = shrinkVertically()
                                                ) {
                                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                                        Column {
                                                            Text(
                                                                text = "Enter how often you want the widget to update in minutes:",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                modifier = Modifier.padding(bottom = 8.dp)
                                                            )
                                                            OutlinedTextField(
                                                                value = updateInterval,
                                                                onValueChange = { newValue ->
                                                                    if (newValue.isEmpty() || newValue.all { it.isDigit() })  {
                                                                        updateInterval = newValue
                                                                        if (newValue.isNotEmpty()) {
                                                                            newValue.toIntOrNull()?.let { intValue ->
                                                                                if (intValue > 0) {
                                                                                    settingsManager.widgetManualUpdateMinutes = intValue
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                },
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .onFocusChanged { focusState ->
                                                                        if (!focusState.isFocused && updateInterval.isEmpty()) {
                                                                            updateInterval = settingsManager.widgetManualUpdateMinutes.toString()
                                                                        }
                                                                    },
                                                                singleLine = true,
                                                                placeholder = { Text("Enter minutes") },
                                                                keyboardActions = KeyboardActions(onDone = {
                                                                    keyboardController?.hide()
                                                                    focusManager.clearFocus()
                                                                }),
                                                            )
                                                        }

                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "Manual Update Even In Low Power Mode",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                modifier = Modifier.weight(1f).padding(end = 16.dp)
                                                            )
                                                            Switch(
                                                                checked = lowPowerMode,
                                                                onCheckedChange = {
                                                                    lowPowerMode = it
                                                                    settingsManager.userOptedInForManualWidgetUpdatesInLowPower = it
                                                                }
                                                            )
                                                        }

                                                        val intervalMinutes = updateInterval.toIntOrNull() ?: 0
                                                        if (lowPowerMode && intervalMinutes < 5 && intervalMinutes > 0) {
                                                            Text(
                                                                text = "Careful. This setting might drain your battery over time",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.inversePrimary,
                                                                fontWeight = FontWeight.Normal
                                                            )
                                                        } else if (!lowPowerMode) {
                                                            Text(
                                                                text = "Widget would update every 15 minutes when in low power",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }

                                                Button(
                                                    onClick = { expanded = false },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary,
                                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                ) {
                                                    Text("Done")
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            when (item.action) {
                                                SettingsAction.ThemeSelection -> if (item.text.contains("Theme")) onNavigateToThemeSelection()
                                                SettingsAction.About -> onNavigateToAbout()
                                                SettingsAction.Credits -> onNavigateToCredits()
                                                SettingsAction.TermsAndPrivacy -> onNavigateToTermsAndPrivacy()
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SettingsRow(
                                        icon = item.icon,
                                        iconColor = item.iconColor,
                                        text = item.text,
                                        modifier = Modifier.weight(1f)
                                    )

                                    item.endContent?.invoke()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
