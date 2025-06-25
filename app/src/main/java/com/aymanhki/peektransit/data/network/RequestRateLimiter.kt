package com.aymanhki.peektransit.data.network

import com.aymanhki.peektransit.utils.PeekTransitConstants
import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class RequestRateLimiter {
    private var lastRequestTime: Long = System.currentTimeMillis()
    private val minimumRequestInterval: Long = (PeekTransitConstants.MINIMUM_REQUEST_INTERVAL * 1000).toLong()
    
    private val callCount = AtomicInteger(0)
    private var minuteStartTime: Long = System.currentTimeMillis()
    private val maxCallsPerMinute: Int = PeekTransitConstants.MAX_CALLS_PER_MINUTE
    
    suspend fun waitIfNeeded() {
        val currentTime = System.currentTimeMillis()
        
        // Check if we need to reset the minute counter
        val timeElapsedSinceMinuteStart = currentTime - minuteStartTime
        if (timeElapsedSinceMinuteStart >= 60_000) {
            callCount.set(0)
            minuteStartTime = currentTime
        }
        
        // Check if we've hit the rate limit
        if (callCount.get() >= maxCallsPerMinute) {
            val timeToWaitForNewMinute = 60_000 - timeElapsedSinceMinuteStart + minimumRequestInterval
            if (timeToWaitForNewMinute > 0) {
                delay(timeToWaitForNewMinute)
                callCount.set(0)
                minuteStartTime = System.currentTimeMillis()
            }
        }
        
        // Ensure minimum interval between requests
        val timeSinceLastRequest = currentTime - lastRequestTime
        if (timeSinceLastRequest < minimumRequestInterval) {
            delay(minimumRequestInterval - timeSinceLastRequest)
        }
        
        callCount.incrementAndGet()
        lastRequestTime = System.currentTimeMillis()
    }
    
    fun getCallStats(): Pair<Int, Long> {
        val currentTime = System.currentTimeMillis()
        val timeElapsed = currentTime - minuteStartTime
        val timeRemaining = maxOf(0, 60_000 - timeElapsed)
        return Pair(callCount.get(), timeRemaining)
    }
    
    fun resetCounter() {
        callCount.set(0)
        minuteStartTime = System.currentTimeMillis()
    }
    
    companion object {
        @Volatile
        private var INSTANCE: RequestRateLimiter? = null
        
        fun getInstance(): RequestRateLimiter {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RequestRateLimiter().also { INSTANCE = it }
            }
        }
    }
}