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
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.PeekTransitConstants.FLEXIABLE_WIDGET_UPDATE_WORKER_INTERVAL_IN_MINUTES
import com.aymanhki.peektransit.utils.PeekTransitConstants.MAXIMUM_WIDGET_UPDATE_WORKER_INTERVAL_IN_MINUTES
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object WidgetUpdateManager {
    private const val TAG = "WidgetUpdateManager"
    private const val ACTION_UPDATE_WIDGET = "com.aymanhki.peektransit.ACTION_UPDATE_WIDGET"
    private const val REQUEST_CODE = 42
    private const val WORK_NAME = "widget_update_worker"
    private var preferDebugMode = false
    private var debugUpdateIntervalMinutes = 1
    private var LOW_BATTERY_THRESHOLD = 15

    private var isCurrentlyInDebugMode = false
    private var userOptedInForManualUpdates = false
    private var userOptedInForManualUpdatesInLowPower = false
    private var batteryReceiver: BatteryStatusReceiver? = null

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
        PeekTransitConstants.initAPIKey(context)
        stopUpdates(context)

        if (PeekTransitConstants.appHasAnyActiveWidgets(context)) {
            registerBatteryReceiver(context)
            updateBasedOnBatteryStatusAndUserPerfrence(context)

            Log.d(TAG, "Started widget updates")
            Log.d(TAG, "manual updates: $userOptedInForManualUpdates")
            Log.d(TAG, "manual updates in low power: $userOptedInForManualUpdatesInLowPower")
            Log.d(TAG, "update interval: $debugUpdateIntervalMinutes minutes")
            Log.d(TAG, "debug mode is on: $preferDebugMode")
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
        this.debugUpdateIntervalMinutes = debugIntervalMinutes

        if (PeekTransitConstants.appHasAnyActiveWidgets(context)) {
            if (shouldUseAlarmBasedWorkManager) {
                if (!checkAlarmUpdatesAreRunning(context)) {
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
            action = ACTION_UPDATE_WIDGET
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
                Log.d(TAG, "Using manual mode updates")
                stopProductionModeUpdates(context)
                startDebugModeUpdates(context)
            } else {
                Log.d(TAG, "Using auto mode updates")
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
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
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
            action = ACTION_UPDATE_WIDGET
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

        workToUpdateWidget(context)
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

        workToUpdateWidget(context)
    }


    private fun stopProductionModeUpdates(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }


    private fun stopAlarmUpdates(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
            action = ACTION_UPDATE_WIDGET
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }

    fun forceWidgetUpdate(context: Context) {
        PeekTransitConstants.triggerAllWidgetsLooksUpdates(context)
    }

    class BatteryStatusReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED,
                Intent.ACTION_BATTERY_LOW,
                Intent.ACTION_BATTERY_OKAY,
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    Log.d(TAG, "Battery or power save mode changed: ${intent.action}")
                    updateBasedOnBatteryStatusAndUserPerfrence(context)
                }
            }
        }
    }

    class PackageReplacedReceiver : BroadcastReceiver() {

        val actions = listOf(
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_MY_PACKAGE_UNSUSPENDED,
            Intent.ACTION_MY_PACKAGE_SUSPENDED,
            Intent.ACTION_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_CHANGED,
            Intent.ACTION_PACKAGE_DATA_CLEARED,
            Intent.ACTION_PACKAGE_FULLY_REMOVED,
        )

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action in  actions ) {
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
            if (intent.action == ACTION_UPDATE_WIDGET) {
                workToUpdateWidget(context)
                forceWidgetUpdate(context)
            }
        }
    }

    class WidgetUpdateWorker(
        context: Context,
        params: WorkerParameters
    ) : CoroutineWorker(context, params) {

        override suspend fun doWork(): Result {
            Log.d(TAG, "Production mode widget update triggered")
            try {
                workToUpdateWidget(applicationContext)
                forceWidgetUpdate(applicationContext)
                return Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widget", e)
                return Result.retry()
            }
        }
    }

    fun workToUpdateWidget(context: Context) {
        Log.d(TAG, "Doing Widget Core Update Work")
        val allAppWidgetIds = PeekTransitConstants.getAllActiveWidgetIds(context)

        for (appWidgetId in allAppWidgetIds) {
            val widgetConfig = PeekTransitConstants.getWidgetConfigUsingAppWidgetId(context, appWidgetId)
            if (widgetConfig == null) { continue }
            val widgetConfigId = widgetConfig.id
            val finalWidgetScheduleData = PeekTransitConstants.getWidgetSchedule(context, appWidgetId.toString(), widgetConfigId) ?: WidgetSchedule(
                widgetAppId = appWidgetId.toString(),
                widgetConfigId = widgetConfigId,
                userLocationLon = "",
                userLocationLat = "",
                lastUpdatedTime = "",
                scheduleData = mutableMapOf<String, List<String>>()
            )

            val needsBackgroundLocation = widgetConfig.widgetData["isClosestStop"] as? Boolean ?: false
            val lastUpdatedTimeString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
            val widgetSize = widgetConfig.widgetData["size"] as? String ?: "medium"

            if (needsBackgroundLocation) {
                val userLocationLon: Double = 0.0
                val userLocationLat: Double = 0.0

                //TODO: Fetch the user location in the background here

                finalWidgetScheduleData.userLocationLon = "${userLocationLon}"
                finalWidgetScheduleData.userLocationLat = "${userLocationLat}"
            } else {
                // fetch schedules manually for stops saved in the config
            }

            finalWidgetScheduleData.lastUpdatedTime = lastUpdatedTimeString
            PeekTransitConstants.savedWidgetSchedule(context,finalWidgetScheduleData)
        }
    }
}