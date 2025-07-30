package com.aymanhki.peektransit.widgets

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object WidgetLocationManager {
    private const val LOCATION_TIMEOUT_MS = 30000L
    private const val LOCATION_UPDATE_INTERVAL_MS = 10000L
    private const val FASTEST_INTERVAL_MS = 5000L
    private const val MAX_RETRY_ATTEMPTS = 3

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var lastKnownLocation: Location? = null
    private var lastError: String? = null

    suspend fun getCurrentLocation(context: Context): Pair<Location?, String?> {
        if (!hasLocationPermission(context)) {
            val error = "Location permission not granted"
            return Pair(lastKnownLocation, error)
        }

        initializeLocationClient(context)

        var attempt = 0
        var location: Location? = null
        var error: String? = null

        while (attempt < MAX_RETRY_ATTEMPTS && location == null) {
            attempt++

            try {
                location = fetchFreshLocation(context)
                if (location != null) {
                    lastKnownLocation = location
                    lastError = null
                    break
                }
            } catch (e: Exception) {
                error = "Location fetch attempt $attempt failed: ${e.message}"
            }

            if (location == null && attempt < MAX_RETRY_ATTEMPTS) {
                try {
                    location = getLastKnownLocation(context)
                    if (location != null) {
                        lastKnownLocation = location
                        error = "Using last known location after fresh location failed"
                        break
                    }
                } catch (e: Exception) {
                    error = "Failed to get last known location: ${e.message}"
                }
            }
        }

        if (location == null) {
            error = lastError ?: "Failed to obtain location after $MAX_RETRY_ATTEMPTS attempts"
        }

        return Pair(location ?: lastKnownLocation, error)
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

    private suspend fun fetchFreshLocation(context: Context): Location? {
        val client = fusedLocationClient ?: return null

        return withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val cancellationTokenSource = CancellationTokenSource()

                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                    stopLocationUpdates()
                }

                try {
                    val locationRequest = LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        LOCATION_UPDATE_INTERVAL_MS
                    ).apply {
                        setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
                        setMaxUpdates(1)
                    }.build()

                    locationCallback = object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            val location = result.lastLocation
                            if (location != null && !continuation.isCompleted) {
                                stopLocationUpdates()
                                continuation.resume(location)
                            }
                        }

                        override fun onLocationAvailability(availability: LocationAvailability) {
                            if (!availability.isLocationAvailable && !continuation.isCompleted) {
                                stopLocationUpdates()
                                continuation.resume(null)
                            }
                        }
                    }

                    client.requestLocationUpdates(
                        locationRequest,
                        locationCallback!!,
                        Looper.getMainLooper()
                    ).addOnFailureListener { exception ->
                        if (!continuation.isCompleted) {
                            stopLocationUpdates()
                            lastError = exception.message
                            continuation.resume(null)
                        }
                    }

                    client.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.token
                    ).addOnSuccessListener { location ->
                        if (location != null && !continuation.isCompleted) {
                            stopLocationUpdates()
                            continuation.resume(location)
                        }
                    }.addOnFailureListener { exception ->
                        lastError = exception.message
                    }

                } catch (e: SecurityException) {
                    lastError = "Location permission revoked"
                    if (!continuation.isCompleted) {
                        continuation.resume(null)
                    }
                } catch (e: Exception) {
                    lastError = e.message
                    if (!continuation.isCompleted) {
                        continuation.resume(null)
                    }
                }
            }
        }
    }

    private suspend fun getLastKnownLocation(context: Context): Location? {
        val client = fusedLocationClient ?: return null

        return withTimeoutOrNull(10000L) {
            suspendCancellableCoroutine { continuation ->
                try {
                    client.lastLocation.addOnSuccessListener { location ->
                        if (!continuation.isCompleted) {
                            continuation.resume(location)
                        }
                    }.addOnFailureListener { exception ->
                        if (!continuation.isCompleted) {
                            lastError = exception.message
                            continuation.resume(null)
                        }
                    }
                } catch (e: SecurityException) {
                    if (!continuation.isCompleted) {
                        lastError = "Location permission revoked"
                        continuation.resume(null)
                    }
                } catch (e: Exception) {
                    if (!continuation.isCompleted) {
                        lastError = e.message
                        continuation.resume(null)
                    }
                }
            }
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            fusedLocationClient?.removeLocationUpdates(callback)
        }
        locationCallback = null
    }

    fun cleanup() {
        stopLocationUpdates()
        fusedLocationClient?.flushLocations()
        fusedLocationClient = null
        lastKnownLocation = null
        lastError = null
    }
}