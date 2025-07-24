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
import android.os.Bundle
import android.util.Log
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.PeekTransitConstants.getWidgetBackgroundColor
import com.aymanhki.peektransit.utils.PeekTransitConstants.getWidgetTextColor
import com.aymanhki.peektransit.utils.PeekTransitConstants.getWidgetTextFont
import generateTextBitmap
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PeekTransitLargeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // PeekTransitConstants.triggerWidgetCoreUpdatesManagerWithUserSettings(context, false, true)

        Log.d("WidgetProvider", "onUpdate called for PeekTransitLargeWidgetProvider")

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        if (context == null) return
        PeekTransitConstants.triggerWidgetCoreUpdatesManagerWithUserSettings(context, false, false)
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
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_SCREEN_ON,
            Intent.ACTION_SCREEN_OFF,
            Intent.ACTION_BOOT_COMPLETED,
        )

        if (action in updateActions) {
            PeekTransitConstants.triggerWidgetCoreUpdatesManagerWithUserSettings(context, true, false)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        if (context == null || appWidgetManager == null) return
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val widgetConfig = PeekTransitConstants.getWidgetConfigUsingAppWidgetId(context, appWidgetId)
        val widgetSize = widgetConfig?.widgetData["size"] as? String ?: "large"
        val showLastUpdatedStatus = widgetConfig?.widgetData["showLastUpdatedStatus"] as? Boolean ?: false
        val widgetScheduleData = PeekTransitConstants.getWidgetSchedule(context, appWidgetId.toString(), widgetConfig?.id)
        val lastUpdatedTimeString = widgetScheduleData?.lastUpdatedTime ?: LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
        val locationCoordsString = "${widgetScheduleData?.userLocationLat ?: ""}, ${widgetScheduleData?.userLocationLon ?: ""}"

        val views: RemoteViews

        if (widgetConfig != null) {
            views = RemoteViews(context.packageName, R.layout.peek_transit_large_layout)
            val settingsManager = SettingsManager.getInstance(context)
            val currentTheme = settingsManager.stopViewTheme
            val isNightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            views.setInt(R.id.peek_transit_large_layout, "setBackgroundColor", getWidgetBackgroundColor(currentTheme, isNightMode))

            //views.setFloat(R.id.peek_transit_large_layout, "setWeightSum", 3f)

            views.setImageViewBitmap(R.id.peek_transit_large_widget_user_location_coordinates_text_image, generateTextBitmap(
                context,
                getWidgetTextFont(currentTheme),
                null,
                1,
                15f,
                getWidgetTextColor(currentTheme, isNightMode),
                locationCoordsString
            ))

            views.setContentDescription(R.id.peek_transit_large_widget_user_location_coordinates_text_image, locationCoordsString)


            val lastUpdatedString = "Last updated: $lastUpdatedTimeString"

            if (showLastUpdatedStatus) {
                views.setImageViewBitmap(R.id.peek_transit_large_widget_last_updated_status_text_image, generateTextBitmap(
                    context,
                    getWidgetTextFont(currentTheme),
                    null,
                    1,
                    PeekTransitConstants.getLastSeenFontSizeForWidgetSize(widgetSize),
                    getWidgetTextColor(currentTheme, isNightMode),
                    lastUpdatedString
                ))

                views.setContentDescription(R.id.peek_transit_large_widget_last_updated_status_text_image, lastUpdatedString)
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


