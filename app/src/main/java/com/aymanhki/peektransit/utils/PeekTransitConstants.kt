package com.aymanhki.peektransit.utils

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aymanhki.peektransit.ui.theme.AccentBlue
import com.aymanhki.peektransit.R

object PeekTransitConstants {

    // API Configuration
    lateinit var TRANSIT_API_KEY: String
    const val BASE_URL = "https://api.winnipegtransit.com/v4/"
    
    // Stop Configuration
    const val STOPS_DISTANCE_RADIUS = 1000.0 // meters
    const val MAX_STOPS_ALLOWED_TO_FETCH = 25
    const val MAX_STOPS_ALLOWED_TO_FETCH_FOR_SEARCH = 15
    const val DISTANCE_CHANGE_ALLOWED_BEFORE_REFRESHING_STOPS = STOPS_DISTANCE_RADIUS/3 // meters
    
    // Route Configuration
    const val MAX_BUS_ROUTE_LENGTH = 10
    const val MAX_BUS_ROUTE_PREFIX_LENGTH = 8
    const val TIME_PERIOD_ALLOWED_FOR_NEXT_BUS_ROUTES = 12 // hours
    
    // Widget Configuration
    const val MAX_STOPS_ALLOWED_SMALL_WIDGET = 2
    const val MAX_STOPS_ALLOWED_MEDIUM_WIDGET = 2
    const val MAX_STOPS_ALLOWED_LARGE_WIDGET = 3
    const val MAX_STOPS_ALLOWED_LOCKSCREEN_WIDGET = 2
    
    const val MAX_VARIANTS_ALLOWED_SMALL_WIDGET = 1
    const val MAX_VARIANTS_ALLOWED_MEDIUM_WIDGET = 2
    const val MAX_VARIANTS_ALLOWED_LARGE_WIDGET = 2
    const val MAX_VARIANTS_ALLOWED_LOCKSCREEN_WIDGET = 1
    
    // Time Configuration
    const val PERIOD_BEFORE_SHOWING_MINUTES_UNTIL_NEXT_BUS = 15 // minutes
    const val MINUTES_ALLOWED_TO_KEEP_DUE_BUSES_IN_SCHEDULE = 1
    const val WINNIPEG_ZONE = "America/Winnipeg"
    const val DATE_FORMAT_API = "yyyy-MM-dd"
    const val TIME_FORMAT_API = "HH:mm"
    
    // Search Configuration
    const val SEARCH_DEBOUNCE_DELAY_MS = 1500L
    
    // Text Constants
    const val SCHEDULE_STRING_SEPARATOR = " ---- "
    const val COMPOSITE_KEY_LINKER_FOR_DICTIONARIES = "-"
    const val WIDGET_TEXT_PLACEHOLDER = "TBD"
    
    // Status Text
    const val LATE_STATUS_TEXT = "Late"
    const val EARLY_STATUS_TEXT = "Early"
    const val CANCELLED_STATUS_TEXT = "Cancelled"
    const val OK_STATUS_TEXT = "Ok"
    const val DUE_STATUS_TEXT = "Due"
    
    // Time Text
    const val MINUTES_REMAINING_TEXT = "min."
    const val MINUTES_PASSED_TEXT = "min. ago"
    const val GLOBAL_AM_TEXT = "AM"
    const val GLOBAL_PM_TEXT = "PM"
    
    // Rate Limiting
    const val MAX_CALLS_PER_MINUTE = 100
    const val MINIMUM_REQUEST_INTERVAL = 0.1 // seconds
    
    // Cache Configuration
    const val CACHE_DURATION_SECONDS = 30
    const val REFRESH_WIDGET_TIMELINE_AFTER_SECONDS = 1
    const val WIDGET_UPDATE_INTERVAL_SECONDS = 300 // 5 minutes
    
    // Max preferred stops
    fun getMaxPerferredstopsInClosestStops(): Int = 5
    
    // Location Tracking Configuration
    // Used by MainViewModel.startLocationMonitoring() for continuous location updates
    const val LOCATION_UPDATE_INTERVAL_MS = 1000L // How often to request location updates (1 second)
    
    // Used by LocationManager.startLocationUpdates() for distance-based location filtering
    const val LOCATION_UPDATE_MIN_DISTANCE_METERS = 5.0f // Minimum distance to trigger location update (5 meters)
    
