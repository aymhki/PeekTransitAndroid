package com.aymanhki.peektransit.utils.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager as AndroidLocationManager
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.google.android.gms.location.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlin.coroutines.resume

class LocationManager(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val androidLocationManager: AndroidLocationManager = context.getSystemService(Context.LOCATION_SERVICE) as AndroidLocationManager
    private var locationCallback: LocationCallback? = null
    private var isRequestingLocation = false

    companion object {
        private const val TAG = "LocationManager"
        private const val QUICK_LOCATION_TIMEOUT_MS = PeekTransitConstants.QUICK_LOCATION_TIMEOUT_MS
        private const val PROGRESSIVE_LOCATION_TIMEOUT_MS = PeekTransitConstants.PROGRESSIVE_LOCATION_TIMEOUT_MS
    }

    suspend fun getCurrentLocation(forceRefresh: Boolean = false): Location? {
        if (!hasLocationPermission()) {
            Log.d(TAG, "Location permissions not granted")
            return null
        }

        if (!isLocationEnabled()) {
            Log.d(TAG, "Location services are disabled")
            return null
        }

        val location = if (forceRefresh) {
            requestProgressiveLocation()
        } else {
            getCachedLocation()?.takeIf { isLocationRecent(it) } ?: requestProgressiveLocation()
        }

        return location ?: getFallbackLocation()
    }


    private suspend fun requestProgressiveLocation(): Location? = coroutineScope {
        if (!hasLocationPermission()) return@coroutineScope null

        if (isRequestingLocation) {
            Log.d(TAG, "Already requesting location, skipping duplicate request")
            return@coroutineScope null
        }

        val quickLocationDeferred = async {
            Log.d(TAG, "Phase 1: Requesting quick location")
            requestLocationWithStrategy(
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
        priority: Int,
        waitForAccuracy: Boolean,
        timeoutMs: Long
    ): Location? = withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine { continuation ->
            if (!hasLocationPermission()) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            isRequestingLocation = true

            val locationRequest = LocationRequest.Builder(priority, PeekTransitConstants.LOCATION_UPDATE_INTERVAL_MS)
                .setWaitForAccurateLocation(waitForAccuracy)
                .setMinUpdateIntervalMillis(PeekTransitConstants.LOCATION_REQUEST_MIN_UPDATE_INTERVAL_MS)
                .setMaxUpdateDelayMillis(timeoutMs)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    val location = locationResult.lastLocation
                    fusedLocationClient.removeLocationUpdates(this)
                    isRequestingLocation = false
                    Log.d(TAG, "Location received: lat=${location?.latitude}, lng=${location?.longitude}, accuracy=${location?.accuracy}")
                    continuation.resume(location)
                }
            }

            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    callback,
                    Looper.getMainLooper()
                )

                continuation.invokeOnCancellation {
                    fusedLocationClient.removeLocationUpdates(callback)
                    isRequestingLocation = false
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception requesting location", e)
                isRequestingLocation = false
                continuation.resume(null)
            }
        }
    }

    private fun getFallbackLocation(): Location? {
        try {
            if (hasLocationPermission()) {
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
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting fallback location", e)
        }
        return null
    }

    suspend fun getCachedLocation(): Location? = suspendCancellableCoroutine { continuation ->
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                continuation.resume(location)
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Failed to get cached location", exception)
                continuation.resume(null)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting cached location", e)
            continuation.resume(null)
        }
    }

    private suspend fun requestFreshLocationWithTimeout(): Location? {
        return withTimeoutOrNull(PeekTransitConstants.LOCATION_REQUEST_TIMEOUT_MS) {
            requestProgressiveLocation()
        }
    }

    private fun isLocationRecent(location: Location): Boolean {
        val ageInMinutes = (System.currentTimeMillis() - location.time) / (1000 * 60)
        return ageInMinutes < 1
    }

    private fun isLocationEnabled(): Boolean {
        val gpsEnabled = androidLocationManager.isProviderEnabled(AndroidLocationManager.GPS_PROVIDER)
        val networkEnabled = androidLocationManager.isProviderEnabled(AndroidLocationManager.NETWORK_PROVIDER)
        return gpsEnabled || networkEnabled
    }

    fun startLocationUpdates(
        updateInterval: Long = PeekTransitConstants.LOCATION_UPDATE_INTERVAL_MS,
        minDistanceThreshold: Float = PeekTransitConstants.LOCATION_UPDATE_MIN_DISTANCE_METERS,
        callback: (Location) -> Unit
    ) {
        if (!hasLocationPermission()) return
        stopLocationUpdates()

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, updateInterval)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(PeekTransitConstants.LOCATION_UPDATE_MIN_INTERVAL_MS)
            .setMaxUpdateDelayMillis(updateInterval * 2)
            .setMinUpdateDistanceMeters(minDistanceThreshold)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    callback(location)
                }
            }
        }

        try {
            Log.d(TAG, "Starting location updates")
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception starting location updates", e)
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
            locationCallback = null
        }
    }

    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
