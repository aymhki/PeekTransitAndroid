package com.aymanhki.peektransit.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.FontRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import com.aymanhki.peektransit.data.models.WidgetModel
import com.aymanhki.peektransit.managers.SettingsManager
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.PeekTransitConstants.getWidgetBackgroundColor
import com.aymanhki.peektransit.utils.PeekTransitConstants.getWidgetTextColor
import com.aymanhki.peektransit.utils.PeekTransitConstants.getWidgetTextFont
import com.aymanhki.peektransit.utils.StopViewTheme

class CentralWidgetLooksUpdateManager
{

    companion object {
        fun getFinalWidgetLook(
            context: Context,
            widgetConfig: WidgetModel?,
            appWidgetId: Int,
            layoutId: Int,
            mainLayoutContainerResId: Int,
            initialLayoutId: Int,
            configureButtonId: Int,
            errorLayoutId: Int,
            errorTextId: Int,
            configureActivity: Class<*>,
            busSchedulesComponentsResIds: Map<Int, Map<Pair<Int, Int>, Map<Int, List<Int>>>>,
            locationCoordinatesLayoutResId: Int,
            locationCoordinatesTextImagedResId: Int,
            lastUpdatedLayoutResId: Int,
            lastUpdatedTextImageResId: Int
        ): RemoteViews {
            var views: RemoteViews

            if (widgetConfig != null) {
                val widgetScheduleData = PeekTransitConstants.getWidgetSchedule(context, appWidgetId.toString(), widgetConfig.id)
                val errorMsg = widgetScheduleData?.errorMsg ?: ""

                if (errorMsg.isNotEmpty()) {
                    views = RemoteViews(context.packageName, errorLayoutId)
                    views.setTextViewText(errorTextId, errorMsg)
                } else {
                    views = RemoteViews(context.packageName, layoutId)
                    views = updateWidgetLooks(context, views, appWidgetId, widgetConfig, layoutId, mainLayoutContainerResId, busSchedulesComponentsResIds, locationCoordinatesLayoutResId, locationCoordinatesTextImagedResId, lastUpdatedLayoutResId, lastUpdatedTextImageResId, widgetScheduleData)
                }
            } else {
                views = RemoteViews(context.packageName, initialLayoutId)
                views = updateWidgetLooksIfNoConfig(
                    context,
                    views,
                    appWidgetId,
                    configureActivity,
                    configureButtonId
                )
            }

            return views
        }

        fun updateWidgetLooks(
            context: Context,
            views: RemoteViews,
            appWidgetId: Int,
            widgetConfig: WidgetModel,
            layoutId: Int,
            mainLayoutContainerResId: Int,
            busSchedulesComponentsResIds: Map<Int, Map<Pair<Int, Int>, Map<Int, List<Int>>>>,
            locationCoordinatesLayoutResId: Int,
            locationCoordinatesTextImagedResId: Int,
            lastUpdatedLayoutResId: Int,
            lastUpdatedTextImageResId: Int,
            widgetScheduleData: WidgetSchedule?
        ): RemoteViews {
            val widgetSize = widgetConfig.widgetData["size"] as? String ?: "medium"
            val showLastUpdatedStatus = widgetConfig.widgetData["showLastUpdatedStatus"] as? Boolean ?: false
            val lastUpdatedTimeString = widgetScheduleData?.lastUpdatedTime ?: ""
            val locationCoordsString = "${widgetScheduleData?.userLocationLat ?: ""}, ${widgetScheduleData?.userLocationLon ?: ""}"
            val settingsManager = SettingsManager.getInstance(context)
            val currentTheme = settingsManager.stopViewTheme
            val isNightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            updateBackgroundColor(views, currentTheme, isNightMode, mainLayoutContainerResId)

            updateLocationCoordinates(
                context,
                views,
                widgetSize,
                isNightMode,
                currentTheme,
                locationCoordsString,
                locationCoordinatesLayoutResId,
                locationCoordinatesTextImagedResId,
                widgetConfig
            )

            updateWidgetSchedules(
                context,
                views,
                widgetConfig,
                widgetScheduleData,
                busSchedulesComponentsResIds,
                isNightMode,
                currentTheme,
                widgetSize
            )

            updateWidgetLastUpdated(
                context,
                views,
                widgetSize,
                isNightMode,
                currentTheme,
                lastUpdatedTimeString,
                showLastUpdatedStatus,
                lastUpdatedLayoutResId,
                lastUpdatedTextImageResId
            )

            return views
        }

        fun updateWidgetLooksIfNoConfig(
            context: Context,
            views: RemoteViews,
            appWidgetId: Int,
            configureActivity: Class<*>,
            buttonId: Int
        ): RemoteViews {
            val configIntent = Intent(context, configureActivity)
            .apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val configPendingIntent = PendingIntent.getActivity(context, appWidgetId,
                configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(buttonId, configPendingIntent)

            return views
        }

        fun updateBackgroundColor(
            views: RemoteViews,
            currentTheme: StopViewTheme,
            isNightMode: Boolean,
            layoutId: Int
        ): RemoteViews {
            views.setInt(layoutId, "setBackgroundColor", getWidgetBackgroundColor(currentTheme, isNightMode))

            return views
        }

        fun updateWidgetLastUpdated(
            context: Context,
            views: RemoteViews,
            widgetSize: String,
            isNightMode: Boolean,
            currentTheme: StopViewTheme,
            lastUpdatedTimeString: String,
            shouldShowLastUpdatedTextImage: Boolean,
            lastUpdatedLayoutResId: Int,
            lastUpdatedTextImageResId: Int
        ): RemoteViews {
            return if (shouldShowLastUpdatedTextImage) {
                updateWidgetLastUpdatedTextImage(
                    context,
                    views,
                    widgetSize,
                    isNightMode,
                    currentTheme,
                    lastUpdatedTimeString,
                    lastUpdatedLayoutResId,
                    lastUpdatedTextImageResId
                )
            } else {
                resetLastUpdatedTextImage(views, lastUpdatedLayoutResId, lastUpdatedTextImageResId)
            }
        }


        fun resetLastUpdatedTextImage(
            views: RemoteViews,
            lastUpdatedLayoutResId: Int,
            lastUpdatedTextImageResId: Int
        ): RemoteViews {
            views.setViewVisibility(lastUpdatedLayoutResId, GONE)
            views.setViewVisibility(lastUpdatedTextImageResId, GONE)
            views.setContentDescription(lastUpdatedLayoutResId, "Last updated status text image")
            return views
        }

        fun updateWidgetLastUpdatedTextImage(
            context: Context,
            views: RemoteViews,
            widgetSize: String,
            isNightMode: Boolean,
            currentTheme: StopViewTheme,
            lastUpdatedTimeString: String,
            lastUpdatedLayoutResId: Int,
            lastUpdatedTextImageResId: Int
        ): RemoteViews {
            views.setViewVisibility(lastUpdatedLayoutResId, VISIBLE)
            views.setViewVisibility(lastUpdatedTextImageResId, VISIBLE)

            var lastUpdatedString: String

            if (widgetSize == "lockscreen" || widgetSize == "small") {
                lastUpdatedString = "Updated: $lastUpdatedTimeString"
            } else {
                lastUpdatedString = "Last updated: $lastUpdatedTimeString"
            }



            views.setImageViewBitmap(
                lastUpdatedTextImageResId, generateTextBitmap(
                    context,
                    getWidgetTextFont(currentTheme),
                    null,
                    1,
                    PeekTransitConstants.getLastUpdatedFontSizeForWidget(widgetSize),
                    getWidgetTextColor(currentTheme, isNightMode),
                    lastUpdatedString
                )
            )

            views.setContentDescription(lastUpdatedTextImageResId, lastUpdatedString)

            return views
        }

        fun updateLocationCoordinates(
            context: Context,
            views: RemoteViews,
            widgetSize: String,
            isNightMode: Boolean,
            currentTheme: StopViewTheme,
            locationCoordsString: String,
            locationCoordinatesLayoutResId: Int,
            locationCoordinatesTextImagedResId: Int,
            widgetConfig: WidgetModel
        ): RemoteViews {
            return if (PeekTransitConstants.DEBUG_WIDGET_LOCATION_ACCESS && widgetSize != "lockscreen" && widgetSize != "small" && widgetConfig.widgetData["isClosestStop"] as? Boolean ?: false) {
                updateLocationCoordinatesTextImage(
                    context,
                    views,
                    widgetSize,
                    isNightMode,
                    currentTheme,
                    locationCoordsString,
                    locationCoordinatesLayoutResId,
                    locationCoordinatesTextImagedResId
                )
            } else {
                resetLocationCoordinatesTextImage(views, locationCoordinatesLayoutResId, locationCoordinatesTextImagedResId)
            }
        }

        fun resetLocationCoordinatesTextImage(
            views: RemoteViews,
            locationCoordinatesLayoutResId: Int,
            locationCoordinatesTextImagedResId: Int,
        ): RemoteViews {
            views.setViewVisibility(locationCoordinatesLayoutResId, GONE)
            views.setViewVisibility(locationCoordinatesTextImagedResId, GONE)
            views.setContentDescription(locationCoordinatesTextImagedResId, "User location coordinates text image")
            return views
        }

        fun updateLocationCoordinatesTextImage(
            context: Context,
            views: RemoteViews,
            widgetSize: String,
            isNightMode: Boolean,
            currentTheme: StopViewTheme,
            locationCoordsString: String,
            locationCoordinatesLayoutResId: Int,
            locationCoordinatesTextImagedResId: Int,
        ): RemoteViews {
            views.setViewVisibility(locationCoordinatesLayoutResId, VISIBLE)
            views.setViewVisibility(locationCoordinatesTextImagedResId, VISIBLE)

            views.setImageViewBitmap(
                locationCoordinatesTextImagedResId, generateTextBitmap(
                    context,
                    getWidgetTextFont(currentTheme),
                    null,
                    1,
                    PeekTransitConstants.getLocationCoordinatesTextSizeForWidget(widgetSize),
                    getWidgetTextColor(currentTheme, isNightMode),
                    locationCoordsString
                )
            )

            views.setContentDescription(
                locationCoordinatesTextImagedResId,
                locationCoordsString
            )

            return views
        }

        fun updateWidgetSchedules(
            context: Context,
            views: RemoteViews,
            widgetConfig: WidgetModel,
            widgetScheduleData: WidgetSchedule?,
            busSchedulesComponentsResIds: Map<Int, Map<Pair<Int, Int>, Map<Int, List<Int>>>>,
            isNightMode: Boolean,
            currentTheme: StopViewTheme,
            widgetSize: String
        ): RemoteViews {
            if (widgetScheduleData != null) {
                val widgetSchedules = widgetScheduleData.scheduleData

                val isCompactMode = (widgetSize == "small" && widgetConfig.widgetData["multipleEntriesPerVariant"] as? Boolean != true)

                if (isCompactMode) {
                    handleCompactMode(context, views, widgetSchedules, busSchedulesComponentsResIds,
                        isNightMode, currentTheme, widgetSize)
                } else {
                    handleNormalMode(context, views, widgetSchedules, busSchedulesComponentsResIds,
                        isNightMode, currentTheme, widgetSize, widgetConfig)
                }
            }
            return views
        }

        private fun handleCompactMode(
            context: Context,
            views: RemoteViews,
            widgetSchedules: Map<String, List<String>>,
            busSchedulesComponentsResIds: Map<Int, Map<Pair<Int, Int>, Map<Int, List<Int>>>>,
            isNightMode: Boolean,
            currentTheme: StopViewTheme,
            widgetSize: String
        ) {
            val firstBusScheduleComponentResIds = busSchedulesComponentsResIds.entries.firstOrNull()
            if (firstBusScheduleComponentResIds != null) {
                views.setViewVisibility(firstBusScheduleComponentResIds.key, VISIBLE)

                val busStopAndScheduleLayoutComponentResIds = firstBusScheduleComponentResIds.value
                val busStopLayoutComponentResIds = busStopAndScheduleLayoutComponentResIds.keys.firstOrNull()

                if (busStopLayoutComponentResIds != null) {
                    val stopTitleLayout = busStopLayoutComponentResIds.first
                    val stopTitleTextImage = busStopLayoutComponentResIds.second

                    views.setViewVisibility(stopTitleLayout, GONE)
                    views.setViewVisibility(stopTitleTextImage, GONE)

                    val busStopSchedules = busStopAndScheduleLayoutComponentResIds.values.firstOrNull()
                    if (busStopSchedules != null) {
                        val stopEntries = widgetSchedules.entries.take(2).toList()

                        var schedulesIndex = 0
                        for (busStopSchedulesIndex in busStopSchedules) {
                            val currentScheduleLayoutResId = busStopSchedulesIndex.key
                            val currentScheduleTextImagesResIds = busStopSchedulesIndex.value

                            val stopEntry = stopEntries.getOrNull(schedulesIndex)
                            val scheduleEntry = stopEntry?.value?.firstOrNull()

                            if (scheduleEntry != null && currentScheduleTextImagesResIds != null) {
                                views.setViewVisibility(currentScheduleLayoutResId, VISIBLE)

                                val scheduleComponents = scheduleEntry.split(PeekTransitConstants.SCHEDULE_STRING_SEPARATOR)

                                val routeNumberResId = currentScheduleTextImagesResIds[0]
                                val routeNumber = scheduleComponents.getOrNull(0) ?: "Unknown Route"
                                val routeNameResId = currentScheduleTextImagesResIds[1]
                                val routeName = scheduleComponents.getOrNull(1) ?: "Unknown Route Name"
                                val finalRouteName = routeName.take(1) + "."
                                val arrivalStatusResId = currentScheduleTextImagesResIds[2]
                                val arrivalStatus = scheduleComponents.getOrNull(2) ?: "Unknown Status"
                                val finalArrivalStatus = arrivalStatus.take(1) + "."
                                val arrivalTimeResId = currentScheduleTextImagesResIds[3]
                                val arrivalTime = scheduleComponents.getOrNull(3) ?: "Unknown Time"

                                views.setViewVisibility(routeNumberResId, VISIBLE)
                                views.setImageViewBitmap(
                                    routeNumberResId, generateTextBitmap(
                                        context,
                                        getWidgetTextFont(currentTheme),
                                        PeekTransitConstants.getRouteNumberWidthForWidget(widgetSize),
                                        1,
                                        PeekTransitConstants.getScheduleEntryFontSizeForWidget(widgetSize, currentTheme),
                                        getWidgetTextColor(currentTheme, isNightMode),
                                        routeNumber
                                    )
                                )
                                views.setContentDescription(routeNumberResId, "Route Number: $routeNumber")

                                views.setViewVisibility(routeNameResId, VISIBLE)
                                views.setImageViewBitmap(
                                    routeNameResId, generateTextBitmap(
                                        context,
                                        getWidgetTextFont(currentTheme),
                                        PeekTransitConstants.getRouteNameWidthForWidget(widgetSize),
                                        1,
                                        PeekTransitConstants.getScheduleEntryFontSizeForWidget(widgetSize, currentTheme),
                                        getWidgetTextColor(currentTheme, isNightMode),
                                        finalRouteName
                                    )
                                )
                                views.setContentDescription(routeNameResId, "Route Name: $routeName")

                                views.setViewVisibility(arrivalStatusResId, VISIBLE)
                                if (arrivalStatus == PeekTransitConstants.LATE_STATUS_TEXT ||
                                    arrivalStatus == PeekTransitConstants.EARLY_STATUS_TEXT ||
                                    arrivalStatus == PeekTransitConstants.CANCELLED_STATUS_TEXT) {
                                    views.setImageViewBitmap(
                                        arrivalStatusResId, generateTextBitmap(
                                            context,
                                            getWidgetTextFont(currentTheme),
                                            PeekTransitConstants.getArrivalStatusWidthForWidget(widgetSize),
                                            1,
                                            PeekTransitConstants.getScheduleEntryFontSizeForWidget(widgetSize, currentTheme),
                                            PeekTransitConstants.getWidgetStatusTextColor(arrivalStatus, currentTheme),
                                            finalArrivalStatus
                                        )
                                    )
                                    views.setContentDescription(arrivalStatusResId, "Arrival Status: $arrivalStatus")
                                } else {
                                    views.setImageViewBitmap(
                                        arrivalStatusResId, generateTextBitmap(
                                            context,
                                            getWidgetTextFont(currentTheme),
                                            PeekTransitConstants.getArrivalStatusWidthForWidget(widgetSize)/2,
                                            1,
                                            PeekTransitConstants.getScheduleEntryFontSizeForWidget(widgetSize, currentTheme),
                                            getWidgetTextColor(currentTheme, isNightMode),
                                            ""
                                        )
                                    )
                                    views.setContentDescription(arrivalStatusResId, "")
                                }

                                if (arrivalStatus != PeekTransitConstants.CANCELLED_STATUS_TEXT) {
                                    views.setViewVisibility(arrivalTimeResId, VISIBLE)
                                    views.setImageViewBitmap(
                                        arrivalTimeResId, generateTextBitmap(
                                            context,
                                            getWidgetTextFont(currentTheme),
                                            PeekTransitConstants.getArrivalTimeWidthForWidget(widgetSize),
                                            1,
                                            PeekTransitConstants.getScheduleEntryFontSizeForWidget(widgetSize, currentTheme),
                                            getWidgetTextColor(currentTheme, isNightMode),
                                            arrivalTime
                                        )
                                    )
                                    views.setContentDescription(arrivalTimeResId, "Arrival Time: $arrivalTime")
                                } else {
                                    views.setViewVisibility(arrivalTimeResId, GONE)
                                }
                            } else {
                                views.setViewVisibility(currentScheduleLayoutResId, GONE)
                            }

                            schedulesIndex++
                            if (schedulesIndex >= 2) break
                        }
                    }
                }

                busSchedulesComponentsResIds.entries.drop(1).forEach { entry ->
                    views.setViewVisibility(entry.key, GONE)
                }
            }
        }

        private fun handleNormalMode(
            context: Context,
            views: RemoteViews,
            widgetSchedules: Map<String, List<String>>,
            busSchedulesComponentsResIds: Map<Int, Map<Pair<Int, Int>, Map<Int, List<Int>>>>,
            isNightMode: Boolean,
            currentTheme: StopViewTheme,
            widgetSize: String,
            widgetConfig: WidgetModel
        ) {
            for ((index) in busSchedulesComponentsResIds.entries.withIndex()) {
                val currentWidgetScheduleEntry = widgetSchedules.entries.elementAtOrNull(index)
                val currentBusScheduleComponentResIds = busSchedulesComponentsResIds.entries.elementAtOrNull(index)
                if (currentWidgetScheduleEntry != null && currentBusScheduleComponentResIds != null) {
                    if (currentWidgetScheduleEntry != null) {
                        views.setViewVisibility(currentBusScheduleComponentResIds.key, VISIBLE)
                        val busStopKey = currentWidgetScheduleEntry.key.split(PeekTransitConstants.COMPOSITE_KEY_LINKER_FOR_DICTIONARIES)
                        var busStopTitle = busStopKey.getOrNull(0) ?: "Unknown Stop"
                        val busStopNumber = busStopKey.getOrNull(1) ?: "Unknown Number"
                        busStopTitle = if (busStopTitle.length > PeekTransitConstants.STOP_NAME_MAX_PREFIX_LENGTH_FOR_WIDGET) {
                            busStopTitle.substring(0, PeekTransitConstants.STOP_NAME_MAX_PREFIX_LENGTH_FOR_WIDGET)+"..."
                        } else {
                            busStopTitle
                        }
                        val busStopText = "${busStopTitle} - ${busStopNumber}"
                        val busStopAndScheduleLayoutComponentResIds = currentBusScheduleComponentResIds.value
                        val busStopLayoutComponentResIds = busStopAndScheduleLayoutComponentResIds.keys.elementAtOrNull(0)
                        if (busStopLayoutComponentResIds != null) {
                            val stopTitleLayout = busStopLayoutComponentResIds.first
                            val stopTitleTextImage = busStopLayoutComponentResIds.second
                            if ( (widgetSize != "lockscreen" && widgetSize != "small") || (widgetSize == "small" && widgetConfig.widgetData["multipleEntriesPerVariant"] as? Boolean == true) ) {
                                views.setViewVisibility(stopTitleLayout, VISIBLE)
                                views.setViewVisibility(stopTitleTextImage, VISIBLE)
                                views.setImageViewBitmap(
                                    stopTitleTextImage, generateTextBitmap(
                                        context,
                                        getWidgetTextFont(currentTheme),
                                        PeekTransitConstants.getStopTitleWidthForWidget(widgetSize),
                                        if (widgetSize == "small") 2 else 1,
                                        PeekTransitConstants.getStopTitleTextSizeForWidget(
                                            widgetSize, currentTheme
                                        ),
                                        getWidgetTextColor(currentTheme, isNightMode),
                                        busStopText
                                    )
                                )
                                views.setContentDescription(
                                    stopTitleTextImage,
                                    currentWidgetScheduleEntry.key
                                )
                            } else {
                                views.setViewVisibility(stopTitleLayout, GONE)
                                views.setViewVisibility(stopTitleTextImage, GONE)
                            }
                            val busStopSchedules = busStopAndScheduleLayoutComponentResIds.values.elementAtOrNull(0)
                            if (busStopSchedules != null) {
                                var schedulesIndex = 0
                                for (busStopSchedulesIndex in busStopSchedules) {
                                    val currentWidgetScheduleEntrySchedule = currentWidgetScheduleEntry.value.elementAtOrNull(schedulesIndex)
                                    val currentScheduleLayoutResId = busStopSchedulesIndex.key
                                    if (!currentWidgetScheduleEntrySchedule.isNullOrEmpty()) {
                                        views.setViewVisibility(currentScheduleLayoutResId, VISIBLE)
                                        val currentScheduleTextImagesResIds = busStopSchedulesIndex.value
                                        val scheduleComponents = currentWidgetScheduleEntrySchedule.split(PeekTransitConstants.SCHEDULE_STRING_SEPARATOR)
                                        if (currentScheduleTextImagesResIds != null) {
                                            val routeNumberResId = currentScheduleTextImagesResIds[0]
                                            val routeNumber = scheduleComponents.getOrNull(0) ?: "Unknown Route"
                                            val routeNameResId = currentScheduleTextImagesResIds[1]
                                            val routeName = scheduleComponents.getOrNull(1) ?: "Unknown Route Name"
                                            val finalRouteName = if (widgetSize == "lockscreen" || widgetSize == "small") {
                                                routeName.take(1) + "."
                                            } else {
                                                routeName
                                            }
                                            val arrivalStatusResId = currentScheduleTextImagesResIds[2]
                                            val arrivalStatus = scheduleComponents.getOrNull(2) ?: "Unknown Status"
                                            val finalArrivalStatus = if (widgetSize == "lockscreen" || widgetSize == "small") {
                                                arrivalStatus.take(1) + "."
                                            } else {
                                                arrivalStatus
                                            }
                                            val arrivalTimeResId = currentScheduleTextImagesResIds[3]
                                            val arrivalTime = scheduleComponents.getOrNull(3) ?: "Unknown Time"
                                            views.setViewVisibility(routeNumberResId, VISIBLE)
                                            views.setViewVisibility(routeNameResId, VISIBLE)
                                            views.setViewVisibility(arrivalStatusResId, VISIBLE)
                                            views.setImageViewBitmap(
                                                routeNumberResId, generateTextBitmap(
                                                    context,
                                                    getWidgetTextFont(currentTheme),
                                                    PeekTransitConstants.getRouteNumberWidthForWidget(widgetSize),
                                                    1,
                                                    PeekTransitConstants.getScheduleEntryFontSizeForWidget(widgetSize, currentTheme),
                                                    getWidgetTextColor(currentTheme, isNightMode),
                                                    routeNumber
                                                )
                                            )
                                            views.setContentDescription(
                                                routeNumberResId,
                                                "Route Number: $routeNumber"
                                            )
                                            views.setImageViewBitmap(
                                                routeNameResId, generateTextBitmap(
                                                    context,
                                                    getWidgetTextFont(currentTheme),
                                                    PeekTransitConstants.getRouteNameWidthForWidget(widgetSize),
                                                    1,
                                                    PeekTransitConstants.getScheduleEntryFontSizeForWidget(widgetSize, currentTheme),
                                                    getWidgetTextColor(currentTheme, isNightMode),
                                                    finalRouteName
                                                )
                                            )
                                            views.setContentDescription(
                                                routeNameResId,
                                                "Route Name: $routeName"
                                            )
                                            if (arrivalStatus == PeekTransitConstants.LATE_STATUS_TEXT || arrivalStatus == PeekTransitConstants.EARLY_STATUS_TEXT || arrivalStatus == PeekTransitConstants.CANCELLED_STATUS_TEXT) {
                                                views.setImageViewBitmap(
                                                    arrivalStatusResId, generateTextBitmap(
                                                        context,
                                                        getWidgetTextFont(currentTheme),
                                                        PeekTransitConstants.getArrivalStatusWidthForWidget(widgetSize),
                                                        1,
                                                        PeekTransitConstants.getScheduleEntryFontSizeForWidget(widgetSize, currentTheme),
                                                        PeekTransitConstants.getWidgetStatusTextColor(arrivalStatus, currentTheme),
                                                        finalArrivalStatus
                                                    )
                                                )
                                                views.setContentDescription(arrivalStatusResId, "Arrival Status: $arrivalStatus")
                                            } else if (widgetSize != "lockscreen" && widgetSize != "small") {
                                                views.setImageViewBitmap(
                                                    arrivalStatusResId, generateTextBitmap(
                                                        context,
                                                        getWidgetTextFont(currentTheme),
                                                        PeekTransitConstants.getArrivalStatusWidthForWidget(widgetSize),
                                                        1,
                                                        PeekTransitConstants.getScheduleEntryFontSizeForWidget(widgetSize, currentTheme),
                                                        getWidgetTextColor(currentTheme, isNightMode),
                                                        ""
                                                    )
                                                )
                                                views.setContentDescription(arrivalStatusResId, "")
                                            } else {
                                                views.setImageViewBitmap(
                                                    arrivalStatusResId, generateTextBitmap(
                                                        context,
                                                        getWidgetTextFont(currentTheme),
                                                        PeekTransitConstants.getArrivalStatusWidthForWidget(widgetSize)/2,
                                                        1,
                                                        PeekTransitConstants.getScheduleEntryFontSizeForWidget(widgetSize, currentTheme),
                                                        getWidgetTextColor(currentTheme, isNightMode),
                                                        ""
                                                    )
                                                )
                                                views.setContentDescription(arrivalStatusResId, "")
                                            }
                                            if (arrivalStatus != PeekTransitConstants.CANCELLED_STATUS_TEXT) {
                                                views.setViewVisibility(arrivalTimeResId, VISIBLE)
                                                views.setImageViewBitmap(
                                                    arrivalTimeResId, generateTextBitmap(
                                                        context,
                                                        getWidgetTextFont(currentTheme),
                                                        PeekTransitConstants.getArrivalTimeWidthForWidget(widgetSize),
                                                        1,
                                                        PeekTransitConstants.getScheduleEntryFontSizeForWidget(widgetSize, currentTheme),
                                                        getWidgetTextColor(currentTheme, isNightMode),
                                                        arrivalTime
                                                    )
                                                )
                                                views.setContentDescription(
                                                    arrivalTimeResId,
                                                    "Arrival Time: $arrivalTime"
                                                )
                                            } else {
                                                views.setViewVisibility(arrivalTimeResId, GONE)
                                            }
                                        }
                                    } else {
                                        views.setViewVisibility(currentScheduleLayoutResId, GONE)
                                    }
                                    schedulesIndex++
                                }
                            }
                        }
                    } else {
                        views.setViewVisibility(currentBusScheduleComponentResIds.key, GONE)
                    }
                }
            }
        }

    }
}

fun generateTextBitmap(
    context: Context,
    @FontRes fontResId: Int,
    maxImageWidthSize: Int?,
    maxLines: Int,
    fontSize: Float,
    @ColorInt fontColor: Int,
    text: String
): Bitmap? {
    val typeface = ResourcesCompat.getFont(context, fontResId) ?: return null
    val density = context.resources.displayMetrics.density

    val textPaint = TextPaint().apply {
        isAntiAlias = true
        this.typeface = typeface
        this.textSize = fontSize * density
        color = fontColor
    }

    val maxImageWidthInPixels = if (maxImageWidthSize != null) {
        (maxImageWidthSize * density).toInt()
    } else {
        textPaint.measureText(text).toInt()
    }.coerceAtLeast(1)

    val builder = StaticLayout.Builder.obtain(
        text, 0, text.length, textPaint, maxImageWidthInPixels
    ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setMaxLines(maxLines)

    if (maxImageWidthSize != null) {
        builder.setEllipsize(TextUtils.TruncateAt.END)
    }

    val staticLayout = builder.build()

    val bitmap = createBitmap(staticLayout.width, staticLayout.height)
    val canvas = Canvas(bitmap)

    staticLayout.draw(canvas)

    return bitmap
}

