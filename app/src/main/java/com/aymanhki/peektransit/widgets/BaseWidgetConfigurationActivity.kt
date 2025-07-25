package com.aymanhki.peektransit.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aymanhki.peektransit.utils.PeekTransitConstants


abstract class BaseWidgetConfigurationActivity : ComponentActivity() {
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    protected abstract val widgetSize: String
    protected abstract val configurationTitle: String

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
            WidgetConfigurationScreen(
                title = configurationTitle,
                widgetsToShow = PeekTransitConstants.getSavedWidgetsForTargetSize(this, widgetSize),
                onWidgetSelected = { widget ->
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
                },
                onCloseWidgetConfigurationScreen = {
                    val resultValue = Intent().apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    setResult(RESULT_CANCELED, resultValue)
                    finish()
                }
            )
        }
    }
}
