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
                                DefaultTab.values().forEach { tab ->
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