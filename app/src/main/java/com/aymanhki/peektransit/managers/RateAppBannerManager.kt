package com.aymanhki.peektransit.managers

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.aymanhki.peektransit.utils.PeekTransitConstants
import java.util.*
import androidx.core.content.edit

class RateAppBannerManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: RateAppBannerManager? = null

        fun getInstance(context: Context): RateAppBannerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RateAppBannerManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val _shouldShowRateAppBanner = MutableLiveData(false)
    val shouldShowRateAppBanner: LiveData<Boolean> = _shouldShowRateAppBanner

    private val _hasShownRateAppBannerThisSession = MutableLiveData(false)
    val hasShownRateAppBannerThisSession: LiveData<Boolean> = _hasShownRateAppBannerThisSession

    private val _wasRateAppBannerManuallyHidden = MutableLiveData(false)
    val wasRateAppBannerManuallyHidden: LiveData<Boolean> = _wasRateAppBannerManuallyHidden

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("rate_app_preferences", Context.MODE_PRIVATE)
    private val rateAppShowCountKey = "rateAppShowCount"
    private val rateAppFirstShownDateKey = "rateAppFirstShownDate"
    private val rateAppLastShownDateKey = "rateAppLastShownDate"
    private val rateAppUserClickedKey = "rateAppUserClicked"
    private var appUsageStartTime: Date? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var rateAppRunnable: Runnable? = null

    fun startTrackingAppUsage() {
        appUsageStartTime = Date()
        startRateAppTimer()
    }

    fun stopTrackingAppUsage() {
        rateAppRunnable?.let { mainHandler.removeCallbacks(it) }
        rateAppRunnable = null
        appUsageStartTime = null
    }

    fun rateAppBannerWasTapped() {
        _hasShownRateAppBannerThisSession.value = true
        _shouldShowRateAppBanner.value = false
        _wasRateAppBannerManuallyHidden.value = true

        sharedPreferences.edit {
            putBoolean(rateAppUserClickedKey, true)
        }
    }

    fun hideRateAppBanner() {
        if (_wasRateAppBannerManuallyHidden.value == true) {
            _shouldShowRateAppBanner.value = false
        }
    }

    private fun startRateAppTimer() {
        rateAppRunnable = Runnable {
            checkAndShowRateAppBanner()
        }

        rateAppRunnable?.let { runnable ->
            mainHandler.postDelayed(runnable, PeekTransitConstants.USAGE_TIME_TO_SHOW_RATE_APP_BANNER_AFTER_IN_SECONDS * 1000L)
        }
    }

    private fun checkAndShowRateAppBanner() {
        if (_hasShownRateAppBannerThisSession.value == true) return
        if (!shouldShowBasedOnRules()) return

        _shouldShowRateAppBanner.value = true
        incrementShowCount()

        sharedPreferences.edit {
            putLong(rateAppLastShownDateKey, Date().time)
        }
    }

    private fun shouldShowBasedOnRules(): Boolean {
        val showCount = sharedPreferences.getInt(rateAppShowCountKey, 0)
        val userHasClicked = sharedPreferences.getBoolean(rateAppUserClickedKey, false)
        val lastShownDateMillis = sharedPreferences.getLong(rateAppLastShownDateKey, 0L)
        val firstShownDateMillis = sharedPreferences.getLong(rateAppFirstShownDateKey, 0L)

        if (showCount == 0) {
            return true
        }

        if (userHasClicked) {
//            if (firstShownDateMillis != 0L) {
//                val firstShownDate = Date(firstShownDateMillis)
//                val calendar = Calendar.getInstance()
//                calendar.time = firstShownDate
//                calendar.add(Calendar.YEAR, 1)
//                val oneYearLater = calendar.time
//
//                return Date().after(oneYearLater) && showCount < PeekTransitConstants.MAXIMUM_TIMES_TO_SHOW_RATE_APP_BANNER
//            }

            return false
        }

        if (lastShownDateMillis != 0L) {
            val lastShownDate = Date(lastShownDateMillis)
            val calendar = Calendar.getInstance()
            calendar.time = lastShownDate
            calendar.add(Calendar.MONTH, 4)
            val fourMonthsLater = calendar.time

            return Date().after(fourMonthsLater) && showCount < PeekTransitConstants.MAXIMUM_TIMES_TO_SHOW_RATE_APP_BANNER
        }

        return false
    }

    private fun incrementShowCount() {
        val currentCount = sharedPreferences.getInt(rateAppShowCountKey, 0)
        val newCount = currentCount + 1

        sharedPreferences.edit {

            if (currentCount == 0) {
                putLong(rateAppFirstShownDateKey, Date().time)
            }

            putInt(rateAppShowCountKey, newCount)
        }
    }

    fun resetRateAppBannerData() {
        sharedPreferences.edit {
            remove(rateAppShowCountKey)
                .remove(rateAppFirstShownDateKey)
                .remove(rateAppLastShownDateKey)
                .remove(rateAppUserClickedKey)
        }

        _shouldShowRateAppBanner.value = false
        _hasShownRateAppBannerThisSession.value = false
        _wasRateAppBannerManuallyHidden.value = false
    }

    fun getRateAppBannerStats(): RateAppStats {
        val showCount = sharedPreferences.getInt(rateAppShowCountKey, 0)
        val firstShownMillis = sharedPreferences.getLong(rateAppFirstShownDateKey, 0L)
        val lastShownMillis = sharedPreferences.getLong(rateAppLastShownDateKey, 0L)
        val userClicked = sharedPreferences.getBoolean(rateAppUserClickedKey, false)

        val firstShown = if (firstShownMillis != 0L) Date(firstShownMillis) else null
        val lastShown = if (lastShownMillis != 0L) Date(lastShownMillis) else null

        return RateAppStats(showCount, firstShown, lastShown, userClicked)
    }
}

data class RateAppStats(
    val showCount: Int,
    val firstShown: Date?,
    val lastShown: Date?,
    val userClicked: Boolean
)