    // Used by LocationManager.requestFreshLocation() for one-time location requests
    const val LOCATION_REQUEST_UPDATE_INTERVAL_MS = 1000L // Update interval for fresh location requests
    const val LOCATION_REQUEST_MIN_UPDATE_INTERVAL_MS = 1000L // Minimum interval between location updates
    const val LOCATION_REQUEST_TIMEOUT_MS = 100000L // Timeout for location requests (100 seconds)
    
    // Used by LocationManager.startLocationUpdates() for fine-tuning location updates
    const val LOCATION_UPDATE_MIN_INTERVAL_MS = 500L // Fastest possible location updates (500ms)
    
    // Used by MapViewScreen.LaunchedEffect(liveLocation) for camera movement
    const val MAP_CAMERA_UPDATE_THRESHOLD_METERS = 10.0f // Distance required to move map camera (10 meters)
    const val MAP_CAMERA_ANIMATION_DURATION_MS = 500 // Camera movement animation duration (500ms)

    // Map Configuration
    const val DEFAULT_MAP_ZOOM = 16.5f
    const val STOP_MARKER_SIZE_DP = 32
    
    // Map Preview Configuration
    const val MAP_PREVIEW_WIDTH_SIZE_DP = 80
    const val MAP_PREVIEW_HEIGHT_SIZE_DP = 160
    const val MAP_PREVIEW_ZOOM_LEVEL = 16.5f
    const val MAP_PREVIEW_RENDER_WIDTH_SIZE_DP = 80
    const val MAP_PREVIEW_RENDER_HEIGHT_SIZE_DP = 160
    const val MAP_PREVIEW_MARKER_SIZE_DP = 20

    // Global API Usage
    const val GLOBAL_API_FOR_SHORT_USAGE = true
    
    // Widget-specific constants
    const val MAX_BUS_ROUTE_LENGTH_FOR_WIDGET = 10
    const val MAX_BUS_ROUTE_PREFIX_LENGTH_FOR_WIDGET = 10
    const val STOP_NAME_MAX_PREFIX_LENGTH_FOR_WIDGET = 28
    const val MAX_PREFERRED_STOPS_IN_CLOSEST_STOPS = 5
    const val GLOBAL_BUS_ICON = "🚌"
    
    // Font Sizes
    const val NORMAL_FONT_SIZE_LARGE = 14f
    const val NORMAL_FONT_SIZE_MEDIUM = 13f
    const val NORMAL_FONT_SIZE_SMALL = 12f
    const val NORMAL_FONT_SIZE_LOCKSCREEN = 12f
    const val NORMAL_FONT_SIZE_DEFAULT = 10f
    
    const val STOP_NAME_FONT_SIZE_LARGE = 11f
    const val STOP_NAME_FONT_SIZE_MEDIUM = 11f
    const val STOP_NAME_FONT_SIZE_SMALL = 9f
    const val STOP_NAME_FONT_SIZE_LOCKSCREEN = 9f
    const val STOP_NAME_FONT_SIZE_DEFAULT = 8f
    
    const val LAST_SEEN_FONT_SIZE = 10f
    const val LAST_SEEN_FONT_SIZE_DEFAULT = 8f

    // Created specifically to be used in the widget glance component,
    // since the glance component for widgets can't access material colors and fonts like the preview in app.
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
    
    // Widget size functions
    fun getMaxStopsAllowed(widgetSize: String): Int {
        return when (widgetSize.lowercase()) {
            "small" -> MAX_STOPS_ALLOWED_SMALL_WIDGET
            "medium" -> MAX_STOPS_ALLOWED_MEDIUM_WIDGET
            "large" -> MAX_STOPS_ALLOWED_LARGE_WIDGET
            "lockscreen" -> MAX_STOPS_ALLOWED_LOCKSCREEN_WIDGET
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
            "small" -> MAX_VARIANTS_ALLOWED_SMALL_WIDGET
            "medium" -> MAX_VARIANTS_ALLOWED_MEDIUM_WIDGET
            "large" -> MAX_VARIANTS_ALLOWED_LARGE_WIDGET
            "lockscreen" -> MAX_VARIANTS_ALLOWED_LOCKSCREEN_WIDGET
            else -> 2
        }
    }
    
    fun getMaxVariantsAllowedForMultipleEntries(widgetSize: String): Int {
        return when (widgetSize.lowercase()) {
            "small", "lockscreen", "medium", "large" -> 1
            else -> 1
        }
    }
    
