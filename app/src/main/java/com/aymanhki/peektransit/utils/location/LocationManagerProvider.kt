package com.aymanhki.peektransit.utils.location

import android.content.Context

object LocationManagerProvider {
    fun getInstance(context: Context): LocationManager {
        return LocationManager(context.applicationContext)
    }
} 