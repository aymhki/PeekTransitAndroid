package com.aymanhki.peektransit.managers

import android.content.Context
import android.content.SharedPreferences
import com.aymanhki.peektransit.utils.DefaultTab
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.SettingsKeys
import com.aymanhki.peektransit.utils.StopViewTheme

class SettingsManager private constructor(context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "PeekTransitSettings"
        
        @Volatile
        private var INSTANCE: SettingsManager? = null
        
        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    var defaultTab: DefaultTab
        get() {
            val tabIndex = sharedPreferences.getInt(SettingsKeys.DEFAULT_TAB, DefaultTab.MAP.index)
            return DefaultTab.fromIndex(tabIndex)
        }
        set(value) {
            sharedPreferences.edit()
                .putInt(SettingsKeys.DEFAULT_TAB, value.index)
                .apply()
        }
    
    var stopViewTheme: StopViewTheme
        get() {
            val themeName = sharedPreferences.getString(SettingsKeys.SHARED_STOP_VIEW_THEME, StopViewTheme.DEFAULT.displayName)
            return StopViewTheme.fromString(themeName)
        }
        set(value) {
            sharedPreferences.edit()
                .putString(SettingsKeys.SHARED_STOP_VIEW_THEME, value.displayName)
                .apply()
        }
} 