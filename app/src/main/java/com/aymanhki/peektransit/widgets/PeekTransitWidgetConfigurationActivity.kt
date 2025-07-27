package com.aymanhki.peektransit.widgets


class PeekTransitSmallWidgetConfigurationActivity : BaseWidgetConfigurationActivity() {
    override val widgetSize: String = "small"
    override val configurationTitle: String = "Configure Small Widget"
}

class PeekTransitMediumWidgetConfigurationActivity : BaseWidgetConfigurationActivity() {
    override val widgetSize: String = "medium"
    override val configurationTitle: String = "Configure Medium Widget"
}

class PeekTransitLargeWidgetConfigurationActivity : BaseWidgetConfigurationActivity() {
    override val widgetSize: String = "large"
    override val configurationTitle: String = "Configure Large Widget"
}

class PeekTransitLockScreenWidgetConfigurationActivity : BaseWidgetConfigurationActivity() {
    override val widgetSize: String = "lockscreen"
    override val configurationTitle: String = "Configure Lock Screen Widget"
}
