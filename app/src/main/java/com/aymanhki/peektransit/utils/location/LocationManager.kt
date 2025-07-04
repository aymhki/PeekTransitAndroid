package com.aymanhki.peektransit.utils.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager as AndroidLocationManager
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.google.android.gms.location.*
import kotlinx.coroutines.async
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class LocationManager(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val androidLocationManager: AndroidLocationManager = context.getSystemService(Context.LOCATION_SERVICE) as AndroidLocationManager
    private var locationCallback: LocationCallback? = null

    companion object {
        private const val TAG = "LocationManager"
    }


    suspend fun getCurrentLocation(forceRefresh: Boolean = false): Location? {
        Log.d(TAG, "Starting location fetch process... (forceRefresh: $forceRefresh)")
        
        if (!hasLocationPermission()) {
            Log.e(TAG, "Location permissions not granted")
            return null
        }
        
        if (!isLocationEnabled()) {
            Log.e(TAG, "Location services are disabled")
            return null
        }
        
        if (!forceRefresh) {
            Log.d(TAG, "Method 1: Attempting to get cached location from FusedLocationProvider")
            val cachedLocation = getCachedLocation()
            if (cachedLocation != null && isLocationRecent(cachedLocation)) {
                Log.d(TAG, "Method 1: Success - Using cached location: ${cachedLocation.latitude}, ${cachedLocation.longitude}")
                return cachedLocation
            } else {
                Log.d(TAG, "Method 1: Failed - Cached location is ${if (cachedLocation == null) "null" else "too old"}")
            }
        } else {
            Log.d(TAG, "Method 1: Skipped - Force refresh requested")
        }
        
        Log.d(TAG, "Parallel Methods: Trying fresh location, GPS, and Network providers simultaneously")
        return withTimeoutOrNull(PeekTransitConstants.LOCATION_REQUEST_TIMEOUT_MS) {
            val freshLocationDeferred = async { requestFreshLocation() }
            val gpsLocationDeferred = async { getLocationFromProvider(AndroidLocationManager.GPS_PROVIDER) }
            val networkLocationDeferred = async { getLocationFromProvider(AndroidLocationManager.NETWORK_PROVIDER) }
            
            var completedCount = 0
            val totalTasks = 3
            
            while (completedCount < totalTasks) {
                val result = select<Location?> {
                    if (freshLocationDeferred.isActive) {
                        freshLocationDeferred.onAwait { location ->
                            if (location != null) {
                                Log.d(TAG, "Parallel Success: Got fresh location: ${location.latitude}, ${location.longitude}")
                                gpsLocationDeferred.cancel()
                                networkLocationDeferred.cancel()
                                location
                            } else {
                                Log.d(TAG, "Fresh location completed with null")
                                null
                            }
                        }
                    }
                    if (gpsLocationDeferred.isActive) {
                        gpsLocationDeferred.onAwait { location ->
                            if (location != null) {
                                Log.d(TAG, "Parallel Success: Got GPS location: ${location.latitude}, ${location.longitude}")
                                freshLocationDeferred.cancel()
                                networkLocationDeferred.cancel()
                                location
                            } else {
                                Log.d(TAG, "GPS location completed with null")
                                null
                            }
                        }
                    }
                    if (networkLocationDeferred.isActive) {
                        networkLocationDeferred.onAwait { location ->
                            if (location != null) {
                                Log.d(TAG, "Parallel Success: Got Network location: ${location.latitude}, ${location.longitude}")
                                freshLocationDeferred.cancel()
                                gpsLocationDeferred.cancel()
                                location
                            } else {
                                Log.d(TAG, "Network location completed with null")
                                null
                            }
                        }
                    }
                }
                
                if (result != null) {
                    return@withTimeoutOrNull result
                }
                
                completedCount++
            }
            
            Log.d(TAG, "All location methods completed without valid result")
            null
        } ?: run {
            Log.d(TAG, "All parallel methods failed or timed out, returning null")
            null
        }
    }
    
    private suspend fun getCachedLocation(): Location? = suspendCancellableCoroutine { continuation ->
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
            requestFreshLocation()
        }
    }
    
    private suspend fun requestFreshLocation(): Location? = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, PeekTransitConstants.LOCATION_REQUEST_UPDATE_INTERVAL_MS)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(PeekTransitConstants.LOCATION_REQUEST_MIN_UPDATE_INTERVAL_MS)
            .setMaxUpdateDelayMillis(PeekTransitConstants.LOCATION_REQUEST_TIMEOUT_MS)
            .build()
        
        val callback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation
                Log.d(TAG, "Fresh location received: ${location?.latitude}, ${location?.longitude}")
                fusedLocationClient.removeLocationUpdates(this)
                continuation.resume(location)
            }
        }
        
        try {
            Log.d(TAG, "Requesting location updates for fresh location...")
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
            
            continuation.invokeOnCancellation {
                fusedLocationClient.removeLocationUpdates(callback)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception requesting fresh location", e)
            continuation.resume(null)
        }
    }
    
    private suspend fun getLocationFromProvider(provider: String): Location? = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        
        try {
            if (!androidLocationManager.isProviderEnabled(provider)) {
                Log.d(TAG, "Provider $provider is not enabled")
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }
            
            val lastKnownLocation = androidLocationManager.getLastKnownLocation(provider)
            if (lastKnownLocation != null && isLocationRecent(lastKnownLocation)) {
                Log.d(TAG, "Using last known location from $provider")
                continuation.resume(lastKnownLocation)
                return@suspendCancellableCoroutine
            }
            
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    Log.d(TAG, "Location received from $provider: ${location.latitude}, ${location.longitude}")
                    androidLocationManager.removeUpdates(this)
                    continuation.resume(location)
                }
                
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {
                    Log.d(TAG, "Provider $provider disabled")
                    androidLocationManager.removeUpdates(this)
                    continuation.resume(null)
                }
            }
            
            Log.d(TAG, "Requesting location updates from $provider...")
            androidLocationManager.requestLocationUpdates(
                provider,
                0L,
                0f,
                listener,
                Looper.getMainLooper()
            )
            
            continuation.invokeOnCancellation {
                androidLocationManager.removeUpdates(listener)
            }
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting location from $provider", e)
            continuation.resume(null)
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting location from $provider", e)
            continuation.resume(null)
        }
    }
    
    private fun isLocationRecent(location: Location): Boolean {
        val ageInMinutes = (System.currentTimeMillis() - location.time) / (1000 * 60)
        val isRecent = ageInMinutes < 1
        Log.d(TAG, "Location age: ${ageInMinutes} minutes, isRecent: $isRecent")
        return isRecent
    }
    
    private fun isLocationEnabled(): Boolean {
        val gpsEnabled = androidLocationManager.isProviderEnabled(AndroidLocationManager.GPS_PROVIDER)
        val networkEnabled = androidLocationManager.isProviderEnabled(AndroidLocationManager.NETWORK_PROVIDER)
        val isEnabled = gpsEnabled || networkEnabled
        Log.d(TAG, "Location services - GPS: $gpsEnabled, Network: $networkEnabled, Overall: $isEnabled")
        return isEnabled
    }
    
    fun startLocationUpdates(
        updateInterval: Long = PeekTransitConstants.LOCATION_UPDATE_INTERVAL_MS,
        minDistanceThreshold: Float = PeekTransitConstants.LOCATION_UPDATE_MIN_DISTANCE_METERS,
        callback: (Location) -> Unit
    ) {
        if (!hasLocationPermission()) return
        
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
                    Log.d(TAG, "Location update received: ${location.latitude}, ${location.longitude}")
                    callback(location)
                }
            }
        }
        
        try {
            Log.d(TAG, "Starting location updates with ${updateInterval}ms interval and ${minDistanceThreshold}m threshold")
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