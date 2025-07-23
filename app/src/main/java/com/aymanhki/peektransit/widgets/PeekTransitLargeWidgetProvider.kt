package com.aymanhki.peektransit.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.aymanhki.peektransit.R
import com.aymanhki.peektransit.managers.SettingsManager
import android.content.res.Configuration
import android.util.Log
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.PeekTransitConstants.getWidgetBackgroundColor
import com.aymanhki.peektransit.utils.PeekTransitConstants.getWidgetTextColor
import com.aymanhki.peektransit.utils.PeekTransitConstants.getWidgetTextFont
import com.aymanhki.peektransit.workers.WidgetUpdateManager
import generateTextBitmap
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PeekTransitLargeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        if (context == null) return
        PeekTransitConstants.initAPIKey(context)
        PeekTransitConstants.startWidgetUpdateManagerWithUserSettings(context)
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        if (context == null) return
        WidgetUpdateManager.stopUpdates(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        Log.d("WidgetProvider", "Received action: $action")

        val updateActions = listOf(
            Intent.ACTION_CONFIGURATION_CHANGED,
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_SCREEN_ON,
            Intent.ACTION_SCREEN_OFF,
        )

        if (action in updateActions) {
            PeekTransitConstants.triggerAllWidgetsUpdates(context)
        } else if (action == Intent.ACTION_BOOT_COMPLETED) {
            PeekTransitConstants.startWidgetUpdateManagerWithUserSettings(context)
        }
    }

    fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val widgetConfig = PeekTransitConstants.getWidgetConfigUsingAppWidgetId(context, appWidgetId)
        val widgetSize = widgetConfig?.widgetData["size"] as? String ?: "large"
        val showLastUpdatedStatus = widgetConfig?.widgetData["showLastUpdatedStatus"] as? Boolean ?: false
        val views: RemoteViews

        if (widgetConfig != null) {
            views = RemoteViews(context.packageName, R.layout.peek_transit_large_layout)
            val settingsManager = SettingsManager.getInstance(context)
            val currentTheme = settingsManager.stopViewTheme
            val isNightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            views.setInt(R.id.peek_transit_large_layout, "setBackgroundColor", getWidgetBackgroundColor(currentTheme, isNightMode))

            //views.setFloat(R.id.peek_transit_large_layout, "setWeightSum", 3f)

            val lastUpdatedString = "Last updated: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))}"

            if (showLastUpdatedStatus) {
                views.setImageViewBitmap(R.id.last_updated_status_text_image, generateTextBitmap(
                    context,
                    getWidgetTextFont(currentTheme),
                    null,
                    1,
                    PeekTransitConstants.getLastSeenFontSizeForWidgetSize(widgetSize),
                    getWidgetTextColor(currentTheme, isNightMode),
                    lastUpdatedString
                ))
            }
        } else {
            views = RemoteViews(context.packageName, R.layout.peek_transit_large_initial_layout)

            val configIntent = Intent(
                context,
                PeekTransitLargeWidgetConfigurationActivity::class.java
            ).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val configPendingIntent = PendingIntent.getActivity(context, appWidgetId,
                configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.configure_large_widget_btn, configPendingIntent)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}


