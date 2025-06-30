package com.aymanhki.peektransit.utils

import androidx.compose.ui.graphics.Color


object PeekTransitConstants {

    // API Configuration
    lateinit var TRANSIT_API_KEY: String
    const val BASE_URL = "https://api.winnipegtransit.com/v4/"
    
    // Stop Configuration
    const val STOPS_DISTANCE_RADIUS = 500.0 // meters
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
    
    // Theme Colors
    const val CLASSIC_THEME_COLOR = "#EB8634"
    const val CLASSIC_THEME_BACKGROUND_COLOR = "#000000"

    val CLASSIC_THEM_TEXT_COLOR = Color(0xFFEB8634)
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