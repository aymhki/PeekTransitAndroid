package com.aymanhki.peektransit.utils

import com.aymanhki.peektransit.data.models.Stop
import com.aymanhki.peektransit.data.models.Variant
import com.aymanhki.peektransit.data.models.WidgetConfiguration

object WidgetPreviewHelper {
    
    data class PreviewResult(
        val scheduleData: List<String>?,
        val widgetData: Map<String, Any>?
    )
    
    fun generatePreviewSchedule(
        widgetData: Map<String, Any>,
        noConfig: Boolean,
        timeFormat: String,
        showLastUpdatedStatus: Boolean,
        multipleEntriesPerVariant: Boolean,
        showLateTextStatus: Boolean = true
    ): PreviewResult? {
        val previewSchedules = mutableListOf<String>()
        val updatedWidgetData = widgetData.toMutableMap()
        
        val timeFormatTextToUse = when (timeFormat) {
            "minutes" -> "X(X) ${PeekTransitConstants.MINUTES_REMAINING_TEXT}"
            "clock" -> "HH:MM ${PeekTransitConstants.GLOBAL_AM_TEXT}/${PeekTransitConstants.GLOBAL_PM_TEXT}"
            else -> "X(X) ${PeekTransitConstants.MINUTES_REMAINING_TEXT}"
        }
        
        val useLateText = timeFormat == "minutes"
        val stringToUseBasedOnTimeFormat = if ((useLateText || multipleEntriesPerVariant) && showLateTextStatus) {
            PeekTransitConstants.EARLY_STATUS_TEXT
        } else {
            PeekTransitConstants.OK_STATUS_TEXT
        }
        
        if (!noConfig) {
            val isClosestStop = widgetData["isClosestStop"] as? Boolean ?: false
            
            if (!isClosestStop) {
                // Widget with specific stops configured
                val stops = widgetData["stops"] as? List<*> ?: return null
                
                for (stop in stops) {
                    val noSelectedVariants = widgetData["noSelectedVariants"] as? Boolean ?: false

                    if (stop !is Stop) continue

                    if (!noSelectedVariants) {
                        // Use selected variants from stop
                        val variants = stop.variants
                        
                        for (variant in variants) {
                            val key = variant.getRouteKey()
                            val name = variant.name
                            
                            if (multipleEntriesPerVariant) {
                                // First entry with minutes
                                previewSchedules.add(generatePreviewEntry(
                                    key,
                                    name,
                                    stringToUseBasedOnTimeFormat,
                                    "X(X) ${PeekTransitConstants.MINUTES_REMAINING_TEXT}"
                                ))
                                // Second entry with clock time
                                previewSchedules.add(generatePreviewEntry(
                                    key,
                                    name,
                                    PeekTransitConstants.OK_STATUS_TEXT,
                                    "HH:MM ${PeekTransitConstants.GLOBAL_AM_TEXT}/${PeekTransitConstants.GLOBAL_PM_TEXT}"
                                ))
                            } else {
                                previewSchedules.add(generatePreviewEntry(
                                    key,
                                    name,
                                    stringToUseBasedOnTimeFormat,
                                    timeFormatTextToUse
                                ))
                            }
                        }
                    } else {
                        // Generate placeholder variants when no variants selected
                        val widgetSize = widgetData["size"] as? String ?: "medium"
                        val maxVariants = if (multipleEntriesPerVariant) {
                            PeekTransitConstants.getMaxVariantsAllowedForMultipleEntries(widgetSize)
                        } else {
                            PeekTransitConstants.getMaxVariantsAllowed(widgetSize)
                        }
                        
                        val selectedVariants = mutableListOf<Variant>()
                        repeat(maxVariants) {
                            val variant = Variant(
                                key = PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER,
                                name = PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER
                            )
                            selectedVariants.add(variant)
                            
                            if (multipleEntriesPerVariant) {
                                previewSchedules.add(generatePreviewEntry(
                                    PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER,
                                    PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER,
                                    stringToUseBasedOnTimeFormat,
                                    "X(X) ${PeekTransitConstants.MINUTES_REMAINING_TEXT}"
                                ))
                                previewSchedules.add(generatePreviewEntry(
                                    PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER,
                                    PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER,
                                    PeekTransitConstants.OK_STATUS_TEXT,
                                    "HH:MM ${PeekTransitConstants.GLOBAL_AM_TEXT}/${PeekTransitConstants.GLOBAL_PM_TEXT}"
                                ))
                            } else {
                                previewSchedules.add(generatePreviewEntry(
                                    PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER,
                                    PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER,
                                    stringToUseBasedOnTimeFormat,
                                    timeFormatTextToUse
                                ))
                            }
                        }
                        
                        // Update stop with generated variants
                        val updatedStop = stop.copy().apply {
                            variants = selectedVariants
                        }
                        
                        val currentStops = updatedWidgetData["stops"] as? MutableList<Stop> ?: mutableListOf()
                        val stopIndex = currentStops.indexOfFirst { it.number == stop.number }
                        if (stopIndex != -1) {
                            currentStops[stopIndex] = updatedStop
                            updatedWidgetData["stops"] = currentStops
                        }
                    }
                }
            } else {
                // Closest stop widget
                val widgetSize = widgetData["size"] as? String ?: "medium"
                val maxStops = if (multipleEntriesPerVariant) {
                    PeekTransitConstants.getMaxStopsAllowedForMultipleEntries(widgetSize)
                } else {
                    PeekTransitConstants.getMaxStopsAllowed(widgetSize)
                }
                
                val maxVariants = if (multipleEntriesPerVariant) {
                    PeekTransitConstants.getMaxVariantsAllowedForMultipleEntries(widgetSize)
                } else {
                    PeekTransitConstants.getMaxVariantsAllowed(widgetSize)
                }
                
                val generatedStops = mutableListOf<Stop>()
                
                repeat(maxStops) { stopIndex ->
                    val selectedVariants = mutableListOf<Variant>()
                    
                    repeat(maxVariants) {
                        val variant = Variant(
                            key = PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER,
                            name = PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER
                        )
                        selectedVariants.add(variant)
                        
                        if (multipleEntriesPerVariant) {
                            previewSchedules.add(generatePreviewEntry(
                                PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER,
                                PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER,
                                stringToUseBasedOnTimeFormat,
                                "X(X) ${PeekTransitConstants.MINUTES_REMAINING_TEXT}"
                            ))
                            previewSchedules.add(generatePreviewEntry(
                                PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER,
                                PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER,
                                PeekTransitConstants.OK_STATUS_TEXT,
                                "HH:MM ${PeekTransitConstants.GLOBAL_AM_TEXT}/${PeekTransitConstants.GLOBAL_PM_TEXT}"
                            ))
                        } else {
                            previewSchedules.add(generatePreviewEntry(
                                PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER,
                                PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER,
                                stringToUseBasedOnTimeFormat,
                                timeFormatTextToUse
                            ))
                        }
                    }
                    
                    val stop = Stop(
                        key = stopIndex,
                        name = PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER,
                        number = -1,
                        direction = "N",
                        variants = selectedVariants
                    )
                    
                    generatedStops.add(stop)
                }
                
                updatedWidgetData["stops"] = generatedStops
            }
        } else {
            // No configuration - generate placeholder data
            val widgetSize = widgetData["size"] as? String ?: "medium"
            val maxStops = if (multipleEntriesPerVariant) {
                PeekTransitConstants.getMaxStopsAllowedForMultipleEntries(widgetSize)
            } else {
                PeekTransitConstants.getMaxStopsAllowed(widgetSize)
            }
            
            val maxVariants = if (multipleEntriesPerVariant) {
                PeekTransitConstants.getMaxVariantsAllowedForMultipleEntries(widgetSize)
            } else {
                PeekTransitConstants.getMaxVariantsAllowed(widgetSize)
            }
            
            val generatedStops = mutableListOf<Stop>()
            
            repeat(maxStops) { stopIndex ->
                val selectedVariants = mutableListOf<Variant>()
                
                repeat(maxVariants) {
                    val variant = Variant(
                        key = PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER,
                        name = PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER
                    )
                    selectedVariants.add(variant)
                    
                    if (multipleEntriesPerVariant) {
                        previewSchedules.add(generatePreviewEntry(
                            PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER,
                            PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER,
                            stringToUseBasedOnTimeFormat,
                            "X(X) ${PeekTransitConstants.MINUTES_REMAINING_TEXT}"
                        ))
                        previewSchedules.add(generatePreviewEntry(
                            PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER,
                            PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER,
                            PeekTransitConstants.OK_STATUS_TEXT,
                            "HH:MM ${PeekTransitConstants.GLOBAL_AM_TEXT}/${PeekTransitConstants.GLOBAL_PM_TEXT}"
                        ))
                    } else {
                        previewSchedules.add(generatePreviewEntry(
                            PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER,
                            PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER,
                            stringToUseBasedOnTimeFormat,
                            timeFormatTextToUse
                        ))
                    }
                }
                
                val stop = Stop(
                    key = stopIndex,
                    name = PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER,
                    number = -1,
                    direction = "N",
                    variants = selectedVariants
                )
                
                generatedStops.add(stop)
            }
            
            updatedWidgetData["stops"] = generatedStops
            updatedWidgetData["isClosestStop"] = true
            updatedWidgetData["noSelectedVariants"] = false
        }
        
        updatedWidgetData["showLastUpdatedStatus"] = showLastUpdatedStatus
        
        return PreviewResult(
            scheduleData = if (previewSchedules.isEmpty()) null else previewSchedules,
            widgetData = updatedWidgetData
        )
    }
    
