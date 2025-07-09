package com.aymanhki.peektransit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aymanhki.peektransit.managers.SettingsManager
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.StopViewTheme
import com.aymanhki.peektransit.utils.WidgetPreviewHelper
import com.aymanhki.peektransit.utils.FontUtils
import com.aymanhki.peektransit.data.models.Stop
import com.aymanhki.peektransit.data.models.WidgetModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WidgetPreviewView(
    widgetData: Map<String, Any>,
    scheduleData: List<String>? = null,
    widgetSize: String? = null,
    theme: StopViewTheme? = null,
    lastUpdated: Date? = null,
    fullyLoaded: Boolean = true,
    isLoading: Boolean = false,
    forPreview: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }

    // Extract configuration from widget data
    val actualWidgetSize = widgetSize ?: (widgetData["size"] as? String) ?: "medium"
    val showLastUpdatedStatus = widgetData["showLastUpdatedStatus"] as? Boolean ?: true
    val timeFormat = widgetData["timeFormat"] as? String ?: "minutes"
    val multipleEntriesPerVariant = widgetData["multipleEntriesPerVariant"] as? Boolean ?: true

    // Use provided theme or get from settings
    val currentTheme = theme ?: settingsManager.stopViewTheme

    // Use provided schedule data or generate preview data
    val (finalScheduleData, finalWidgetData) = if (scheduleData != null) {
        scheduleData to widgetData
    } else {
        // Generate preview data using the same helper as iOS
        val previewResult = WidgetPreviewHelper.generatePreviewSchedule(
            widgetData = widgetData,
            noConfig = false,
            timeFormat = timeFormat,
            showLastUpdatedStatus = showLastUpdatedStatus,
            multipleEntriesPerVariant = multipleEntriesPerVariant,
            showLateTextStatus = true
        )

        (previewResult?.scheduleData ?: emptyList()) to (previewResult?.widgetData ?: widgetData)
    }

    val backgroundColor = when (currentTheme) {
        StopViewTheme.CLASSIC -> Color.Black
        StopViewTheme.MODERN -> Color.Transparent // Match LiveBusStopScreen arrival card background
    }

    Box(
        modifier = modifier
            .size(
                width = PeekTransitConstants.getWidgetPreviewWidthForSize(actualWidgetSize, context).dp,
                height = PeekTransitConstants.getWidgetPreviewHeightForSize(actualWidgetSize, context).dp
            )
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        // Use a preview-specific composable that mimics DynamicWidgetView behavior
        PreviewDynamicWidgetView(
            widgetData = finalWidgetData,
            scheduleData = finalScheduleData,
            widgetSize = actualWidgetSize,
            theme = currentTheme,
            lastUpdated = lastUpdated ?: Date(),
            fullyLoaded = fullyLoaded,
            isLoading = isLoading
        )
    }
}


