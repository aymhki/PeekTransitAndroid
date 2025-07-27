package com.aymanhki.peektransit.utils

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.PowerManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.aymanhki.peektransit.ui.theme.AccentBlue
import com.aymanhki.peektransit.R
import com.aymanhki.peektransit.data.models.WidgetModel
import com.aymanhki.peektransit.managers.SavedWidgetsManager
import com.aymanhki.peektransit.managers.SettingsManager
import com.aymanhki.peektransit.widgets.PeekTransitLargeWidgetProvider
import com.aymanhki.peektransit.widgets.PeekTransitLockScreenWidgetProvider
import com.aymanhki.peektransit.widgets.PeekTransitMediumWidgetProvider
import com.aymanhki.peektransit.widgets.PeekTransitSmallWidgetProvider
import com.aymanhki.peektransit.widgets.SavedWidgetSchedulesManager
import com.aymanhki.peektransit.widgets.WidgetSchedule
import com.aymanhki.peektransit.widgets.WidgetUpdateManager
import kotlin.math.roundToInt

object PeekTransitConstants {
    const val DEBUG_MODE = false
    const val DEBUG_WIDGET_LOCATION_ACCESS = true
    const val HOW_OFTEN_TO_UPDATE_WIDGET_IN_DEBUG_MODE_IN_MINUTES_BY_DEFAULT = 5
    const val MAXIMUM_WIDGET_UPDATE_WORKER_INTERVAL_IN_MINUTES = 15L
    const val FLEXIABLE_WIDGET_UPDATE_WORKER_INTERVAL_IN_MINUTES = 5L
    var TRANSIT_API_KEY: String = ""
    const val BASE_URL = "https://api.winnipegtransit.com/v4/"
    const val STOPS_DISTANCE_RADIUS_IN_METERS = 1000.0
    const val MAX_STOPS_ALLOWED_TO_FETCH = 25
    const val MAX_STOPS_ALLOWED_TO_FETCH_FOR_SEARCH = 15
    const val DISTANCE_CHANGE_ALLOWED_BEFORE_REFRESHING_STOPS_IN_METERS = STOPS_DISTANCE_RADIUS_IN_METERS/3
    const val TIME_PERIOD_ALLOWED_FOR_NEXT_BUS_ROUTES_IN_HOURS = 12
    const val PERIOD_BEFORE_SHOWING_MINUTES_UNTIL_NEXT_BUS_IN_MINUTES = 15
    const val MINUTES_ALLOWED_TO_KEEP_DUE_BUSES_IN_SCHEDULE = 1
    const val SEARCH_DEBOUNCE_DELAY_MS = 1500L
    const val SCHEDULE_STRING_SEPARATOR = " ---- "
    const val COMPOSITE_KEY_LINKER_FOR_DICTIONARIES = "-"
    const val WIDGET_TEXT_PLACEHOLDER = "TBD"
    const val LATE_STATUS_TEXT = "Late"
    const val EARLY_STATUS_TEXT = "Early"
    const val CANCELLED_STATUS_TEXT = "Cancelled"
    const val OK_STATUS_TEXT = "Ok"
    const val DUE_STATUS_TEXT = "Due"
    const val MINUTES_REMAINING_TEXT = "min."
    const val MINUTES_PASSED_TEXT = "min. ago"
    const val GLOBAL_AM_TEXT = "AM"
    const val GLOBAL_PM_TEXT = "PM"
    const val MAX_CALLS_PER_MINUTE = 100
    const val MINIMUM_REQUEST_INTERVAL_IN_SECONDS = 0.1
    const val LOCATION_UPDATE_INTERVAL_MS = 1000L
    const val LOCATION_UPDATE_MIN_DISTANCE_METERS = 1.0f
    const val LOCATION_REQUEST_UPDATE_INTERVAL_MS = 1000L
    const val LOCATION_REQUEST_MIN_UPDATE_INTERVAL_MS = 1000L
    const val LOCATION_REQUEST_TIMEOUT_MS = 15000L
    const val LOCATION_UPDATE_MIN_INTERVAL_MS = 500L
    const val DEFAULT_MAP_ZOOM = 16.5f
    const val STOP_MARKER_SIZE_DP = 32
    const val MAP_PREVIEW_WIDTH_SIZE_DP = 80
    const val MAP_PREVIEW_HEIGHT_SIZE_DP = 160
    const val MAP_PREVIEW_ZOOM_LEVEL = 16.5f
    const val MAP_PREVIEW_RENDER_WIDTH_SIZE_DP = 80
    const val MAP_PREVIEW_RENDER_HEIGHT_SIZE_DP = 160
    const val MAP_PREVIEW_MARKER_SIZE_DP = 20
    const val GLOBAL_API_FOR_SHORT_USAGE = true
    const val STOP_NAME_MAX_PREFIX_LENGTH_FOR_WIDGET = 28
    val CLASSIC_THEM_TEXT_COLOR = Color(0xFFFC7C24)
    val CLASSIC_THEM_BACKGROUND_COLOR_ALWAYS = Color.Black
    val BACKGROUND_COLOR_IN_MODERN_THEME_DAY = Color.White
    val BACKGROUND_COLOR_IN_MODERN_THEME_NIGHT = Color(0xFF232323)
    val TEXT_COLOR_IN_MODERN_THEME_DAY = Color.Black
    val TEXT_COLOR_IN_MODERN_THEME_NIGHT = Color.White
    val LATE_OR_CANCELLED_TEXT_COLOR_IN_MODERN_THEME_ALWAYS = Color(0xFFD2183B)
    val EARLY_OR_DUE_TEXT_COLOR_IN_MODERN_THEME_ALWAYS = AccentBlue
    val CLASSIC_THEME_FONT = R.font.lcd_dot
    val MODERN_THEME_FONT = R.font.courier_prime_bold
    val ACCENT_COLOR_IN_ALL_THEMES = AccentBlue

