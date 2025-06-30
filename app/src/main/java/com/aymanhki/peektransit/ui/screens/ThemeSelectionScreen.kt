package com.aymanhki.peektransit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aymanhki.peektransit.managers.SettingsManager
import com.aymanhki.peektransit.ui.components.CustomTopAppBar
import com.aymanhki.peektransit.ui.components.ThemePreviewCard
import com.aymanhki.peektransit.utils.StopViewTheme

@Composable
fun ThemeSelectionScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    var selectedTheme by remember { mutableStateOf(settingsManager.stopViewTheme) }
    
    Scaffold(
        topBar = {
            CustomTopAppBar(
                title = { 
                    Text(
                        text = "Stop View Theme",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Select a Theme",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            StopViewTheme.values().forEach { theme ->
                ThemePreviewCard(
                    theme = theme,
                    isSelected = selectedTheme == theme,
                    onThemeSelected = { newTheme ->
                        selectedTheme = newTheme
                        settingsManager.stopViewTheme = newTheme
                    }
                )
            }
        }
    }
} 