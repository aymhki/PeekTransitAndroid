package com.aymanhki.peektransit.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.aymanhki.peektransit.utils.PeekTransitConstants

abstract class BaseWidgetProvider : AppWidgetProvider() {

    protected abstract val mainLayoutResId: Int
    protected abstract val initialLayoutResId: Int
    protected abstract val configureButtonResId: Int
    protected abstract val errorLayoutResId: Int
    protected abstract val errorTextResId: Int
    protected abstract val configurationActivityClass: Class<*>
    protected open val logTag: String = "BaseWidgetProvider"

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d(logTag, "onUpdate called for ${this.javaClass.simpleName}")

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
        Log.d(logTag, "Received action: $action")

        if (action in PeekTransitConstants.updateActions) {
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
        val finalView = CentralWidgetLooksUpdateManager.getFinalWidgetLook(
            context,
            widgetConfig,
            appWidgetId,
            mainLayoutResId,
            initialLayoutResId,
            configureButtonResId,
            errorLayoutResId,
            errorTextResId,
            configurationActivityClass
        )
        appWidgetManager.updateAppWidget(appWidgetId, finalView)
    }
}