    const val LONG_SCHEDULE_ENTRY_WITH_EARLY_FOR_TESTING = "671" + SCHEDULE_STRING_SEPARATOR + "University of Manitoba" + SCHEDULE_STRING_SEPARATOR + EARLY_STATUS_TEXT + SCHEDULE_STRING_SEPARATOR  + "12:55 PM"
    const val LONG_SCHEDULE_ENTRY_WITH_LATE_FOR_TESTING = "899" + SCHEDULE_STRING_SEPARATOR + "Kildonan Place" + SCHEDULE_STRING_SEPARATOR + LATE_STATUS_TEXT + SCHEDULE_STRING_SEPARATOR  + "12:55 AM"
    const val LONG_SCHEDULE_ENTRY_WITH_DUE_FOR_TESTING = "W31" + SCHEDULE_STRING_SEPARATOR + "Waterford Green" + SCHEDULE_STRING_SEPARATOR + EARLY_STATUS_TEXT + SCHEDULE_STRING_SEPARATOR  + DUE_STATUS_TEXT
    const val LONG_SCHEDULE_ENTRY_WITH_CANCELLED_FOR_TESTING = "R82" + SCHEDULE_STRING_SEPARATOR + "University of Manitoba" + SCHEDULE_STRING_SEPARATOR + CANCELLED_STATUS_TEXT + SCHEDULE_STRING_SEPARATOR  + "12:55 PM"

    val TEST_ENTRIES = listOf<String>(
        LONG_SCHEDULE_ENTRY_WITH_EARLY_FOR_TESTING,
        LONG_SCHEDULE_ENTRY_WITH_LATE_FOR_TESTING,
        LONG_SCHEDULE_ENTRY_WITH_DUE_FOR_TESTING,
        LONG_SCHEDULE_ENTRY_WITH_CANCELLED_FOR_TESTING
    )

