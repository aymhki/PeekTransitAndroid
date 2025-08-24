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

class TipBannerManager private constructor(private val context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: TipBannerManager? = null
        fun getInstance(context: Context): TipBannerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TipBannerManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val _shouldShowTipBanner = MutableLiveData(false)
    val shouldShowTipBanner: LiveData<Boolean> = _shouldShowTipBanner

    private val _hasShownTipBannerThisSession = MutableLiveData(false)
    val hasShownTipBannerThisSession: LiveData<Boolean> = _hasShownTipBannerThisSession

    private val _wasTipBannerManuallyHidden = MutableLiveData(false)
    val wasTipBannerManuallyHidden: LiveData<Boolean> = _wasTipBannerManuallyHidden

    private val _attemptedToStartUsageTracking = MutableLiveData(false)
    val attemptedToStartUsageTracking: LiveData<Boolean> = _attemptedToStartUsageTracking

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("tip_banner_preferences", Context.MODE_PRIVATE)

    private val tipBannerShowCountKey = "tipBannerShowCount"
    private val tipBannerFirstShownDateKey = "tipBannerFirstShownDate"
    private val tipBannerLastShownDateKey = "tipBannerLastShownDate"
    private val tipBannerUserClickedKey = "tipBannerUserClicked"
    private val tipBannerUsageTrackingCountKey = "tipBannerUsageTrackingCount"

    private var appUsageStartTime: Date? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tipBannerRunnable: Runnable? = null

    fun startTrackingAppUsage() {
        val currentTrackingCount = sharedPreferences.getInt(tipBannerUsageTrackingCountKey, 0)
        val newTrackingCount = currentTrackingCount + 1

        sharedPreferences.edit {
            putInt(tipBannerUsageTrackingCountKey, newTrackingCount)
        }

        _attemptedToStartUsageTracking.value = true

        if (newTrackingCount >= 3) {
            appUsageStartTime = Date()
            startTipBannerTimer()
        }
    }

    fun stopTrackingAppUsage() {
        tipBannerRunnable?.let { mainHandler.removeCallbacks(it) }
        tipBannerRunnable = null
        appUsageStartTime = null
    }

    fun tipBannerWasTapped() {
        _hasShownTipBannerThisSession.value = true
        _shouldShowTipBanner.value = false
        _wasTipBannerManuallyHidden.value = true
        incrementShowCount()

        sharedPreferences.edit {
            putBoolean(tipBannerUserClickedKey, true)
            putLong(tipBannerLastShownDateKey, Date().time)
        }

        val showCount = sharedPreferences.getInt(tipBannerShowCountKey, 0)

        if (showCount >= PeekTransitConstants.MAXIMUM_TIMES_TO_SHOW_TIP_BANNER) {
            resetTipBannerData()
        }
    }

    fun hideTipBanner() {
        if (_wasTipBannerManuallyHidden.value == true) {
            _shouldShowTipBanner.value = false
        }
    }

    private fun startTipBannerTimer() {
        tipBannerRunnable = Runnable {
            checkAndShowTipBanner()
        }
        tipBannerRunnable?.let { runnable ->
            mainHandler.postDelayed(runnable, PeekTransitConstants.USAGE_TIME_TO_SHOW_TIP_BANNER_AFTER_IN_SECONDS * 1000L)
        }
    }

    private fun checkAndShowTipBanner() {
        if (_hasShownTipBannerThisSession.value == true) return
        if (!shouldShowBasedOnRules()) return

        _shouldShowTipBanner.value = true

    }

    private fun shouldShowBasedOnRules(): Boolean {
        val showCount = sharedPreferences.getInt(tipBannerShowCountKey, 0)
        val userHasClicked = sharedPreferences.getBoolean(tipBannerUserClickedKey, false)
        val lastShownDateMillis = sharedPreferences.getLong(tipBannerLastShownDateKey, 0L)
        val currentTrackingCount = sharedPreferences.getInt(tipBannerUsageTrackingCountKey, 0)

        if (currentTrackingCount < 3) {
            return false
        }

        if (userHasClicked && lastShownDateMillis != 0L) {
            val lastShownDate = Date(lastShownDateMillis)
            val calendar = Calendar.getInstance()
            calendar.time = lastShownDate
            calendar.add(Calendar.YEAR, 1)
            val oneYearLater = calendar.time

            return Date().after(oneYearLater) && showCount < PeekTransitConstants.MAXIMUM_TIMES_TO_SHOW_TIP_BANNER
        }

        if (!userHasClicked && showCount < PeekTransitConstants.MAXIMUM_TIMES_TO_SHOW_TIP_BANNER) {
            return true
        }

        return false
    }

    private fun incrementShowCount() {
        val currentCount = sharedPreferences.getInt(tipBannerShowCountKey, 0)
        val newCount = currentCount + 1

        sharedPreferences.edit {
            putInt(tipBannerShowCountKey, newCount)
            if (currentCount == 0) {
                putLong(tipBannerFirstShownDateKey, Date().time)
            }
            putLong(tipBannerLastShownDateKey, Date().time)
        }
    }

    fun resetTipBannerData() {
        sharedPreferences.edit {
            remove(tipBannerShowCountKey)
            remove(tipBannerFirstShownDateKey)
            remove(tipBannerLastShownDateKey)
            remove(tipBannerUserClickedKey)
        }
        _shouldShowTipBanner.value = false
        _hasShownTipBannerThisSession.value = false
        _wasTipBannerManuallyHidden.value = false
    }

    fun getTipBannerStats(): TipBannerStats {
        val showCount = sharedPreferences.getInt(tipBannerShowCountKey, 0)
        val firstShownMillis = sharedPreferences.getLong(tipBannerFirstShownDateKey, 0L)
        val lastShownMillis = sharedPreferences.getLong(tipBannerLastShownDateKey, 0L)
        val userClicked = sharedPreferences.getBoolean(tipBannerUserClickedKey, false)

        val firstShown = if (firstShownMillis != 0L) Date(firstShownMillis) else null
        val lastShown = if (lastShownMillis != 0L) Date(lastShownMillis) else null

        return TipBannerStats(showCount, firstShown, lastShown, userClicked)
    }
}

data class TipBannerStats(
    val showCount: Int,
    val firstShown: Date?,
    val lastShown: Date?,
    val userClicked: Boolean
)
