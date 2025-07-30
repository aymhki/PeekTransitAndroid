package com.aymanhki.peektransit.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aymanhki.peektransit.data.models.Stop
import com.aymanhki.peektransit.data.models.Variant
import com.aymanhki.peektransit.data.models.WidgetModel
import com.aymanhki.peektransit.data.network.WinnipegTransitAPI
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.PeekTransitConstants.FLEXIABLE_WIDGET_UPDATE_WORKER_INTERVAL_IN_MINUTES
import com.aymanhki.peektransit.utils.PeekTransitConstants.MAXIMUM_WIDGET_UPDATE_WORKER_INTERVAL_IN_MINUTES
import com.aymanhki.peektransit.utils.TimeFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import android.location.Location
import kotlinx.coroutines.*

object WidgetUpdateManager {
    private const val TAG = "WidgetUpdateManager"
    private const val REQUEST_CODE = 42
    private const val WORK_NAME = "widget_update_worker"
    private var preferDebugMode = false
    private var debugUpdateIntervalMinutes = 1
    private var LOW_BATTERY_THRESHOLD = 15
    private var isCurrentlyInDebugMode = false
    private var userOptedInForManualUpdates = false
    private var userOptedInForManualUpdatesInLowPower = false
    private var batteryReceiver: BatteryStatusReceiver? = null
    private val api = WinnipegTransitAPI.getInstance()

    fun startCoreUpdates(
        context: Context,
        debugging: Boolean = false,
        userOptedInForManualUpdates: Boolean = true,
        userOptedInForManualUpdatesInLowPower: Boolean = true,
        debugIntervalMinutes: Int = PeekTransitConstants.HOW_OFTEN_TO_UPDATE_WIDGET_IN_DEBUG_MODE_IN_MINUTES_BY_DEFAULT
    ) {
        preferDebugMode = debugging
        this.userOptedInForManualUpdates = userOptedInForManualUpdates
        this.userOptedInForManualUpdatesInLowPower = userOptedInForManualUpdatesInLowPower
        this.debugUpdateIntervalMinutes = debugIntervalMinutes
        stopUpdates(context)

        if (PeekTransitConstants.appHasAnyActiveWidgets(context)) {
            PeekTransitConstants.initAPIKey(context)
            registerBatteryReceiver(context)
            updateBasedOnBatteryStatusAndUserPerfrence(context)

            Log.d(TAG, "Started widget updates, manual updates: ${userOptedInForManualUpdates}, " +
                    "manual updates in low power: ${userOptedInForManualUpdatesInLowPower}, " +
                    "debug interval: $debugUpdateIntervalMinutes minutes, " +
                    "debug mode is on: $preferDebugMode")
        }
    }

    fun startCoreUpdatesIfNeeded(
        context: Context,
        debugging: Boolean = false,
        userOptedInForManualUpdates: Boolean = true,
        userOptedInForManualUpdatesInLowPower: Boolean = true,
        debugIntervalMinutes: Int = PeekTransitConstants.HOW_OFTEN_TO_UPDATE_WIDGET_IN_DEBUG_MODE_IN_MINUTES_BY_DEFAULT
    ) {
        val powerSavingActive = isLowBattery(context) || isPowerSaveMode(context)
        val shouldDoManualUpdates = (debugging || userOptedInForManualUpdates)
        val shouldUseAlarmBasedWorkManager = shouldDoManualUpdates && (!powerSavingActive || userOptedInForManualUpdatesInLowPower)


        if (PeekTransitConstants.appHasAnyActiveWidgets(context)) {
            PeekTransitConstants.initAPIKey(context)

            if (shouldUseAlarmBasedWorkManager) {
                if (!checkAlarmUpdatesAreRunning(context) || this.debugUpdateIntervalMinutes != debugIntervalMinutes) {
                    this.debugUpdateIntervalMinutes = debugIntervalMinutes
                    stopProductionModeUpdates(context)
                    startDebugModeUpdates(context)
                }
            } else {
                if (!checkProductionModeUpdatesAreRunning(context)) {
                    stopAlarmUpdates(context)
                    startProductionModeUpdates(context)
                }
            }
        } else {
            stopUpdates(context)
        }
    }