    fun formatDistance(distanceInMeters: Double): String {
        return when {
            distanceInMeters < 1000 -> "${distanceInMeters.roundToInt()} meters away"
            else -> "${(distanceInMeters / 1000).let { "%.1f".format(it) }}km away"
        }
    }

    fun getMaxPreferredStopsInClosestStops(widgetSize: String): Int {
        return when (widgetSize.lowercase()) {
            "small", "lockscreen", "medium", "large" -> 5
            else -> 1
        }
    }

    fun getMaxStopsAllowed(widgetSize: String): Int {
        return when (widgetSize.lowercase()) {
            "small" -> 2
            "medium" -> 3
            "large" -> 3
            "lockscreen" -> 2
            else -> 1
        }
    }
    
    fun getMaxStopsAllowedForMultipleEntries(widgetSize: String): Int {
        return when (widgetSize.lowercase()) {
            "small", "lockscreen" -> 1
            "medium" -> 2
            "large" -> 3
            else -> 1
        }
    }
    
    fun getMaxVariantsAllowed(widgetSize: String): Int {
        return when (widgetSize.lowercase()) {
            "small" -> 1
            "medium" -> 2
            "large" -> 2
            "lockscreen" -> 1
            else -> 2
        }
    }
    
    fun getMaxVariantsAllowedForMultipleEntries(widgetSize: String): Int {
        return when (widgetSize.lowercase()) {
            "small", "lockscreen", "medium", "large" -> 1
            else -> 1
        }
    }
    
    fun getNormalFontSizeForWidgetSize(widgetSize: String): Float {
        return when (widgetSize.lowercase()) {
            "small" -> 12f
            "medium" -> 13f
            "large" -> 14f
            "lockscreen" -> 12f
            else -> 10f
        }
    }
    
    fun getStopNameFontSizeForWidgetPreview(widgetSize: String): Float {
        return when (widgetSize.lowercase()) {
            "small" -> 9f
            "medium" -> 11f
            "large" -> 11f
            "lockscreen" -> 9f
            else -> 8f
        }
    }
    
    fun getLastSeenFontSizeForWidgetPreview(widgetSize: String): Float {
        return when (widgetSize.lowercase()) {
            "small" -> 12f
            "medium" -> 14f
            "large" -> 14f
            "lockscreen" -> 12f
            else -> 14f
        }
    }

    fun getLocationCoordinatesTextSizeForWidget(widgetSize: String): Float {
        return when (widgetSize.lowercase()) {
            "small" -> 12f
            "medium" -> 12f
            "large" -> 12f
            "lockscreen" -> 12f
            else -> 12f
        }
    }

    fun getWidgetPreviewWidthForSize(widgetSize: String, context: Context? = null): Int {
        val baseWidth = when (widgetSize.lowercase()) {
            "small" -> 180
            "medium" -> 400
            "large" -> 400
            "lockscreen" -> 180
            else -> 110
        }
        
        return if (context != null && isLargeDevice(context)) {
            (baseWidth * 1.2f).toInt()
        } else {
            baseWidth
        }
    }

    fun getWidgetPreviewHeightForSize(widgetSize: String, context: Context? = null): Int {
        val baseHeight = when (widgetSize.lowercase()) {
            "small" -> 220
            "medium" -> 220
            "large" -> 400
            "lockscreen" -> 110
            else -> 110
        }

        return if (context != null && isLargeDevice(context)) {
            (baseHeight * 1.2f).toInt()
        } else {
            baseHeight
        }
    }
    
    fun getRouteNumberWidthForWidgetPreview(widgetSize: String): Int {
        return when (widgetSize.lowercase()) {
            "small" -> 40
            "medium" -> 50
            "large" -> 50
            "lockscreen" -> 40
            else -> 50
        }
    }
    
    fun getRouteNameWidthForWidgetPreview(widgetSize: String): Int {
        return when (widgetSize.lowercase()) {
            "small" -> 30
            "medium" -> 100
            "large" -> 100
            "lockscreen" -> 30
            else -> 100
        }
    }

