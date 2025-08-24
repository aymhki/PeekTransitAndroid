package com.aymanhki.peektransit.managers

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class FirstLaunchManager private constructor(context: Context) {

    companion object {
        private const val PREFS_NAME = "peek_transit_first_launch"
        private const val KEY_IS_FIRST_LAUNCH = "is_first_launch"

        @Volatile
        private var INSTANCE: FirstLaunchManager? = null

        fun getInstance(context: Context): FirstLaunchManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirstLaunchManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val isFirstLaunch: Boolean
        get() = sharedPreferences.getBoolean(KEY_IS_FIRST_LAUNCH, true)

    fun setFirstLaunchCompleted() {
        sharedPreferences.edit {
            putBoolean(KEY_IS_FIRST_LAUNCH, false)
        }
    }

    fun resetFirstLaunchState() {
        sharedPreferences.edit {
            putBoolean(KEY_IS_FIRST_LAUNCH, true)
        }
    }
}
