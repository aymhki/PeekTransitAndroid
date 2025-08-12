package com.aymanhki.peektransit.managers

import android.content.Context
import android.content.SharedPreferences
import com.aymanhki.peektransit.utils.DefaultTab
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.SharedPreferencesKeys
import com.aymanhki.peektransit.utils.StopViewTheme
import androidx.core.content.edit
import com.aymanhki.peektransit.utils.PeekTransitConstants.triggerWidgetCoreUpdatesManagerWithUserSettings


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
            val tabIndex = sharedPreferences.getInt(SharedPreferencesKeys.DEFAULT_TAB, DefaultTab.MAP.index)
            return DefaultTab.fromIndex(tabIndex)
        }
        set(value) {
            sharedPreferences.edit {
                putInt(SharedPreferencesKeys.DEFAULT_TAB, value.index)
            }
        }
    
    var stopViewTheme: StopViewTheme
        get() {
            val themeName = sharedPreferences.getString(SharedPreferencesKeys.SHARED_STOP_VIEW_THEME, StopViewTheme.DEFAULT.displayName)
            return StopViewTheme.fromString(themeName)
        }
        set(value) {
            sharedPreferences.edit {
                putString(SharedPreferencesKeys.SHARED_STOP_VIEW_THEME, value.displayName)
            }

            triggerWidgetCoreUpdatesManagerWithUserSettings(context, true, true)
        }

    var userOptedInForManualWidgetUpdates: Boolean
        get() = sharedPreferences.getBoolean(SharedPreferencesKeys.WIDGET_UPDATE_SETTINGS_MANUAL_UPDATES, true)
        set(value) {
            sharedPreferences.edit {
                putBoolean(SharedPreferencesKeys.WIDGET_UPDATE_SETTINGS_MANUAL_UPDATES, value)
            }

            triggerWidgetCoreUpdatesManagerWithUserSettings(context, true, false)
        }

    var userOptedInForManualWidgetUpdatesInLowPower: Boolean
        get() = sharedPreferences.getBoolean(SharedPreferencesKeys.WIDGET_UPDATE_SETTINGS_MANUAL_UPDATES_IN_LOW_POWER, true)
        set(value) {
            sharedPreferences.edit {
                putBoolean(SharedPreferencesKeys.WIDGET_UPDATE_SETTINGS_MANUAL_UPDATES_IN_LOW_POWER, value)
            }

            triggerWidgetCoreUpdatesManagerWithUserSettings(context, true, false)
        }

    var widgetManualUpdateMinutes: Int
        get() = sharedPreferences.getInt(SharedPreferencesKeys.WIDGET_UPDATE_SETTINGS_MANUAL_UPDATES_MINUTES, PeekTransitConstants.HOW_OFTEN_TO_UPDATE_WIDGET_IN_DEBUG_MODE_IN_MINUTES_BY_DEFAULT)
        set(value) {
            sharedPreferences.edit {
                putInt(SharedPreferencesKeys.WIDGET_UPDATE_SETTINGS_MANUAL_UPDATES_MINUTES, value)
            }

            triggerWidgetCoreUpdatesManagerWithUserSettings(context, true, false)
        }

}