@Composable
private fun PreviewDynamicWidgetView(
    widgetData: Map<String, Any>,
    scheduleData: List<String>,
    widgetSize: String,
    theme: StopViewTheme,
    lastUpdated: Date? = null,
    fullyLoaded: Boolean = true,
    isLoading: Boolean = false
) {
    val config = WidgetModel.parseWidgetData(widgetData)
    val stops = widgetData["stops"] as? List<Stop> ?: emptyList()

    val backgroundColor = when (theme) {
        StopViewTheme.CLASSIC -> Color.Black
        StopViewTheme.MODERN -> Color.Transparent // Match LiveBusStopScreen arrival card background
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(vertical = 8.dp, horizontal = 12.dp),
    ) {
        when {
            (scheduleData.isEmpty() || widgetData.isEmpty() || scheduleData.all { it.isBlank() }) -> {
                // No data available - show "Open app" message
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Open app",
                        color = Color.Red,
                        fontSize = PeekTransitConstants.getNormalFontSizeForWidgetSize(widgetSize).sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            stops.isEmpty() && !config.isClosestStop -> {
                // No stops configured
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No stops selected",
                        color = when (theme) {
                            StopViewTheme.CLASSIC -> PeekTransitConstants.CLASSIC_THEM_TEXT_COLOR
                            StopViewTheme.MODERN -> MaterialTheme.colorScheme.onBackground
                        },
                        fontSize = PeekTransitConstants.getNormalFontSizeForWidgetSize(widgetSize).sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = when (theme) {
                            StopViewTheme.CLASSIC -> FontUtils.LCDDotFontFamily
                            StopViewTheme.MODERN -> FontUtils.CourierFontFamily
                        }
                    )
                }
            }

            stops.isEmpty() && config.isClosestStop -> {
                // Location-based widget with no stops yet
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isLoading) "Loading..." else "No nearby stops",
                        color = when (theme) {
                            StopViewTheme.CLASSIC -> PeekTransitConstants.CLASSIC_THEM_TEXT_COLOR
                            StopViewTheme.MODERN -> MaterialTheme.colorScheme.onBackground
                        },
                        fontSize = PeekTransitConstants.getNormalFontSizeForWidgetSize(widgetSize).sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = when (theme) {
                            StopViewTheme.CLASSIC -> FontUtils.LCDDotFontFamily
                            StopViewTheme.MODERN -> FontUtils.CourierFontFamily
                        }
                    )
                }
            }

            else -> {
                // This Column holds the stops and pushes the "last updated" text down
                Column(modifier = Modifier.weight(1f)) {
                    val maxStops = PeekTransitConstants.getMaxStopsAllowed(widgetSize)
                    val stopsToShow = stops.take(maxStops)

                    stopsToShow.forEachIndexed { stopIndex, stop ->
                        PreviewWidgetStopView(
                            // ✅ FIX #1: Each stop is given a weight, making it expand to fill its share of space.
                            modifier = Modifier.weight(1f),
                            stop = stop,
                            scheduleData = scheduleData,
                            selectedVariants = config.selectedVariants[stop.number.toString()]
                                ?: emptyList(),
                            widgetSize = widgetSize,
                            theme = theme,
                            multipleEntriesPerVariant = config.multipleEntriesPerVariant,
                            noSelectedVariants = config.noSelectedVariants,
                            isFirst = stopIndex == 0,
                            fullyLoaded = fullyLoaded
                        )
                    }
                }

                // "Last Updated" view will always be at the bottom
                if (config.showLastUpdatedStatus && lastUpdated != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    PreviewLastUpdatedView(
                        lastUpdated = lastUpdated,
                        widgetSize = widgetSize,
                        theme = theme,
                        fullyLoaded = fullyLoaded,
                        isLoading = isLoading
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewWidgetStopView(
    modifier: Modifier = Modifier, // Accept modifier from parent
    stop: Stop,
    scheduleData: List<String>,
    selectedVariants: List<Any>,
    widgetSize: String,
    theme: StopViewTheme,
    multipleEntriesPerVariant: Boolean,
    noSelectedVariants: Boolean,
    isFirst: Boolean = false,
    fullyLoaded: Boolean = true
) {
    val stopNamePrefixSize = PeekTransitConstants.getStopNameMaxPrefixLengthForWidget()
    val stopName = stop.name
    val stopNumber = stop.number
    val stopNamePrefix =
        if (stopName.length > stopNamePrefixSize) "${stopName.take(stopNamePrefixSize)}..." else stopName

    val textColor = when (theme) {
        StopViewTheme.CLASSIC -> PeekTransitConstants.CLASSIC_THEM_TEXT_COLOR
        StopViewTheme.MODERN -> MaterialTheme.colorScheme.onBackground
    }

    // Apply the modifier here to make this Column expand
    Column(modifier = modifier.fillMaxWidth()) {
        // Stop Header
        if (widgetSize != "lockscreen" &&
            (!(widgetSize == "small" && !multipleEntriesPerVariant) ||
                    widgetSize == "small" && scheduleData.size <= 1) &&
            fullyLoaded
        ) {

            val stopHeaderText = when (widgetSize) {
                "small" -> "• $stopNamePrefix - ${if (stopNumber == -1) PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER else stopNumber}"
                "large" -> "• $stopNamePrefix - ${if (stopNumber == -1) PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER else stopNumber}"
                else -> "• $stopNamePrefix - ${if (stopNumber == -1) PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER else stopNumber}"
            }

            Text(
                text = stopHeaderText,
                color = textColor,
                fontSize = PeekTransitConstants.getStopNameFontSizeForWidgetSize(widgetSize).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = when (theme) {
                    StopViewTheme.CLASSIC -> FontUtils.LCDDotFontFamily
                    StopViewTheme.MODERN -> FontUtils.CourierFontFamily
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = when (widgetSize) {
                            "small", "large" -> 4.dp
                            else -> 1.dp
                        },
                    )
            )
        }

        // Schedule Entries
        if (stop.variants.isNotEmpty()) {
            val maxSchedules = if (multipleEntriesPerVariant) {
                PeekTransitConstants.getMaxVariantsAllowedForMultipleEntries(widgetSize)
            } else {
                PeekTransitConstants.getMaxVariantsAllowed(widgetSize)
            }
            val variantsToProcess = stop.variants.take(maxSchedules)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                variantsToProcess.forEachIndexed { variantIndex, variant ->
                    val key = variant.getRouteKey()
                    val variantName = variant.name

                    // Find matching schedules for this variant
                    val matchingSchedules = scheduleData.filter { scheduleString ->
                        val components =
                            scheduleString.split(PeekTransitConstants.SCHEDULE_STRING_SEPARATOR)
                        components.size >= 2 &&
                                components[0] == key &&
                                components[1] == variantName
                    }

                    // Determine how many schedules to show
                    val schedulesToShow = if (multipleEntriesPerVariant) {
                        matchingSchedules.take(2)
                    } else {
                        matchingSchedules.take(1)
                    }

                    schedulesToShow.forEachIndexed { scheduleIndex, schedule ->
                        PreviewBusScheduleRowView(
                            scheduleEntry = schedule,
                            widgetSize = widgetSize,
                            theme = theme,
                            isFirst = variantIndex == 0 && scheduleIndex == 0
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewBusScheduleRowView(
    scheduleEntry: String,
    widgetSize: String,
    theme: StopViewTheme,
    isFirst: Boolean = false
) {
    val components = scheduleEntry.split(PeekTransitConstants.SCHEDULE_STRING_SEPARATOR)
    if (components.size < 4) return

    val routeNumber = components[0]
    val routeName = components[1]
    val status = components[2]
    val time = components[3]

    val textColor = when (theme) {
        StopViewTheme.CLASSIC -> PeekTransitConstants.CLASSIC_THEM_TEXT_COLOR
        StopViewTheme.MODERN -> MaterialTheme.colorScheme.onBackground
    }

    val statusColor = when (theme) {
        StopViewTheme.CLASSIC -> textColor
        StopViewTheme.MODERN -> when (status) {
            PeekTransitConstants.LATE_STATUS_TEXT -> MaterialTheme.colorScheme.error
            PeekTransitConstants.EARLY_STATUS_TEXT -> MaterialTheme.colorScheme.primary
            PeekTransitConstants.CANCELLED_STATUS_TEXT -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Route Number
        Text(
            text = routeNumber,
            color = textColor,
            fontSize = PeekTransitConstants.getNormalFontSizeForWidgetSize(widgetSize).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = when (theme) {
                StopViewTheme.CLASSIC -> FontUtils.LCDDotFontFamily
                StopViewTheme.MODERN -> FontUtils.CourierFontFamily
            },
            modifier = Modifier.width(PeekTransitConstants.getRouteNumberWidth(widgetSize).dp)
        )

        // Route Name
        if (routeName.isNotEmpty()) {
            val displayRouteName = if (widgetSize != "small" && widgetSize != "lockscreen") {
                routeName
            } else {
                "${routeName.take(1)}."
            }

            Text(
                text = displayRouteName,
                color = textColor,
                fontSize = PeekTransitConstants.getNormalFontSizeForWidgetSize(widgetSize).sp,
                fontFamily = when (theme) {
                    StopViewTheme.CLASSIC -> FontUtils.LCDDotFontFamily
                    StopViewTheme.MODERN -> FontUtils.CourierFontFamily
                },
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier.width(PeekTransitConstants.getRouteNameWidth(widgetSize).dp)
            )
        }

        // Spacer to push content to the sides
        Spacer(modifier = Modifier.weight(1f))

        // Status
        if (status == PeekTransitConstants.LATE_STATUS_TEXT ||
            status == PeekTransitConstants.EARLY_STATUS_TEXT ||
            status == PeekTransitConstants.CANCELLED_STATUS_TEXT
        ) {

            val statusText = if ((widgetSize == "small" || widgetSize == "lockscreen") &&
                status != PeekTransitConstants.CANCELLED_STATUS_TEXT
            ) {
                "${status.take(1)}."
            } else {
                status
            }

            Text(
                text = statusText,
                color = statusColor,
                fontSize = PeekTransitConstants.getNormalFontSizeForWidgetSize(widgetSize).sp,
                fontWeight = FontWeight.Medium,
                fontFamily = when (theme) {
                    StopViewTheme.CLASSIC -> FontUtils.LCDDotFontFamily
                    StopViewTheme.MODERN -> FontUtils.CourierFontFamily
                }
            )
        }

        // Time
        if (status != PeekTransitConstants.CANCELLED_STATUS_TEXT) {
            Spacer(modifier = Modifier.width(8.dp)) // Maintain some space between status and time
            Text(
                text = time,
                color = if (time == PeekTransitConstants.DUE_STATUS_TEXT) statusColor else textColor,
                fontSize = PeekTransitConstants.getNormalFontSizeForWidgetSize(widgetSize).sp,
                fontWeight = if (time == PeekTransitConstants.DUE_STATUS_TEXT) FontWeight.Bold else FontWeight.Normal,
                fontFamily = when (theme) {
                    StopViewTheme.CLASSIC -> FontUtils.LCDDotFontFamily
                    StopViewTheme.MODERN -> FontUtils.CourierFontFamily
                },
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PreviewLastUpdatedView(
    lastUpdated: Date,
    widgetSize: String,
    theme: StopViewTheme,
    fullyLoaded: Boolean,
    isLoading: Boolean = false
) {
    val textColor = when (theme) {
        StopViewTheme.CLASSIC -> PeekTransitConstants.CLASSIC_THEM_TEXT_COLOR
        StopViewTheme.MODERN -> MaterialTheme.colorScheme.onBackground
    }

    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeString = timeFormat.format(lastUpdated)

    val displayText = when {
        isLoading -> "Updating..."
        !fullyLoaded -> "Updating..."
        else -> "Updated $timeString"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (widgetSize == "lockscreen") 0.dp else 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            color = textColor,
            fontSize = PeekTransitConstants.getLastSeenFontSizeForWidgetSize(widgetSize).sp,
            fontFamily = when (theme) {
                StopViewTheme.CLASSIC -> FontUtils.LCDDotFontFamily
                StopViewTheme.MODERN -> FontUtils.CourierFontFamily
            }
        )
    }
}
