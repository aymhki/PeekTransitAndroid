package com.aymanhki.peektransit.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.PeekTransitConstants.getSavedWidgetsForTargetSize


class PeekTransitLargeWidgetConfigurationActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {

        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        super.onCreate(savedInstanceState)
        setContent {
            enableEdgeToEdge()
            WidgetConfigurationScreen(
                title = "Configure Large Widget",
                widgetsToShow = getSavedWidgetsForTargetSize(this, "large"),
                onWidgetSelected = { widget ->
                    PeekTransitConstants.saveWidgetSelection(this, appWidgetId, widget)

                    val resultValue = Intent().apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }

                    PeekTransitConstants.triggerWidgetCoreUpdatesManagerWithUserSettings(this, true, false)
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



