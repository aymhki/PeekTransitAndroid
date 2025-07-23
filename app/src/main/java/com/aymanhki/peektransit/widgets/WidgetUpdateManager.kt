package com.aymanhki.peektransit.workers

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
import com.aymanhki.peektransit.widgets.PeekTransitLargeWidgetProvider
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

    fun startUpdates(
        context: Context,
        debugging: Boolean = false,
        userOptedInForManualUpdates: Boolean = false,
        userOptedInForManualUpdatesInLowPower: Boolean = false,
        debugIntervalMinutes: Int = 1
    ) {
        preferDebugMode = debugging
        this.userOptedInForManualUpdates = userOptedInForManualUpdates
        this.userOptedInForManualUpdatesInLowPower = userOptedInForManualUpdatesInLowPower
        this.debugUpdateIntervalMinutes = debugIntervalMinutes
        stopUpdates(context)
        registerBatteryReceiver(context)
        updateBasedOnBatteryStatusAndUserPerfrence(context)
        Log.d(TAG, "Started widget updates, preferred mode: ${if(debugging) "DEBUG" else "PRODUCTION"}")
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

        if (shouldUseAlarmBasedWorkManager != isCurrentlyInDebugMode) {
            isCurrentlyInDebugMode = shouldUseAlarmBasedWorkManager

            if (shouldUseAlarmBasedWorkManager) {
                Log.d(TAG, "Switching to debug mode updates")
                stopProductionModeUpdates(context)
                startDebugModeUpdates(context)
            } else {
                Log.d(TAG, "Switching to production mode updates")
                stopAlarmUpdates(context)
                startProductionModeUpdates(context)
            }
        }
    }

    private fun isLowBattery(context: Context): Boolean {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = level * 100 / scale

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

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
        Log.d(TAG, "Starting debug mode updates every $debugUpdateIntervalMinutes minute(s)")

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
    }

    private fun startProductionModeUpdates(context: Context) {
        Log.d(TAG, "Starting production mode updates")

        val updateRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            updateRequest
        )
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
        PeekTransitConstants.triggerWidgetUpdateUsingProvider(
            context,
            PeekTransitLargeWidgetProvider::class.java
        )
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

    class WidgetUpdateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "Debug mode widget update triggered")
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
        Log.d(TAG, "Performing common widget update work")
    }
}