    fun checkAlarmUpdatesAreRunning(context: Context): Boolean {
        val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
            action = PeekTransitConstants.ACTION_UPDATE_WIDGET
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        return pendingIntent != null
    }

    fun checkProductionModeUpdatesAreRunning(context: Context): Boolean {
        return WorkManager.getInstance(context).getWorkInfosByTag(WORK_NAME).get().any { it.state.isFinished.not() }
    }

    fun stopUpdates(context: Context) {
        stopAlarmUpdates(context)
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        unregisterBatteryReceiver(context)
        Log.d(TAG, "Stopped all widget updates")
    }

    private fun updateBasedOnBatteryStatusAndUserPerfrence(context: Context) {
        val powerSavingActive = isLowBattery(context) || isPowerSaveMode(context)
        val shouldDoManualUpdates = (preferDebugMode || userOptedInForManualUpdates)
        val shouldUseAlarmBasedWorkManager = shouldDoManualUpdates && (!powerSavingActive || userOptedInForManualUpdatesInLowPower)
        isCurrentlyInDebugMode = shouldUseAlarmBasedWorkManager

        if (PeekTransitConstants.appHasAnyActiveWidgets(context)) {
            if (shouldUseAlarmBasedWorkManager) {
                stopProductionModeUpdates(context)
                startDebugModeUpdates(context)
            } else {
                stopAlarmUpdates(context)
                startProductionModeUpdates(context)
            }
        } else {
            stopUpdates(context)
        }
    }

    private fun isLowBattery(context: Context): Boolean {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = level * 100 / scale

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        return (batteryPct <= LOW_BATTERY_THRESHOLD) && !isCharging
    }

    private fun isPowerSaveMode(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isPowerSaveMode
    }

    private fun registerBatteryReceiver(context: Context) {
        if (batteryReceiver != null) return

        val receiver = BatteryStatusReceiver()
        val filter = IntentFilter().apply {
            PeekTransitConstants.batterStatusActions.forEach { action ->
                addAction(action)
            }
        }

        batteryReceiver = receiver
        context.applicationContext.registerReceiver(receiver, filter)
    }

