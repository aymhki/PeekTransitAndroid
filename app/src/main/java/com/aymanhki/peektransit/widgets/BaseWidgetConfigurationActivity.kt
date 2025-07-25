package com.aymanhki.peektransit.widgets

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.aymanhki.peektransit.data.models.WidgetModel
import com.aymanhki.peektransit.utils.PeekTransitConstants

abstract class BaseWidgetConfigurationActivity : ComponentActivity() {
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    protected abstract val widgetSize: String
    protected abstract val configurationTitle: String

    private var pendingWidget: WidgetModel? = null
    private var showLocationPermissionDialog by mutableStateOf(false)
    private var showInsufficientPermissionDialog by mutableStateOf(false)

    private val backgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && hasBackgroundLocationPermission()) {
            pendingWidget?.let { widget ->
                finalizeWidgetConfiguration(widget)
            }
        } else {
            showInsufficientPermissionDialog = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        super.onCreate(savedInstanceState)
        setContent {
            enableEdgeToEdge()

            if (showLocationPermissionDialog) {
                LocationPermissionDialog(
                    onAllow = {
                        showLocationPermissionDialog = false
                        requestBackgroundLocationPermission()
                    },
                    onCancel = {
                        showLocationPermissionDialog = false
                        pendingWidget = null
                    }
                )
            }

            if (showInsufficientPermissionDialog) {
                InsufficientPermissionDialog(
                    onGoToSettings = {
                        showInsufficientPermissionDialog = false
                        openAppSettings()
                    },
                    onCancel = {
                        showInsufficientPermissionDialog = false
                        pendingWidget = null
                    }
                )
            }

            WidgetConfigurationScreen(
                title = configurationTitle,
                widgetsToShow = PeekTransitConstants.getSavedWidgetsForTargetSize(this@BaseWidgetConfigurationActivity, widgetSize),
                onWidgetSelected = { widget ->
                    handleWidgetSelection(widget)
                },
                onCloseWidgetConfigurationScreen = {
                    val resultValue = Intent().apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    setResult(RESULT_CANCELED, resultValue)
                    finish()
                },
                widgetSize = widgetSize
            )
        }
    }

    private fun handleWidgetSelection(widget: WidgetModel) {
        val isClosestStop = widget.widgetData["isClosestStop"] as? Boolean ?: false

        if (isClosestStop) {
            if (hasBackgroundLocationPermission()) {
                finalizeWidgetConfiguration(widget)
            } else {
                pendingWidget = widget
                showLocationPermissionDialog = true
            }
        } else {
            finalizeWidgetConfiguration(widget)
        }
    }

    private fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        } else {
            backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun finalizeWidgetConfiguration(widget: WidgetModel) {
        PeekTransitConstants.saveWidgetSelection(this, appWidgetId, widget)
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        PeekTransitConstants.triggerWidgetCoreUpdatesManagerWithUserSettings(
            this,
            true,
            false
        )
        setResult(RESULT_OK, resultValue)
        finish()
    }

    override fun onResume() {
        super.onResume()
        pendingWidget?.let { widget ->
            if (hasBackgroundLocationPermission()) {
                finalizeWidgetConfiguration(widget)
                pendingWidget = null
            } else {
                showInsufficientPermissionDialog = true
            }
        }
    }
}

@Composable
private fun LocationPermissionDialog(
    onAllow: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text("Background Location Access Required")
        },
        text = {
            Text(
                "This widget configuration requires background location access at all times 'Allow Always' to properly update the nearby bus stops and the schedules displayed automatically. Peek Transit does not collect, store, or share your location data. Allow the Peek Transit to have access to your background location at all times?"
            )
        },
        confirmButton = {
            TextButton(onClick = onAllow) {
                Text("Allow")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun InsufficientPermissionDialog(
    onGoToSettings: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text("Insufficient Location Permission")
        },
        text = {
            Text(
                "The current location permission level will work inside the app but is insufficient for the widget. Please go to app settings and select 'Allow all the time' for location access to use this widget configuration. You could also select a different widget configuration that does not require background location access."
            )
        },
        confirmButton = {
            TextButton(onClick = onGoToSettings) {
                Text("Go to Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}
