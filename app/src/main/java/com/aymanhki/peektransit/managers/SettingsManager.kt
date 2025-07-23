package com.aymanhki.peektransit.managers

import android.content.Context
import android.content.SharedPreferences
import com.aymanhki.peektransit.utils.DefaultTab
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.SettingsKeys
import com.aymanhki.peektransit.utils.StopViewTheme
import com.aymanhki.peektransit.widgets.PeekTransitLargeWidgetProvider
import androidx.core.content.edit


class SettingsManager private constructor(private val context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "PeekTransitSettings"

        fun getInstance(context: Context): SettingsManager {
            return SettingsManager(context.applicationContext)
        }
    }
    
    var defaultTab: DefaultTab
        get() {
            val tabIndex = sharedPreferences.getInt(SettingsKeys.DEFAULT_TAB, DefaultTab.MAP.index)
            return DefaultTab.fromIndex(tabIndex)
        }
        set(value) {
            sharedPreferences.edit {
                putInt(SettingsKeys.DEFAULT_TAB, value.index)
            }
        }
    
    var stopViewTheme: StopViewTheme
        get() {
            val themeName = sharedPreferences.getString(SettingsKeys.SHARED_STOP_VIEW_THEME, StopViewTheme.DEFAULT.displayName)
            return StopViewTheme.fromString(themeName)
        }
        set(value) {
            sharedPreferences.edit {
                putString(SettingsKeys.SHARED_STOP_VIEW_THEME, value.displayName)
            }

            PeekTransitConstants.triggerWidgetUpdateUsingProvider(context, PeekTransitLargeWidgetProvider::class.java)
        }

    var userOptedInForManualWidgetUpdates: Boolean
        get() = sharedPreferences.getBoolean(SettingsKeys.WIDGET_UPDATE_SETTINGS_MANUAL_UPDATES, true)
        set(value) {
            sharedPreferences.edit {
                putBoolean(SettingsKeys.WIDGET_UPDATE_SETTINGS_MANUAL_UPDATES, value)
            }
        }

    var userOptedInForManualWidgetUpdatesInLowPower: Boolean
        get() = sharedPreferences.getBoolean(SettingsKeys.WIDGET_UPDATE_SETTINGS_MANUAL_UPDATES_IN_LOW_POWER, true)
        set(value) {
            sharedPreferences.edit {
                putBoolean(SettingsKeys.WIDGET_UPDATE_SETTINGS_MANUAL_UPDATES_IN_LOW_POWER, value)
            }
        }

    var widgetManualUpdateMinutes: Int
        get() = sharedPreferences.getInt(SettingsKeys.WIDGET_UPDATE_SETTINGS_MANUAL_UPDATES_MINUTES, PeekTransitConstants.HOW_OFTEN_TO_UPDATE_WIDGET_IN_DEBUG_MODE_IN_MINUTES_BY_DEFAULT)
        set(value) {
            sharedPreferences.edit {
                putInt(SettingsKeys.WIDGET_UPDATE_SETTINGS_MANUAL_UPDATES_MINUTES, value)
            }
        }

}