    private fun generatePreviewEntry(
        routeKey: String,
        routeName: String,
        status: String,
        time: String
    ): String {
        val separator = PeekTransitConstants.SCHEDULE_STRING_SEPARATOR
        return "$routeKey$separator$routeName$separator$status$separator$time"
    }
    
    fun generatePreviewData(widgetConfiguration: WidgetConfiguration): Map<String, Any> {
        val widgetData = widgetConfiguration.toWidgetData().toMutableMap()
        
        // Add preview-specific data if needed
        if (widgetConfiguration.isClosestStop && widgetConfiguration.stops.isEmpty()) {
            // For closest stop widgets without selected stops, add placeholder stops
            val placeholderStops = generatePlaceholderStops(widgetConfiguration.size)
            widgetData["stops"] = placeholderStops
        } else {
            widgetData["stops"] = widgetConfiguration.stops
        }
        
        return widgetData
    }
    
    fun generatePlaceholderStops(widgetSize: String): List<Stop> {
        val maxStops = PeekTransitConstants.getMaxStopsAllowed(widgetSize)
        val stops = mutableListOf<Stop>()
        
        repeat(maxStops) { i ->
            stops.add(Stop(
                key = i,
                name = "Bus Stop ${i + 1}",
                number = 10000 + i,
                direction = "Direction"
            ))
        }
        
        return stops
    }
    
    fun getWidgetSize(sizeString: String): String {
        return when (sizeString.lowercase()) {
            "small" -> "small"
            "large" -> "large"
            "lockscreen" -> "lockscreen"
            else -> "medium"
        }
    }
}