    private fun unregisterBatteryReceiver(context: Context) {
        batteryReceiver?.let {
            try {
                context.applicationContext.unregisterReceiver(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering battery receiver", e)
            }
            batteryReceiver = null
        }
    }

    private fun startDebugModeUpdates(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
            action = PeekTransitConstants.ACTION_UPDATE_WIDGET
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val intervalMillis = debugUpdateIntervalMinutes * 60 * 1000L

        alarmManager.setRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime(),
            intervalMillis,
            pendingIntent
        )

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            workToUpdateWidgetCoreData(context)
            broadcastWidgetLooksUpdate(context)
        }
    }

    private fun startProductionModeUpdates(context: Context) {
        val updateRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            MAXIMUM_WIDGET_UPDATE_WORKER_INTERVAL_IN_MINUTES, TimeUnit.MINUTES,
            FLEXIABLE_WIDGET_UPDATE_WORKER_INTERVAL_IN_MINUTES, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            updateRequest
        )

         CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
             workToUpdateWidgetCoreData(context)
             broadcastWidgetLooksUpdate(context)
         }
    }

    private fun stopProductionModeUpdates(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private fun stopAlarmUpdates(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
            action = PeekTransitConstants.ACTION_UPDATE_WIDGET
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }

    fun broadcastWidgetLooksUpdate(context: Context) {
        PeekTransitConstants.triggerAllWidgetsLooksUpdates(context)
    }

    class BatteryStatusReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action in PeekTransitConstants.batterStatusActions) {
                updateBasedOnBatteryStatusAndUserPerfrence(context)
            }
        }
    }

    class PackageReplacedReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action in PeekTransitConstants.replacePackageUpdateActions ) {
                Log.d("PackageReplacedReceiver", "App updated, restarting widget updates")
                PeekTransitConstants.initAPIKey(context)
                PeekTransitConstants.triggerWidgetCoreUpdatesManagerWithUserSettings(context, true, false)
            } else {
                Log.d("PackageReplacedReceiver", "Received action: ${intent.action}")
            }
        }
    }

    class WidgetUpdateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "Manual mode widget update triggered")
            if (intent.action == PeekTransitConstants.ACTION_UPDATE_WIDGET) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    try {
                        workToUpdateWidgetCoreData(context)
                        broadcastWidgetLooksUpdate(context)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in manual widget update", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    class WidgetUpdateWorker(
        context: Context,
        params: WorkerParameters
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            Log.d(TAG, "Production mode widget update triggered")
            return try {
                workToUpdateWidgetCoreData(applicationContext)
                broadcastWidgetLooksUpdate(applicationContext)
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widget", e)
                Result.retry()
            }
        }
    }

    suspend fun workToUpdateWidgetCoreData(context: Context) {
        val allAppWidgetIds = PeekTransitConstants.getAllActiveWidgetIds(context)

        val widgetsNeedingLocation = mutableListOf<Pair<Int, WidgetModel>>()
        val widgetsNotNeedingLocation = mutableListOf<Pair<Int, WidgetModel>>()

        for (appWidgetId in allAppWidgetIds) {
            val widgetConfig = PeekTransitConstants.getWidgetConfigUsingAppWidgetId(context, appWidgetId)
            if (widgetConfig == null) continue
            val needsBackgroundLocation = widgetConfig.widgetData["isClosestStop"] as? Boolean ?: false

            if (needsBackgroundLocation) {
                widgetsNeedingLocation.add(appWidgetId to widgetConfig)
            } else {
                widgetsNotNeedingLocation.add(appWidgetId to widgetConfig)
            }
        }

        var userLocation: Location? = null
        var locationError: String? = null

        if (widgetsNeedingLocation.isNotEmpty()) {
            val locationResult = WidgetLocationManager.getCurrentLocation(context)
            userLocation = locationResult.first
            locationError = locationResult.second
        }

        for ((appWidgetId, widgetConfig) in widgetsNeedingLocation) {
            processWidget(
                context = context,
                appWidgetId = appWidgetId,
                widgetConfig = widgetConfig,
                userLocation = userLocation,
                locationError = locationError,
                needsBackgroundLocation = true
            )
        }

        for ((appWidgetId, widgetConfig) in widgetsNotNeedingLocation) {
            processWidget(
                context = context,
                appWidgetId = appWidgetId,
                widgetConfig = widgetConfig,
                userLocation = null,
                locationError = null,
                needsBackgroundLocation = false
            )
        }
    }

    private suspend fun processWidget(
        context: Context,
        appWidgetId: Int,
        widgetConfig: WidgetModel,
        userLocation: Location?,
        locationError: String?,
        needsBackgroundLocation: Boolean
    ) {
        val widgetConfigId = widgetConfig.id
        val finalWidgetScheduleData = PeekTransitConstants.getWidgetSchedule(
            context,
            appWidgetId.toString(),
            widgetConfigId
        ) ?: WidgetSchedule(
            widgetAppId = appWidgetId.toString(),
            widgetConfigId = widgetConfigId,
            userLocationLon = "",
            userLocationLat = "",
            lastUpdatedTime = "",
            scheduleData = mutableMapOf<String, List<String>>(),
            errorMsg = ""
        )

        val thisIsTheFirstUpdateForTheWidget = finalWidgetScheduleData.lastUpdatedTime.isEmpty()
        val newErrorMsg = checkForWidgetErrors(
            context = context,
            appWidgetId = appWidgetId.toString(),
            widgetConfig = widgetConfig,
            widgetScheduleData = finalWidgetScheduleData
        )

        if (newErrorMsg.isNotEmpty()) {
            finalWidgetScheduleData.errorMsg = newErrorMsg
            PeekTransitConstants.savedWidgetSchedule(context, finalWidgetScheduleData)
            return
        } else {
            finalWidgetScheduleData.errorMsg = ""
        }

        val widgetSize = widgetConfig.widgetData["size"] as? String ?: "medium"

        try {
            if (needsBackgroundLocation) {
                if (userLocation != null) {
                    finalWidgetScheduleData.userLocationLon = "${userLocation.latitude}"
                    finalWidgetScheduleData.userLocationLat = "${userLocation.longitude}"
                    val nearbyStops = api.getNearbyStops(userLocation, PeekTransitConstants.GLOBAL_API_FOR_SHORT_USAGE)
                    val filteredStops = getFilteredStopsForWidget(nearbyStops, widgetConfig)
                    finalWidgetScheduleData.scheduleData = getStopsScheduleData(filteredStops, widgetConfig)
                } else {
                    if (finalWidgetScheduleData.scheduleData.isEmpty() || thisIsTheFirstUpdateForTheWidget) {
                        finalWidgetScheduleData.errorMsg = "Unable to fetch user location. Please check your location settings."
                        if (!locationError.isNullOrEmpty()) {
                            finalWidgetScheduleData.errorMsg += " Error: $locationError"
                        }
                    }
                }
            } else {
                val selectedStops = widgetConfig.widgetData["stops"] as? List<Stop> ?: emptyList()
                val schedules = getStopsScheduleData(selectedStops, widgetConfig)
                finalWidgetScheduleData.scheduleData = schedules
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching schedules for widget $appWidgetId", e)
            if (thisIsTheFirstUpdateForTheWidget) {
                var finalErrorMsg = "An error occurred while fetching schedules. Check your internet connection"
                if (needsBackgroundLocation) {
                    finalErrorMsg += ". And make sure location services are enabled for this widget."
                } else {
                    finalErrorMsg += "."
                }
                finalErrorMsg += " " + estimateWhenTheNextUpdateWillBe(context)
                finalWidgetScheduleData.errorMsg = finalErrorMsg
            }
        }

        var lastUpdatedTimeString: String = finalWidgetScheduleData.lastUpdatedTime

        if (!thisIsTheFirstUpdateForTheWidget && WidgetSchedule.theTwoHaveDifferentSchedules(finalWidgetScheduleData, PeekTransitConstants.getWidgetSchedule(context, appWidgetId.toString(), widgetConfigId))) {
            lastUpdatedTimeString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
        } else if (!thisIsTheFirstUpdateForTheWidget) {
            if (widgetSize == "lockscreen" || widgetSize == "small") {
                if (!lastUpdatedTimeString.contains(" O.")) {
                    lastUpdatedTimeString += " O."
                }
            } else {
                if (!lastUpdatedTimeString.contains(" Old")) {
                    lastUpdatedTimeString += " Old"
                }
            }
        } else if (finalWidgetScheduleData.errorMsg.isEmpty()) {
            lastUpdatedTimeString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
        }

        finalWidgetScheduleData.lastUpdatedTime = lastUpdatedTimeString
        PeekTransitConstants.savedWidgetSchedule(context, finalWidgetScheduleData)
    }

    fun estimateWhenTheNextUpdateWillBe(context: Context): String {
        var toReturn: String

        if (checkAlarmUpdatesAreRunning(context)) {
            val nextUpdateTime = LocalDateTime.now().plusMinutes((debugUpdateIntervalMinutes + 1).toLong() )
            toReturn = "This widget will update again at: ${nextUpdateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}"
        } else if (checkProductionModeUpdatesAreRunning(context)) {
            val minimumNextUpdateTime = LocalDateTime.now().plusMinutes(
                MAXIMUM_WIDGET_UPDATE_WORKER_INTERVAL_IN_MINUTES + 1
            )

            val maximumNextUpdateTime = LocalDateTime.now().plusMinutes(
                MAXIMUM_WIDGET_UPDATE_WORKER_INTERVAL_IN_MINUTES + FLEXIABLE_WIDGET_UPDATE_WORKER_INTERVAL_IN_MINUTES + 1
            )

            toReturn = "This widget will update again between: ${minimumNextUpdateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))} and ${maximumNextUpdateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}"
        } else {
            toReturn = "This widget is not going to update again automatically."
        }

        return toReturn
    }

    suspend fun getStopsScheduleData(stopsToUse: List<Stop>, widgetConfig: WidgetModel): Map<String, List<String>> {
        val toReturn = mutableMapOf<String, List<String>>()

        stopsToUse.forEach { currentStop ->
            val schedules = api.getStopSchedule(currentStop.number)
            val timeFormat = TimeFormat.fromString(widgetConfig.widgetData["timeFormat"] as? String ?: "default")
            val isMultipleEntriesPerVariant  = widgetConfig.widgetData["multipleEntriesPerVariant"]  as? Boolean ?: false
            val isNoSelectedVariants = widgetConfig.widgetData["noSelectedVariants"] as? Boolean ?: false
            val selectedVariantsForThisStop = widgetConfig.widgetData["selectedVariants"] as? Map<String, List<Variant>> ?: emptyMap()
            val isClosestStop = widgetConfig.widgetData["isClosestStop"] as? Boolean ?: false
            val preferredStops = widgetConfig.widgetData["preferredStops"] as? List<Stop> ?: emptyList()
            val cleanedSchedules: List<String>
            val maxVariants: Int

            if (isMultipleEntriesPerVariant) {
                cleanedSchedules = api.cleanScheduleMixedTimeFormat(schedules)
                maxVariants = PeekTransitConstants.getMaxVariantsAllowedForMultipleEntries(widgetConfig.widgetData["size"] as? String ?: "medium")
            } else {
                cleanedSchedules = api.cleanStopSchedule(schedules, timeFormat)
                maxVariants = PeekTransitConstants.getMaxVariantsAllowed(widgetConfig.widgetData["size"] as? String ?: "medium")
            }

            var finalSchedulesForThisStop = emptyList<String>()
            val autoPopulateVariants = isNoSelectedVariants || selectedVariantsForThisStop.isEmpty() || (isClosestStop && preferredStops.isEmpty())

            if (autoPopulateVariants) {
                val selectedVariants = mutableListOf<Variant>()
                var processedVariants = setOf<String>()

                for (scheduleString in cleanedSchedules) {
                    val components = scheduleString.split(PeekTransitConstants.SCHEDULE_STRING_SEPARATOR)

                    if (components.size > 2) {
                        val variantKey = components[0]
                        val variantName = components[1]
                        val variantIdentifier = "${variantKey}${PeekTransitConstants.COMPOSITE_KEY_LINKER_FOR_DICTIONARIES}${variantName}"

                        if (!processedVariants.contains(variantIdentifier)) {
                            val variantEntries = cleanedSchedules.filter {
                                val entryComponents = it.split(PeekTransitConstants.SCHEDULE_STRING_SEPARATOR)
                                return@filter entryComponents.size >= 2 && entryComponents[0] == variantKey && entryComponents[1] == variantName
                            }

                            val entriesToAdd = if (isMultipleEntriesPerVariant) {
                                variantEntries.take(2)
                            } else {
                                variantEntries.take(1)
                            }

                            finalSchedulesForThisStop = finalSchedulesForThisStop.plus(entriesToAdd)

                            selectedVariants.add(Variant(key = variantKey, name = variantName))
                            processedVariants = processedVariants.plus(variantIdentifier)

                            if (selectedVariants.size >= maxVariants) {
                                break
                            }

                            currentStop.selectedVariants = selectedVariants
                        }
                    }
                }
            } else {
                val thisStopVariants = selectedVariantsForThisStop[currentStop.number.toString()] ?: emptyList()

                for (variant in thisStopVariants) {
                    val variantKey = variant.key as? String
                    val variantName = variant.name as? String

                    val matchingSchedules = cleanedSchedules.filter {
                        val components = it.split(PeekTransitConstants.SCHEDULE_STRING_SEPARATOR)
                        return@filter components.size >= 2 && components[0] == variantKey && components[1] == variantName
                    }

                    val entriesToAdd = if (isMultipleEntriesPerVariant) {
                        matchingSchedules.take(2)
                    } else {
                        matchingSchedules.take(1)
                    }

                    finalSchedulesForThisStop = finalSchedulesForThisStop.plus(entriesToAdd)


                }
            }


            if (finalSchedulesForThisStop.size < maxVariants){
                val missingVariantEntries = mutableListOf<Variant>()

                if (autoPopulateVariants) {
                    for (variant in currentStop.selectedVariants) {
                        var variantHasAnEntry = false

                        for (scheduleString in finalSchedulesForThisStop) {
                            val components = scheduleString.split(PeekTransitConstants.SCHEDULE_STRING_SEPARATOR)

                            if (components.size >= 2 && components[0] == variant.key && components[1] == variant.name) {
                                variantHasAnEntry = true
                                break
                            }
                        }

                        if (!variantHasAnEntry) {
                            missingVariantEntries.add(variant)
                        }
                    }
                } else {
                    for (variant in selectedVariantsForThisStop[currentStop.number.toString()] ?: emptyList()) {
                        var variantHasAnEntry = false

                        for (scheduleString in finalSchedulesForThisStop) {
                            val components = scheduleString.split(PeekTransitConstants.SCHEDULE_STRING_SEPARATOR)

                            if (components.size >= 2 && components[0] == variant.key && components[1] == variant.name) {
                                variantHasAnEntry = true
                                break
                            }
                        }

                        if (!variantHasAnEntry) {
                            missingVariantEntries.add(variant)
                        }
                    }
                }

                val difference = maxVariants - finalSchedulesForThisStop.size

                for (i in 0 until difference) {
                    for (variant in missingVariantEntries) {
                        finalSchedulesForThisStop = finalSchedulesForThisStop.plus(
                            variant.key +
                                    PeekTransitConstants.SCHEDULE_STRING_SEPARATOR +
                                    variant.name + PeekTransitConstants.SCHEDULE_STRING_SEPARATOR +
                                    PeekTransitConstants.OK_STATUS_TEXT +
                                    PeekTransitConstants.SCHEDULE_STRING_SEPARATOR +
                                    PeekTransitConstants.TIME_PERIOD_ALLOWED_FOR_NEXT_BUS_ROUTES_IN_HOURS + "hrs+"
                        )
                    }
                }
            }

            if (finalSchedulesForThisStop.size == 0 || finalSchedulesForThisStop.isEmpty()) {
                for (i in 0 until maxVariants) {
                    finalSchedulesForThisStop = finalSchedulesForThisStop.plus(
                        PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER +
                                PeekTransitConstants.SCHEDULE_STRING_SEPARATOR +
                                PeekTransitConstants.WIDGET_TEXT_PLACEHOLDER +
                                PeekTransitConstants.SCHEDULE_STRING_SEPARATOR +
                                PeekTransitConstants.OK_STATUS_TEXT +
                                PeekTransitConstants.SCHEDULE_STRING_SEPARATOR +
                                PeekTransitConstants.TIME_PERIOD_ALLOWED_FOR_NEXT_BUS_ROUTES_IN_HOURS + "hrs+"
                    )
                }
            }


            val stopKey = "${currentStop.name}${PeekTransitConstants.COMPOSITE_KEY_LINKER_FOR_DICTIONARIES}${currentStop.number}"
            toReturn[stopKey] = finalSchedulesForThisStop
        }

        return toReturn
    }

    suspend fun getFilteredStopsForWidget(stops: List<Stop>, widgetConfig: WidgetModel): List<Stop> {
        var filteredStops: List<Stop> = emptyList()
        val seenVariants = mutableSetOf<String>()
        val isMultipleEntriesPerVariant = widgetConfig.widgetData["multipleEntriesPerVariant"] as? Boolean ?: false

        val nearbyStopsDict = mutableMapOf<Int, Stop>()
        for (stop in stops) {
            val number = stop.number as? Int ?: continue
            nearbyStopsDict[number] = stop
        }

        val maxStops: Int = if (isMultipleEntriesPerVariant) {
            PeekTransitConstants.getMaxStopsAllowedForMultipleEntries(widgetConfig.widgetData["size"] as? String ?: "medium")
        } else {
            PeekTransitConstants.getMaxStopsAllowed(widgetConfig.widgetData["size"] as? String ?: "medium")
        }

        val preferredVariants = mutableSetOf<String>()

        val preferredStops = widgetConfig.widgetData["preferredStops"] as? List<Stop> ?: emptyList()
        val selectedVariants = widgetConfig.widgetData["selectedVariants"] as? Map<String, List<Variant>> ?: emptyMap()

        if (preferredStops.isNotEmpty()) {
            for (preferredStop in preferredStops) {
                for (variant in selectedVariants[preferredStop.number.toString()] ?: emptyList()) {
                    val variantKey = variant.key as? String
                    val variantName = variant.name as? String

                    if (!variantKey.isNullOrEmpty() && !variantName.isNullOrEmpty()) {
                        val variantIdentifier = "${variantKey}${PeekTransitConstants.COMPOSITE_KEY_LINKER_FOR_DICTIONARIES}${variantName}"

                        if (!preferredVariants.contains(variantIdentifier)) {
                            preferredVariants.add(variantIdentifier)
                        }
                    }
                }
            }
        }

        if (preferredStops.isNotEmpty()) {
            for (preferredStop in preferredStops) {
                if (filteredStops.size >= maxStops) {
                    break
                }

                try {
                    var matchingNearbyStop = nearbyStopsDict[preferredStop.number as? Int]

                    if (matchingNearbyStop != null) {
                        matchingNearbyStop = preferredStop

                        val schedule = api.getStopSchedule(matchingNearbyStop.number)
                        val cleanedSchedule = api.cleanStopSchedule(schedule, TimeFormat.DEFAULT)
                        val currentStopVariants = mutableSetOf<String>()

                        for (scheduleString in cleanedSchedule) {
                            val components =
                                scheduleString.split(PeekTransitConstants.SCHEDULE_STRING_SEPARATOR)

                            if (components.size >= 2) {
                                val variantKey = components[0]
                                val variantName = components[1]
                                val variantIdentifier = "${variantKey}${PeekTransitConstants.COMPOSITE_KEY_LINKER_FOR_DICTIONARIES}${variantName}"
                                currentStopVariants.add(variantIdentifier)
                            }
                        }

                        val uniqueVariants = currentStopVariants.subtract(seenVariants)

                        if (uniqueVariants.isNotEmpty()) {
                            filteredStops = filteredStops.plus(matchingNearbyStop)
                            seenVariants.addAll(currentStopVariants)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WidgetUpdateManager", "Error getting stop schedule", e)
                    continue
                }
            }
        }

        if (filteredStops.size < maxStops && preferredVariants.isNotEmpty()) {
            val processedStopNumbers = mutableSetOf<Int>()

            for (filteredStop in filteredStops) {
                processedStopNumbers.add(filteredStop.number as? Int ?: continue)
            }

            for (stop in stops) {
                if (processedStopNumbers.contains(stop.number)) {
                    continue
                }

                try {
                    val schedule = api.getStopSchedule(stop.number)
                    val cleanedSchedule = api.cleanStopSchedule(schedule, TimeFormat.DEFAULT)

                    val stopVariants = mutableSetOf<String>()

                    for (scheduleString in cleanedSchedule) {
                        val components = scheduleString.split(PeekTransitConstants.SCHEDULE_STRING_SEPARATOR)

                        if (components.size >= 2) {
                            val variantKey = components[0]
                            val variantName = components[1]
                            val variantIdentifier = "${variantKey}${PeekTransitConstants.COMPOSITE_KEY_LINKER_FOR_DICTIONARIES}${variantName}"
                            stopVariants.add(variantIdentifier)
                        }

                    }

                    val matchingPreferredVariants = stopVariants.intersect(preferredVariants)

                    if (matchingPreferredVariants.isNotEmpty()) {
                        val updatedStop = stop
                        val selectedVariantsForThisStop = mutableListOf<Variant>()

                        for (variantCombo in matchingPreferredVariants) {
                            val components = variantCombo.split(PeekTransitConstants.COMPOSITE_KEY_LINKER_FOR_DICTIONARIES)

                            if (components.size == 2) {
                                val variant = Variant(key = components[0], name = components[1])
                                selectedVariantsForThisStop.add(variant)
                            }
                        }

                        updatedStop.selectedVariants = selectedVariantsForThisStop
                        filteredStops = filteredStops.plus(updatedStop)
                        seenVariants.addAll(stopVariants)


                    }

                    if (filteredStops.size >= maxStops) {
                        break
                    }

                } catch (e: Exception) {
                    Log.e("WidgetUpdateManager", "Error getting stop schedule", e)
                    continue
                }
            }
        }


        if (filteredStops.size < maxStops) {
            val processedStopNumbers = mutableSetOf<Int>()

            for (filteredStop in filteredStops) {
                processedStopNumbers.add(filteredStop.number as? Int ?: continue)
            }

            for (stop in stops) {
                if (processedStopNumbers.contains(stop.number)) {
                    continue
                }

                try {
                    val schedule = api.getStopSchedule(stop.number)
                    val cleanedSchedule = api.cleanStopSchedule(schedule, TimeFormat.DEFAULT)

                    val stopVariants = mutableSetOf<String>()

                    for (scheduleString in cleanedSchedule) {
                        val components =
                            scheduleString.split(PeekTransitConstants.SCHEDULE_STRING_SEPARATOR)

                        if (components.size >= 2) {
                            val variantKey = components[0]
                            val variantName = components[1]
                            val variantIdentifier = "${variantKey}${PeekTransitConstants.COMPOSITE_KEY_LINKER_FOR_DICTIONARIES}${variantName}"
                            stopVariants.add(variantIdentifier)
                        }

                    }

                    val uniqueVariants = stopVariants.subtract(seenVariants)

                    if (uniqueVariants.isNotEmpty()) {
                        filteredStops = filteredStops.plus(stop)
                        seenVariants.addAll(stopVariants)

                        if (filteredStops.size >= maxStops) {
                            break
                        }
                    }

                } catch (e: Exception) {
                    Log.e("WidgetUpdateManager", "Error getting stop schedule", e)
                    continue
                }
            }
        }

        if (filteredStops.isEmpty()) {
            val usedKeys = mutableSetOf<String>()

            for (stop in stops) {
                val direction = stop.direction
                val street = stop.street
                val streetName = street.name
                val compositeKey = "${direction}${PeekTransitConstants.COMPOSITE_KEY_LINKER_FOR_DICTIONARIES}${streetName}"

                if (!usedKeys.contains(compositeKey)) {
                    usedKeys.add(compositeKey)
                    filteredStops = filteredStops.plus(stop)

                    if (filteredStops.size >= maxStops) {
                        break
                    }
                }
            }

            if (filteredStops.isEmpty()) {
                filteredStops = stops.take(maxStops).toMutableList()
            }
        }

        return filteredStops
    }

    fun checkForWidgetErrors(
        context: Context,
        appWidgetId: String,
        widgetConfig: WidgetModel,
        widgetScheduleData: WidgetSchedule
    ): String {
        var toReturn = ""

        if (toReturn.isEmpty() && widgetConfig.widgetData == null || widgetConfig.widgetData.isEmpty()) {
            toReturn = "Widget Configuration data seems to be empty or invalid."
        }

        if (toReturn.isEmpty() && widgetConfig.widgetData["isClosestStop"] as? Boolean == true) {
            if (!PeekTransitConstants.hasBackgroundLocationPermission(context)) {
                toReturn = "Location permission is required for this widget to function properly."
            }
        }

        if (toReturn.isEmpty() && appWidgetId != widgetScheduleData.widgetAppId || widgetConfig.id != widgetScheduleData.widgetConfigId) {
            toReturn = "Widget Schedule data seems to be corrupted or mismatched."
        }

        return toReturn
    }
}