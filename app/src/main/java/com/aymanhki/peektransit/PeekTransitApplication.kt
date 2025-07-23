package com.aymanhki.peektransit

import android.app.Application
import com.aymanhki.peektransit.utils.PeekTransitConstants

class PeekTransitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PeekTransitConstants.initAPIKey(this)
        PeekTransitConstants.triggerWidgetCoreUpdatesManagerWithUserSettings(this, true, false)
    }
}