    // Widget font size functions
    fun getNormalFontSizeForWidgetSize(widgetSize: String): Float {
        return when (widgetSize.lowercase()) {
            "small" -> NORMAL_FONT_SIZE_SMALL
            "medium" -> NORMAL_FONT_SIZE_MEDIUM
            "large" -> NORMAL_FONT_SIZE_LARGE
            "lockscreen" -> NORMAL_FONT_SIZE_LOCKSCREEN
            else -> NORMAL_FONT_SIZE_DEFAULT
        }
    }
    
    fun getStopNameFontSizeForWidgetSize(widgetSize: String): Float {
        return when (widgetSize.lowercase()) {
            "small" -> STOP_NAME_FONT_SIZE_SMALL
            "medium" -> STOP_NAME_FONT_SIZE_MEDIUM
            "large" -> STOP_NAME_FONT_SIZE_LARGE
            "lockscreen" -> STOP_NAME_FONT_SIZE_LOCKSCREEN
            else -> STOP_NAME_FONT_SIZE_DEFAULT
        }
    }
    
    fun getLastSeenFontSizeForWidgetSize(widgetSize: String): Float {
        return when (widgetSize.lowercase()) {
            "small", "medium", "large", "lockscreen" -> LAST_SEEN_FONT_SIZE
            else -> LAST_SEEN_FONT_SIZE_DEFAULT
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
    
    // Route display functions
    fun getRouteNumberWidth(widgetSize: String): Int {
        return when (widgetSize.lowercase()) {
            "small" -> 40
            "medium" -> 50
            "large" -> 50
            "lockscreen" -> 40
            else -> 50
        }
    }
    
    fun getRouteNameWidth(widgetSize: String): Int {
        return when (widgetSize.lowercase()) {
            "small" -> 30
            "medium" -> 100
            "large" -> 100
            "lockscreen" -> 30
            else -> 100
        }
    }
    
    fun shouldShowShortRouteName(status: String): Boolean {
        return status.lowercase() in listOf("late", "early", "cancelled")
    }
    
    // Device detection
    fun isLargeDevice(context: Context): Boolean {
        val configuration = context.resources.configuration
        val screenLayout = configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        return screenLayout >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }
    
    // Widget-specific helper functions
    fun getScheduleStringSeparator(): String = SCHEDULE_STRING_SEPARATOR
    
    fun getWidgetTextPlaceholder(): String = WIDGET_TEXT_PLACEHOLDER
    
    fun getMaxBusRouteLengthForWidget(): Int = MAX_BUS_ROUTE_LENGTH_FOR_WIDGET
    
    fun getMaxBusRoutePrefixLengthForWidget(): Int = MAX_BUS_ROUTE_PREFIX_LENGTH_FOR_WIDGET
    
    fun getStopNameMaxPrefixLengthForWidget(): Int = STOP_NAME_MAX_PREFIX_LENGTH_FOR_WIDGET
    
    fun getRefreshWidgetTimelineAfterHowManySeconds(): Int = REFRESH_WIDGET_TIMELINE_AFTER_SECONDS
}

enum class DefaultTab(val index: Int, val displayName: String, val icon: String) {
    MAP(0, "Map", "map"),
    STOPS(1, "Stops", "list"),
    SAVED(2, "Saved", "bookmark"),
    WIDGETS(3, "Widgets", "note"),
    MORE(4, "More", "more_horiz");
    
    companion object {
        fun fromIndex(index: Int): DefaultTab {
            return values().find { it.index == index } ?: MAP
        }
    }
}

enum class StopViewTheme(val displayName: String, val description: String) {
    MODERN("Modern", "Auto"),
    CLASSIC("Classic", "Always Dark");
    
    companion object {
        val DEFAULT = MODERN
        
        fun fromString(value: String?): StopViewTheme {
            return values().find { it.displayName == value } ?: DEFAULT
        }
    }
}

enum class TimeFormat {
    MINUTES_ONLY,
    CLOCK_TIME,
    MIXED
}

object SettingsKeys {
    const val DEFAULT_TAB = "default_tab_preference"
    const val STOP_VIEW_THEME = "stop_view_theme_preference"
    const val SHARED_STOP_VIEW_THEME = "shared_stop_view_theme"
}