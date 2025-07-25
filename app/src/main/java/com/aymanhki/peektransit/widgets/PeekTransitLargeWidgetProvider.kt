package com.aymanhki.peektransit.widgets

import com.aymanhki.peektransit.R

class PeekTransitLargeWidgetProvider : BaseWidgetProvider() {
    override val mainLayoutResId: Int = R.layout.peek_transit_large_layout
    override val initialLayoutResId: Int = R.layout.peek_transit_large_initial_layout
    override val configureButtonResId: Int = R.id.configure_large_widget_btn
    override val configurationActivityClass: Class<*> = PeekTransitLargeWidgetConfigurationActivity::class.java
    override val logTag: String = "LargeWidgetProvider"
}


