package com.aymanhki.peektransit.utils

import com.aymanhki.peektransit.data.models.Stop
import com.aymanhki.peektransit.data.models.Variant

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
                val stops = widgetData["stops"] as? List<*> ?: return null
                
                for (stop in stops) {
                    val noSelectedVariants = widgetData["noSelectedVariants"] as? Boolean ?: false

                    if (stop !is Stop) continue

                    if (!noSelectedVariants) {
                        val variants = stop.variants
                        
                        for (variant in variants) {
                            val key = variant.getRouteKey()
                            val name = variant.name
                            
                            if (multipleEntriesPerVariant) {
                                previewSchedules.add(generatePreviewEntry(
                                    key,
                                    name,
                                    stringToUseBasedOnTimeFormat,
                                    "X(X) ${PeekTransitConstants.MINUTES_REMAINING_TEXT}"
                                ))
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

}

