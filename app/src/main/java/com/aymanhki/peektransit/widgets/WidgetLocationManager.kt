package com.aymanhki.peektransit.widgets

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager as AndroidLocationManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.google.android.gms.location.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.resume

object WidgetLocationManager {
    private const val TAG = "WidgetLocationManager"
    private const val QUICK_LOCATION_TIMEOUT_MS = PeekTransitConstants.QUICK_LOCATION_TIMEOUT_MS
    private const val PROGRESSIVE_LOCATION_TIMEOUT_MS = PeekTransitConstants.PROGRESSIVE_LOCATION_TIMEOUT_MS
    private const val MAX_RETRY_ATTEMPTS = 2

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var lastKnownLocation: Location? = null
    private var lastError: String? = null
    private var isRequestingLocation = false

    suspend fun getCurrentLocation(context: Context, forceRefresh: Boolean = false): Pair<Location?, String?> {
        if (!hasLocationPermission(context)) {
            val error = "Location permission not granted"
            return Pair(lastKnownLocation, error)
        }

        if (!isLocationEnabled(context)) {
            val error = "Location services are disabled"
            return Pair(lastKnownLocation, error)
        }

        initializeLocationClient(context)

        val location = if (forceRefresh) {
            requestProgressiveLocation(context)
        } else {
            getCachedLocation(context)?.takeIf { isLocationRecent(it) } ?: requestProgressiveLocation(context)
        }

        val finalLocation = location ?: getFallbackLocation(context)

        return if (finalLocation != null) {
            lastKnownLocation = finalLocation
            lastError = null
            Pair(finalLocation, null)
        } else {
            val error = lastError ?: "Failed to obtain location"
            Pair(lastKnownLocation, error)
        }
    }

    private suspend fun requestProgressiveLocation(context: Context): Location? = coroutineScope {
        if (!hasLocationPermission(context)) return@coroutineScope null

        if (isRequestingLocation) {
            Log.d(TAG, "Already requesting location, skipping duplicate request")
            return@coroutineScope null
        }

        val quickLocationDeferred = async {
            Log.d(TAG, "Phase 1: Requesting quick location")
            requestLocationWithStrategy(
                context = context,
                priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                waitForAccuracy = false,
                timeoutMs = QUICK_LOCATION_TIMEOUT_MS
            )
        }

        val quickLocation = quickLocationDeferred.await()
        if (quickLocation != null && quickLocation.accuracy <= PeekTransitConstants.DISTANCE_CHANGE_ALLOWED_BEFORE_REFRESHING_STOPS_IN_METERS) {
            Log.d(TAG, "Got acceptable quick location: accuracy=${quickLocation.accuracy}m")
            return@coroutineScope quickLocation
        }

        Log.d(TAG, "Phase 2: Requesting higher accuracy location")
        val accurateLocation = requestLocationWithStrategy(
            context = context,
            priority = Priority.PRIORITY_HIGH_ACCURACY,
            waitForAccuracy = false,
            timeoutMs = PROGRESSIVE_LOCATION_TIMEOUT_MS
        )

        return@coroutineScope when {
            accurateLocation != null && quickLocation != null -> {
                if (accurateLocation.accuracy < quickLocation.accuracy) accurateLocation else quickLocation
            }
            accurateLocation != null -> accurateLocation
            quickLocation != null -> quickLocation
            else -> null
        }
    }