    fun getLastSeenFontSizeForWidget(widgetSize: String): Float {
        return when (widgetSize.lowercase()) {
            "small" -> 12f
            "medium" -> 12f
            "large" -> 12f
            "lockscreen" -> 12f
            else -> 14f
        }
    }


    fun getStopTitleWidthForWidget(widgetSize: String): Int {
        return when (widgetSize.lowercase()) {
            "small" -> 200
            "medium" -> 400
            "large" -> 400
            "lockscreen" -> 200
            else -> 100
        }
    }

    fun getStopTitleTextSizeForWidget(widgetSize: String): Float {
        return when (widgetSize.lowercase()) {
            "small" -> 16f
            "medium" -> 16f
            "large" -> 16f
            "lockscreen" -> 14f
            else -> 14f
        }
    }

    fun getRouteNumberWidthForWidget(widgetSize: String): Int {
        return when (widgetSize.lowercase()) {
            "small" -> 50
            "medium" -> 50
            "large" -> 50
            "lockscreen" -> 50
            else -> 50
        }
    }

    fun getRouteNumberTextSizeForWidget(widgetSize: String): Float {
        return when (widgetSize.lowercase()) {
            "small" -> 14f
            "medium" -> 14f
            "large" -> 14f
            "lockscreen" -> 14f
            else -> 12f
        }
    }

    fun getRouteNameWidthForWidget(widgetSize: String): Int {
        return when (widgetSize.lowercase()) {
            "small" -> 30
            "medium" -> 120
            "large" -> 120
            "lockscreen" -> 30
            else -> 100
        }
    }


    fun getRouteNameTextSizeForWidget(widgetSize: String): Float {
        return when (widgetSize.lowercase()) {
            "small" -> 14f
            "medium" -> 14f
            "large" -> 14f
            "lockscreen" -> 14f
            else -> 12f
        }
    }

    fun getArrivalStatusWidthForWidget(widgetSize: String): Int {
        return when (widgetSize.lowercase()) {
            "small" -> 30
            "medium" -> 60
            "large" -> 60
            "lockscreen" -> 30
            else -> 60
        }
    }

    fun getArrivalStatusTextSizeForWidget(widgetSize: String): Float {
        return when (widgetSize.lowercase()) {
            "small" -> 14f
            "medium" -> 14f
            "large" -> 14f
            "lockscreen" -> 14f
            else -> 12f
        }
    }

    fun getArrivalTimeWidthForWidget(widgetSize: String): Int {
        return when (widgetSize.lowercase()) {
            "small" -> 60
            "medium" -> 80
            "large" -> 80
            "lockscreen" -> 60
            else -> 80
        }
    }

    fun getArrivalTimeTextSizeForWidget(widgetSize: String): Float {
        return when (widgetSize.lowercase()) {
            "small" -> 14f
            "medium" -> 14f
            "large" -> 14f
            "lockscreen" -> 14f
            else -> 12f
        }
    }

    fun getWidgetBackgroundColor(stopViewTheme: StopViewTheme, isDarkMode: Boolean): Int {
        return when (stopViewTheme) {
            StopViewTheme.MODERN -> if (isDarkMode) BACKGROUND_COLOR_IN_MODERN_THEME_NIGHT else BACKGROUND_COLOR_IN_MODERN_THEME_DAY
            StopViewTheme.CLASSIC -> CLASSIC_THEM_BACKGROUND_COLOR_ALWAYS
        }.toArgb()
    }

    fun getWidgetTextFont(stopViewTheme: StopViewTheme): Int {
        return when (stopViewTheme) {
            StopViewTheme.MODERN ->  MODERN_THEME_FONT
            StopViewTheme.CLASSIC -> CLASSIC_THEME_FONT
        }
    }

