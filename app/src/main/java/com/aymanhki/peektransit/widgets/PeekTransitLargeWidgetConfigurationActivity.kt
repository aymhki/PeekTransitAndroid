package com.aymanhki.peektransit.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.aymanhki.peektransit.data.models.WidgetModel
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
            WidgetConfigurationScreen(
                title = "Configure Large Widget",
                widgetsToShow = getSavedWidgetsForTargetSize(this, "large"),
                onWidgetSelected = { widget ->
                    PeekTransitConstants.saveWidgetSelection(this, appWidgetId, widget)

                    val resultValue = Intent().apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }

                    val appWidgetManager = AppWidgetManager.getInstance(this)
                    PeekTransitLargeWidgetProvider().updateAppWidget(this, appWidgetManager, appWidgetId)

                    setResult(RESULT_OK, resultValue)
                    finish()
                }
            )
        }
    }
}