    private suspend fun requestLocationWithStrategy(
        context: Context,
        priority: Int,
        waitForAccuracy: Boolean,
        timeoutMs: Long
    ): Location? = withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine { continuation ->
            if (!hasLocationPermission(context)) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            isRequestingLocation = true
            val client = fusedLocationClient
            if (client == null) {
                isRequestingLocation = false
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val locationRequest = LocationRequest.Builder(priority, PeekTransitConstants.LOCATION_UPDATE_INTERVAL_MS)
                .setWaitForAccurateLocation(waitForAccuracy)
                .setMinUpdateIntervalMillis(PeekTransitConstants.LOCATION_REQUEST_MIN_UPDATE_INTERVAL_MS)
                .setMaxUpdateDelayMillis(timeoutMs)
                .setMaxUpdates(1)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    val location = locationResult.lastLocation
                    client.removeLocationUpdates(this)
                    isRequestingLocation = false
                    Log.d(TAG, "Location received: lat=${location?.latitude}, lng=${location?.longitude}, accuracy=${location?.accuracy}")
                    if (!continuation.isCompleted) {
                        continuation.resume(location)
                    }
                }

                override fun onLocationAvailability(availability: LocationAvailability) {
                    if (!availability.isLocationAvailable) {
                        client.removeLocationUpdates(this)
                        isRequestingLocation = false
                        if (!continuation.isCompleted) {
                            continuation.resume(null)
                        }
                    }
                }
            }

            try {
                client.requestLocationUpdates(
                    locationRequest,
                    callback,
                    Looper.getMainLooper()
                )
                continuation.invokeOnCancellation {
                    client.removeLocationUpdates(callback)
                    isRequestingLocation = false
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception requesting location", e)
                lastError = "Location permission revoked"
                isRequestingLocation = false
                if (!continuation.isCompleted) {
                    continuation.resume(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception requesting location", e)
                lastError = e.message
                isRequestingLocation = false
                if (!continuation.isCompleted) {
                    continuation.resume(null)
                }
            }
        }
    }

    private suspend fun getCachedLocation(context: Context): Location? = suspendCancellableCoroutine { continuation ->
        val client = fusedLocationClient
        if (client == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        try {
            client.lastLocation.addOnSuccessListener { location ->
                if (!continuation.isCompleted) {
                    continuation.resume(location)
                }
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Failed to get cached location", exception)
                lastError = exception.message
                if (!continuation.isCompleted) {
                    continuation.resume(null)
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting cached location", e)
            lastError = "Location permission revoked"
            if (!continuation.isCompleted) {
                continuation.resume(null)
            }
        }
    }

    private fun getFallbackLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null

        try {
            val androidLocationManager = context.getSystemService(Context.LOCATION_SERVICE) as AndroidLocationManager

            val gpsLocation = if (androidLocationManager.isProviderEnabled(AndroidLocationManager.GPS_PROVIDER)) {
                androidLocationManager.getLastKnownLocation(AndroidLocationManager.GPS_PROVIDER)
            } else null

            val networkLocation = if (androidLocationManager.isProviderEnabled(AndroidLocationManager.NETWORK_PROVIDER)) {
                androidLocationManager.getLastKnownLocation(AndroidLocationManager.NETWORK_PROVIDER)
            } else null

            return when {
                gpsLocation != null && networkLocation != null ->
                    if (gpsLocation.time > networkLocation.time) gpsLocation else networkLocation
                gpsLocation != null -> gpsLocation
                networkLocation != null -> networkLocation
                else -> null
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting fallback location", e)
            lastError = "Location permission revoked"
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting fallback location", e)
            lastError = e.message
        }
        return null
    }

    private fun isLocationRecent(location: Location): Boolean {
        val ageInMinutes = (System.currentTimeMillis() - location.time) / (1000 * 60)
        return ageInMinutes < 1
    }

    private fun isLocationEnabled(context: Context): Boolean {
        val androidLocationManager = context.getSystemService(Context.LOCATION_SERVICE) as AndroidLocationManager
        val gpsEnabled = androidLocationManager.isProviderEnabled(AndroidLocationManager.GPS_PROVIDER)
        val networkEnabled = androidLocationManager.isProviderEnabled(AndroidLocationManager.NETWORK_PROVIDER)
        return gpsEnabled || networkEnabled
    }

    private fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun initializeLocationClient(context: Context) {
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        }
    }

    fun cleanup() {
        fusedLocationClient?.let { client ->
            try {
                client.flushLocations()
            } catch (e: Exception) {
                Log.e(TAG, "Error flushing locations during cleanup", e)
            }
        }
        fusedLocationClient = null
        lastKnownLocation = null
        lastError = null
        isRequestingLocation = false
    }
}