    fun getWidgetTextColor(stopViewTheme: StopViewTheme, isDarkMode: Boolean): Int {
        return when (stopViewTheme) {
            StopViewTheme.MODERN -> if (isDarkMode) TEXT_COLOR_IN_MODERN_THEME_NIGHT else TEXT_COLOR_IN_MODERN_THEME_DAY
            StopViewTheme.CLASSIC -> CLASSIC_THEM_TEXT_COLOR
        }.toArgb()
    }

    fun getWidgetStatusTextColor(status: String, stopViewTheme: StopViewTheme): Int {
        return when (stopViewTheme) {
            StopViewTheme.MODERN -> when (status) {
                LATE_STATUS_TEXT, CANCELLED_STATUS_TEXT -> LATE_OR_CANCELLED_TEXT_COLOR_IN_MODERN_THEME_ALWAYS.toArgb()
                EARLY_STATUS_TEXT, DUE_STATUS_TEXT -> EARLY_OR_DUE_TEXT_COLOR_IN_MODERN_THEME_ALWAYS.toArgb()
                else -> TEXT_COLOR_IN_MODERN_THEME_DAY.toArgb()
            }
            StopViewTheme.CLASSIC -> CLASSIC_THEM_TEXT_COLOR.toArgb()
        }
    }
    
