package com.aymanhki.peektransit.data.models

data class WidgetModel(
    val id: String,
    val name: String,
    val widgetData: Map<String, Any>
) {
    fun getStops(): List<Stop> {
        return (widgetData["stops"] as? List<*>)?.filterIsInstance<Stop>() ?: emptyList()
    }
    
    fun getSelectedStops(): List<Stop> {
        return (widgetData["selectedStops"] as? List<*>)?.filterIsInstance<Stop>() ?: emptyList()
    }
    
    fun getVariants(): List<Variant> {
        return (widgetData["variants"] as? List<*>)?.filterIsInstance<Variant>() ?: emptyList()
    }
    
    fun getSelectedVariants(): List<Variant> {
        return (widgetData["selectedVariants"] as? List<*>)?.filterIsInstance<Variant>() ?: emptyList()
    }
    
    fun getWidgetSize(): String {
        return widgetData["size"] as? String ?: "medium"
    }
    
    fun getShowLastUpdated(): Boolean {
        return widgetData["showLastUpdated"] as? Boolean ?: true
    }
    
    fun getTimeFormat(): String {
        return widgetData["timeFormat"] as? String ?: "mixed"
    }
    
    fun getShowMultipleArrivals(): Boolean {
        return widgetData["showMultipleArrivals"] as? Boolean ?: false
    }
    
    fun getUseNearestStop(): Boolean {
        return widgetData["useNearestStop"] as? Boolean ?: false
    }
    
    fun getUsePreferredStops(): Boolean {
        return widgetData["usePreferredStops"] as? Boolean ?: false
    }
    
    fun getAutoSelectSoonestBus(): Boolean {
        return widgetData["autoSelectSoonestBus"] as? Boolean ?: false
    }
}