package com.aymanhki.peektransit.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.aymanhki.peektransit.utils.PeekTransitConstants

abstract class BaseWidgetProvider : AppWidgetProvider() {

    protected abstract val mainLayoutFrameResId: Int
    protected abstract val backgroundImageResId: Int
    protected abstract val mainLayoutResId: Int
    protected abstract val mainLayoutContainerResId: Int
    protected abstract val locationCoordinatesLayoutResId: Int
    protected abstract val locationCoordinatesTextImagedResId: Int
    protected abstract val lastUpdatedLayoutResId: Int
    protected abstract val lastUpdatedTextImageResId: Int
    protected abstract val initialLayoutResId: Int
    protected abstract val configureButtonResId: Int
    protected abstract val errorLayoutResId: Int
    protected abstract val errorTextResId: Int
    protected abstract val configurationActivityClass: Class<*>
    protected open val logTag: String = "BaseWidgetProvider"
    protected abstract val busSchedulesComponentsResIds: Map<Int, Map<Pair<Int, Int>, Map<Int, List<Int>>>>
    protected abstract val refreshBackgroundFrameResId: Int
    protected abstract val refreshButtonResId: Int
    protected abstract val loadingIndicatorResId: Int

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {

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
        WidgetLocationManager.cleanup()
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        if (context == null || appWidgetIds == null) return

        PeekTransitConstants.removeDeletedWidgetInstancesData(context, appWidgetIds)

        if (!PeekTransitConstants.isThereActiveWidgetsWithLocationAccessNeeded(context)) {
            WidgetLocationManager.cleanup()
        }

    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action

        if (action in PeekTransitConstants.updateActions) {
            PeekTransitConstants.triggerWidgetCoreUpdatesManagerWithUserSettings(context, true, action != PeekTransitConstants.ACTION_MANUAL_REFRESH_WIDGET)
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

        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val dimensions = getWidgetDimensions(options)

        val finalView = CentralWidgetLooksUpdateManager.getFinalWidgetLook(
            context,
            widgetConfig,
            appWidgetId,
            mainLayoutResId,
            mainLayoutContainerResId,
            initialLayoutResId,
            configureButtonResId,
            errorLayoutResId,
            errorTextResId,
            configurationActivityClass,
            busSchedulesComponentsResIds,
            locationCoordinatesLayoutResId,
            locationCoordinatesTextImagedResId,
            lastUpdatedLayoutResId,
            lastUpdatedTextImageResId,
            dimensions.first,
            dimensions.second,
            mainLayoutFrameResId,
            backgroundImageResId,
            refreshBackgroundFrameResId,
            refreshButtonResId,
            loadingIndicatorResId
        )
        appWidgetManager.updateAppWidget(appWidgetId, finalView)
    }

    private fun getWidgetDimensions(options: Bundle): Pair<Int, Int> {
        val portraitWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val portraitHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
        val landscapeWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
        val landscapeHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)

        val maxWidth = minOf(portraitWidth, landscapeWidth)
        val maxHeight = minOf(portraitHeight, landscapeHeight)


        return Pair(maxWidth, maxHeight)
    }
}