    fun isLargeDevice(context: Context): Boolean {
        val configuration = context.resources.configuration
        val screenLayout = configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        return screenLayout >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    fun initAPIKey(context: Context) {
        if ( TRANSIT_API_KEY.isBlank() || TRANSIT_API_KEY.isEmpty() )
        {
            TRANSIT_API_KEY = context.applicationContext.packageManager
                .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                .metaData.getString("TRANSIT_API_KEY") ?: ""
        }
    }

    fun getSavedWidgetsForTargetSize(context: Context, targetSize: String): List<WidgetModel> {
        val savedWidgetsManager = SavedWidgetsManager.getInstance(context)
        return savedWidgetsManager.savedWidgets.value.filter { it.widgetData["size"] == targetSize }
    }

    fun getWidgetSchedule(context: Context, appWidgetId: String, widgetConfigId: String?): WidgetSchedule? {
        val savedWidgetSchedulesManager = SavedWidgetSchedulesManager.getInstance(context)
        return if (widgetConfigId == null) {
            null
        } else {
            savedWidgetSchedulesManager.getWidgetSchedule(appWidgetId, widgetConfigId)
        }
    }

    fun savedWidgetSchedule(context: Context, widgetSchedule: WidgetSchedule) {
        val savedWidgetSchedulesManager = SavedWidgetSchedulesManager.getInstance(context)
        savedWidgetSchedulesManager.saveWidgetSchedule(widgetSchedule)
    }

    fun saveWidgetSelection(context: Context, appWidgetId: Int, widget: WidgetModel) {
        val prefs = context.getSharedPreferences(SharedPrefrencesKeys.WIDGET_DATA_ID_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString(SharedPrefrencesKeys.WIDGET_DATA_ID_SHARED_PREFERENCES_KEY_PREFIX + appWidgetId, widget.id)
            apply()
        }
    }

    fun getWidgetConfigUsingAppWidgetId(context: Context, appWidgetId: Int): WidgetModel? {
        val prefs = context.getSharedPreferences(SharedPrefrencesKeys.WIDGET_DATA_ID_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
        val widgetId = prefs.getString(SharedPrefrencesKeys.WIDGET_DATA_ID_SHARED_PREFERENCES_KEY_PREFIX + appWidgetId, null) ?: return null
        val savedWidgetsManager = SavedWidgetsManager.getInstance(context)
        return savedWidgetsManager.savedWidgets.value.find { it.id == widgetId }
    }

    fun deleteWidgetConfigurationUsingWidgetId(context: Context, appWidgetId: Int) {
        val prefs = context.getSharedPreferences(SharedPrefrencesKeys.WIDGET_DATA_ID_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            remove(SharedPrefrencesKeys.WIDGET_DATA_ID_SHARED_PREFERENCES_KEY_PREFIX + appWidgetId)
            apply()
        }
    }

    fun deleteWidgetScheduleUsingAppWidgetId(context: Context, appWidgetId: Int) {
        val widgetConfig = getWidgetConfigUsingAppWidgetId(context, appWidgetId)

        if (widgetConfig != null) {
            val savedWidgetSchedulesManager = SavedWidgetSchedulesManager.getInstance(context)
            savedWidgetSchedulesManager.deleteWidgetSchedule(appWidgetId.toString(), widgetConfig.id)
        }
    }

    fun triggerAllWidgetsLooksUpdates(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        allWidgetProviders.forEach { providerClass ->
            val componentName = ComponentName(context, providerClass)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (appWidgetIds.isNotEmpty()) {
                val updateIntent = Intent(context, providerClass).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(updateIntent)
            }
        }
    }

    fun appHasAnyActiveWidgets(context: Context): Boolean {
        if (allWidgetProviders.isEmpty()) return false

        val appWidgetManager = AppWidgetManager.getInstance(context)

        return allWidgetProviders.any { providerClass ->
            val componentName = ComponentName(context, providerClass)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
            widgetIds.isNotEmpty()
        }
    }

    fun getAllActiveWidgetIds(context: Context): List<Int> {
        if (allWidgetProviders.isEmpty()) return emptyList()

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val allWidgetIds = mutableListOf<Int>()

        allWidgetProviders.forEach { providerClass ->
            val componentName = ComponentName(context, providerClass)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
            allWidgetIds.addAll(widgetIds.toList())
        }

        return allWidgetIds
    }

    fun triggerWidgetCoreUpdatesManagerWithUserSettings(context: Context, sendBroadcastToTriggerWidgetsLookUpdate: Boolean, startUpdatesIfNeeded: Boolean) {

        val settingsManager = SettingsManager.getInstance(context)
        val userOptedInForManualUpdates = settingsManager.userOptedInForManualWidgetUpdates
        val userOptedInForManualUpdatesInLowPower = settingsManager.userOptedInForManualWidgetUpdatesInLowPower
        val widgetUpdatesIntervalInMinutes = settingsManager.widgetManualUpdateMinutes

        if (startUpdatesIfNeeded) {
            WidgetUpdateManager.startCoreUpdatesIfNeeded(
                context,
                debugging = DEBUG_MODE,
                userOptedInForManualUpdates = userOptedInForManualUpdates,
                userOptedInForManualUpdatesInLowPower = userOptedInForManualUpdatesInLowPower,
                debugIntervalMinutes = widgetUpdatesIntervalInMinutes
            )
        } else {
            WidgetUpdateManager.startCoreUpdates(
                context,
                debugging = DEBUG_MODE,
                userOptedInForManualUpdates = userOptedInForManualUpdates,
                userOptedInForManualUpdatesInLowPower = userOptedInForManualUpdatesInLowPower,
                debugIntervalMinutes = widgetUpdatesIntervalInMinutes
            )
        }

        if (sendBroadcastToTriggerWidgetsLookUpdate) {
            triggerAllWidgetsLooksUpdates(context)
        }
    }

    fun hasBackgroundLocationPermission(context: Context): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return context.checkSelfPermission(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            return context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun isThereActiveWidgetsWithLocationAccessNeeded(context: Context): Boolean {
        val activeWidgetIds = getAllActiveWidgetIds(context)
        var toReturn = false

        activeWidgetIds.forEach { appWidgetId ->
            val widgetConfig = getWidgetConfigUsingAppWidgetId(context, appWidgetId)
            if (widgetConfig != null) {
                val widgetSchedule = getWidgetSchedule(context, appWidgetId.toString(), widgetConfig.id)

                if (widgetSchedule != null && widgetConfig.widgetData["isClosestStop"] as? Boolean == true ) {
                    toReturn = true
                    return@forEach
                }
            }
        }

        return toReturn
    }

    fun removeDeletedWidgetInstancesData(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            deleteWidgetScheduleUsingAppWidgetId(context, appWidgetId)
            deleteWidgetConfigurationUsingWidgetId(context, appWidgetId)
        }
    }



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

    val replacePackageUpdateActions = listOf(
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_MY_PACKAGE_UNSUSPENDED,
        Intent.ACTION_MY_PACKAGE_SUSPENDED,
        Intent.ACTION_PACKAGE_REPLACED,
        Intent.ACTION_PACKAGE_ADDED,
        Intent.ACTION_PACKAGE_REMOVED,
        Intent.ACTION_PACKAGE_CHANGED,
        Intent.ACTION_PACKAGE_DATA_CLEARED,
        Intent.ACTION_PACKAGE_FULLY_REMOVED,
    )

    val batterStatusActions = listOf(
        Intent.ACTION_BATTERY_CHANGED,
        Intent.ACTION_BATTERY_LOW,
        Intent.ACTION_BATTERY_OKAY,
        PowerManager.ACTION_POWER_SAVE_MODE_CHANGED
    )

    const val ACTION_UPDATE_WIDGET = "com.aymanhki.peektransit.ACTION_UPDATE_WIDGET"
}

enum class DefaultTab(val index: Int, val displayName: String, val icon: String) {
    MAP(0, "Map", "map"),
    STOPS(1, "Stops", "list"),
    SAVED(2, "Saved", "bookmark"),
    WIDGETS(3, "Widgets", "note"),
    MORE(4, "More", "more_horiz");
    
    companion object {
        fun fromIndex(index: Int): DefaultTab {
            return DefaultTab.entries.find { it.index == index } ?: MAP
        }
    }
}

enum class StopViewTheme(val displayName: String, val description: String) {
    MODERN("Modern", "Auto"),
    CLASSIC("Classic", "Always Dark");
    
    companion object {
        val DEFAULT = MODERN
        
        fun fromString(value: String?): StopViewTheme {
            return StopViewTheme.entries.find { it.displayName == value } ?: DEFAULT
        }
    }
}

enum class TimeFormat {
    MINUTES_ONLY,
    CLOCK_TIME,
    MIXED;

     companion object {
         val DEFAULT = MINUTES_ONLY

         fun fromString(value: String): TimeFormat {

             return when (value) {
                 "minutes" -> MINUTES_ONLY
                 "clock" -> CLOCK_TIME
                 "mixed", "default" -> MIXED
                 else -> MIXED
             }
         }
     }
}

object SharedPrefrencesKeys {
    const val DEFAULT_TAB = "default_tab_preference"
    const val STOP_VIEW_THEME = "stop_view_theme_preference"
    const val SHARED_STOP_VIEW_THEME = "shared_stop_view_theme"
    const val WIDGET_UPDATE_SETTINGS_MANUAL_UPDATES = "widget_update_settings_manual_updates"
    const val WIDGET_UPDATE_SETTINGS_MANUAL_UPDATES_IN_LOW_POWER = "widget_update_settings_manual_updates_in_low_power"
    const val WIDGET_UPDATE_SETTINGS_MANUAL_UPDATES_MINUTES = "widget_update_settings_manual_updates_minutes"
    const val WIDGET_DATA_ID_SHARED_PREFERENCES_KEY = "PeekTransitWidgetData"
    const val WIDGET_DATA_ID_SHARED_PREFERENCES_KEY_PREFIX = "widget_data_id_"
}

val allWidgetProviders = listOf(
    PeekTransitSmallWidgetProvider::class.java,
    PeekTransitMediumWidgetProvider::class.java,
    PeekTransitLargeWidgetProvider::class.java,
    PeekTransitLockScreenWidgetProvider::class.java
)

