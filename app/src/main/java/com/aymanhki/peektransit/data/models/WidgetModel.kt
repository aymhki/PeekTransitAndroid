package com.aymanhki.peektransit.data.models

import com.aymanhki.peektransit.data.adapters.WidgetModelTypeAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import java.util.UUID

data class WidgetModel(
    val id: String = UUID.randomUUID().toString(),
    val widgetData: Map<String, Any> = emptyMap()
) {
    fun toJson(): String {
        return createGson().toJson(this)
    }
    
    companion object {
        private fun createGson(): Gson {
            return GsonBuilder()
                .registerTypeAdapter(WidgetModel::class.java, WidgetModelTypeAdapter())
                .serializeSpecialFloatingPointValues()
                .create()
        }
        
        fun fromJson(json: String): WidgetModel? {
            return try {
                createGson().fromJson(json, WidgetModel::class.java)
            } catch (e: JsonSyntaxException) {
                null
            }
        }
        
        fun parseWidgetData(widgetData: Map<String, Any>): WidgetConfiguration {
            val gson = createGson()
            
            val size = widgetData["size"] as? String ?: "small"
            val name = widgetData["name"] as? String ?: "Unnamed Widget"
            val showLastUpdatedStatus = widgetData["showLastUpdatedStatus"] as? Boolean ?: false
            val timeFormat = widgetData["timeFormat"] as? String ?: "default"
            val multipleEntriesPerVariant = widgetData["multipleEntriesPerVariant"] as? Boolean ?: false
            val isClosestStop = widgetData["isClosestStop"] as? Boolean ?: false
            val noSelectedVariants = widgetData["noSelectedVariants"] as? Boolean ?: false
            
            val selectedStops = widgetData["stops"] as? List<Stop> ?: emptyList()
            val preferredStops = widgetData["preferredStops"] as? List<Stop> ?: emptyList()
            val selectedVariants = widgetData["selectedVariants"] as? Map<String, List<Variant>> ?: emptyMap()
            
            return WidgetConfiguration(
                size = size,
                name = name,
                showLastUpdatedStatus = showLastUpdatedStatus,
                timeFormat = timeFormat,
                multipleEntriesPerVariant = multipleEntriesPerVariant,
                isClosestStop = isClosestStop,
                noSelectedVariants = noSelectedVariants,
                stops = selectedStops,
                preferredStops = preferredStops,
                selectedVariants = selectedVariants
            )
        }
        
    }
}

data class WidgetConfiguration(
    val size: String,
    val name: String,
    val showLastUpdatedStatus: Boolean,
    val timeFormat: String,
    val multipleEntriesPerVariant: Boolean,
    val isClosestStop: Boolean,
    val noSelectedVariants: Boolean,
    val stops: List<Stop>,
    val preferredStops: List<Stop>,
    val selectedVariants: Map<String, List<Variant>>
) {
    fun toWidgetData(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()
        data["size"] = size
        data["name"] = name
        data["showLastUpdatedStatus"] = showLastUpdatedStatus
        data["timeFormat"] = timeFormat
        data["multipleEntriesPerVariant"] = multipleEntriesPerVariant
        data["isClosestStop"] = isClosestStop
        data["noSelectedVariants"] = noSelectedVariants
        data["createdAt"] = System.currentTimeMillis()
        
        if (stops.isNotEmpty()) {
            val stopsWithVariants = stops.map { stop ->
                val stopKey = stop.number.toString()
                val stopVariants = selectedVariants[stopKey] ?: emptyList()
                
                stop.copy(
                    variants = stopVariants,
                    selectedVariants = stopVariants
                )
            }
            data["stops"] = stopsWithVariants
        }
        
        if (preferredStops.isNotEmpty()) {
            val stopsWithVariants = preferredStops.map { stop ->
                val stopKey = stop.number.toString()
                val stopVariants = selectedVariants[stopKey] ?: emptyList()
                
                stop.copy(
                    variants = stopVariants,
                    selectedVariants = stopVariants
                )
            }
            data["preferredStops"] = stopsWithVariants
        }
        
        if (selectedVariants.isNotEmpty()) {
            data["selectedVariants"] = selectedVariants
        }
        
        return data
    }
}

