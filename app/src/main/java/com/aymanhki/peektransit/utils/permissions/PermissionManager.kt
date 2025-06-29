package com.aymanhki.peektransit.utils.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Permission state class that replaces MultiplePermissionsState from Accompanist
 * Provides exact same API for drop-in replacement with enhanced functionality
 */
class PermissionState(
    val allPermissionsGranted: Boolean,
    val permissions: List<PermissionInfo>,
    val shouldShowRationale: Boolean,
    private val onLaunchPermissionRequest: () -> Unit,
    private val onOpenSettings: () -> Unit
) {
    /**
     * Launch permission request - matches Accompanist API exactly
     * Now intelligently chooses between permission dialog or settings
     */
    fun launchMultiplePermissionRequest() {
        // If any permissions were already requested and denied, go to settings
        if (hasRequestedPermissions()) {
            onOpenSettings()
        } else {
            // First time requesting, show permission dialog
            onLaunchPermissionRequest()
        }
    }
    
    /**
     * Force permission dialog (for first-time requests)
     */
    fun requestPermissions() {
        onLaunchPermissionRequest()
    }
    
    /**
     * Force open settings (for denied permissions)
     */
    fun openSettings() {
        onOpenSettings()
    }
    
    /**
     * Check if any permissions were previously requested
     */
    private fun hasRequestedPermissions(): Boolean {
        return permissions.any { 
            it.status == PermissionStatus.DENIED || 
            it.status == PermissionStatus.PERMANENTLY_DENIED 
        }
    }
    
    /**
     * Check if a specific permission is granted
     */
    fun isPermissionGranted(permission: String): Boolean {
        return permissions.find { it.permission == permission }?.status == PermissionStatus.GRANTED
    }
    
    /**
     * Get all granted permissions
     */
    val grantedPermissions: List<String>
        get() = permissions.filter { it.status == PermissionStatus.GRANTED }.map { it.permission }
    
    /**
     * Get all denied permissions that can be requested again
     */
    val deniedPermissions: List<String>
        get() = permissions.filter { it.status == PermissionStatus.DENIED }.map { it.permission }
    
    /**
     * Get all permanently denied permissions
     */
    val permanentlyDeniedPermissions: List<String>
        get() = permissions.filter { it.status == PermissionStatus.PERMANENTLY_DENIED }.map { it.permission }
        
    override fun toString(): String {
        return "PermissionState(allGranted=$allPermissionsGranted, shouldShowRationale=$shouldShowRationale, permissions=${permissions.size})"
    }
}

/**
 * Individual permission information
 */
data class PermissionInfo(
    val permission: String,
    val status: PermissionStatus
)

/**
 * Permission status enum that matches Android's actual permission states
 */
enum class PermissionStatus {
    GRANTED,                // Permission is granted
    NOT_REQUESTED,         // Permission has never been requested
    DENIED,                // Permission denied but can request again (shouldShowRationale = true)
    PERMANENTLY_DENIED     // Permission permanently denied (shouldShowRationale = false after request)
}

/**
 * Permission manager using stable Android APIs
 * Must be initialized in Activity lifecycle
 * Includes lifecycle awareness to refresh permissions when returning from settings
 */
class PermissionManager(private val activity: ComponentActivity) : DefaultLifecycleObserver {
    
    companion object {
        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        private const val PERMISSION_PREFS = "permission_prefs"
        private const val PERMISSION_REQUESTED_PREFIX = "requested_"
    }
    
    // SharedPreferences to track which permissions were requested
    private val prefs: SharedPreferences = activity.getSharedPreferences(PERMISSION_PREFS, Context.MODE_PRIVATE)
    
    // Permission state holder
    private val _permissionState = mutableStateOf(createInitialPermissionState())
    val permissionState: State<PermissionState> = _permissionState
    
    // Activity result launcher for requesting permissions
    val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Mark permissions as requested
        markPermissionsAsRequested(permissions.keys)
        updatePermissionState(permissions)
    }
    
    init {
        // Register lifecycle observer to refresh permissions when app resumes
        activity.lifecycle.addObserver(this)
    }
    
    /**
     * Mark permissions as requested in SharedPreferences
     */
    private fun markPermissionsAsRequested(permissions: Collection<String>) {
        prefs.edit().apply {
            permissions.forEach { permission ->
                putBoolean(PERMISSION_REQUESTED_PREFIX + permission, true)
            }
        }.apply()
    }
    
    /**
     * Check if permission was previously requested
     */
    private fun wasPermissionRequested(permission: String): Boolean {
        return prefs.getBoolean(PERMISSION_REQUESTED_PREFIX + permission, false)
    }
    
    /**
     * Create initial permission state by checking current permissions
     */
    private fun createInitialPermissionState(): PermissionState {
        val permissionInfos = LOCATION_PERMISSIONS.map { permission ->
            val isGranted = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
            val shouldShowRationale = activity.shouldShowRequestPermissionRationale(permission)
            val wasRequested = wasPermissionRequested(permission)
            
            val status = when {
                isGranted -> PermissionStatus.GRANTED
                shouldShowRationale -> PermissionStatus.DENIED
                wasRequested -> PermissionStatus.PERMANENTLY_DENIED
                else -> PermissionStatus.NOT_REQUESTED
            }
            
            PermissionInfo(permission, status)
        }
        
        // Location is granted if ANY location permission is granted (coarse OR fine)
        val locationGranted = hasAnyLocationPermission()
        val shouldShowRationale = permissionInfos.any { it.status == PermissionStatus.DENIED }
        
        return PermissionState(
            allPermissionsGranted = locationGranted,
            permissions = permissionInfos,
            shouldShowRationale = shouldShowRationale,
            onLaunchPermissionRequest = { requestPermissions() },
            onOpenSettings = { openAppSettings() }
        )
    }
    
    /**
     * Update permission state after permission request result
     */
    private fun updatePermissionState(permissions: Map<String, Boolean>) {
        val permissionInfos = LOCATION_PERMISSIONS.map { permission ->
            val isGranted = permissions[permission] == true
            val shouldShowRationale = activity.shouldShowRequestPermissionRationale(permission)
            val wasRequested = wasPermissionRequested(permission)
            
            val status = when {
                isGranted -> PermissionStatus.GRANTED
                shouldShowRationale -> PermissionStatus.DENIED
                wasRequested -> PermissionStatus.PERMANENTLY_DENIED
                else -> PermissionStatus.NOT_REQUESTED
            }
            
            PermissionInfo(permission, status)
        }
        
        // Location is granted if ANY location permission is granted (coarse OR fine)
        val locationGranted = hasAnyLocationPermission()
        val shouldShowRationale = permissionInfos.any { it.status == PermissionStatus.DENIED }
        
        _permissionState.value = PermissionState(
            allPermissionsGranted = locationGranted,
            permissions = permissionInfos,
            shouldShowRationale = shouldShowRationale,
            onLaunchPermissionRequest = { requestPermissions() },
            onOpenSettings = { openAppSettings() }
        )
    }
    
    /**
     * Check if any location permission is granted (coarse OR fine)
     * This handles the case where precise location toggle is off
     */
    private fun hasAnyLocationPermission(): Boolean {
        return LOCATION_PERMISSIONS.any { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Launch permission request with validation
     */
    fun requestPermissions() {
        // Don't request if location is already granted (any permission)
        if (hasAnyLocationPermission()) {
            refreshPermissionState() // Update UI to reflect current state
            return
        }
        
        // Don't request if no permissions can be requested (all permanently denied)
        if (!canRequestPermissions()) {
            return
        }
        
        try {
            // Mark permissions as requested before launching
            markPermissionsAsRequested(LOCATION_PERMISSIONS.toList())
            permissionLauncher.launch(LOCATION_PERMISSIONS)
        } catch (e: Exception) {
            // Handle any launch exceptions gracefully
            android.util.Log.e("PermissionManager", "Failed to launch permission request", e)
        }
    }
    
    /**
     * Refresh permission state (useful when returning from settings)
     */
    fun refreshPermissionState() {
        _permissionState.value = createInitialPermissionState()
    }
    
    /**
     * Lifecycle callback - refresh permissions when app resumes
     * This automatically detects permission changes made in settings
     * Also handles "ask every time" permission changes
     */
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        refreshPermissionState()
    }
    
    /**
     * Lifecycle callback - cleanup when activity is destroyed
     */
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        activity.lifecycle.removeObserver(this)
    }
    
    /**
     * Check if any permissions can be requested (not permanently denied)
     * Handles "ask every time" and other edge cases
     */
    fun canRequestPermissions(): Boolean {
        // If any location permission is already granted, no need to request
        if (hasAnyLocationPermission()) {
            return false
        }
        
        return LOCATION_PERMISSIONS.any { permission ->
            val isGranted = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
            val shouldShowRationale = activity.shouldShowRequestPermissionRationale(permission)
            val wasRequested = wasPermissionRequested(permission)
            
            when {
                isGranted -> false // Already granted
                shouldShowRationale -> true // Can request again (includes "ask every time")
                !wasRequested -> true // Never requested yet
                else -> false // Permanently denied
            }
        }
    }
    
         /**
      * Check if we have adequate location access (any location permission)
      * This is the main method apps should use to check location availability
      */
     fun hasLocationAccess(): Boolean {
         return hasAnyLocationPermission()
     }
     
     /**
      * Check if location permission is in "ask every time" mode
      * This happens when shouldShowRationale is true OR permission was never requested
      */
     fun isAskEveryTimeMode(): Boolean {
         if (hasAnyLocationPermission()) return false
         
         return LOCATION_PERMISSIONS.any { permission ->
             val shouldShowRationale = activity.shouldShowRequestPermissionRationale(permission)
             val wasRequested = wasPermissionRequested(permission)
             
             // "Ask every time" shows rationale OR was never requested
             shouldShowRationale || !wasRequested
         }
     }
     
     /**
      * Get individual permission status
      */
     fun getPermissionStatus(permission: String): PermissionStatus {
         val isGranted = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
         val shouldShowRationale = activity.shouldShowRequestPermissionRationale(permission)
         val wasRequested = wasPermissionRequested(permission)
         
         return when {
             isGranted -> PermissionStatus.GRANTED
             shouldShowRationale -> PermissionStatus.DENIED
             wasRequested -> PermissionStatus.PERMANENTLY_DENIED
                          else -> PermissionStatus.NOT_REQUESTED
          }
      }
     
     /**
      * Open app settings where users can tap "Permissions" to manage location access
      */
     fun openAppSettings() {
         try {
             val intent = Intent().apply {
                 action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                 data = Uri.fromParts("package", activity.packageName, null)
                 flags = Intent.FLAG_ACTIVITY_NEW_TASK
             }
             activity.startActivity(intent)
         } catch (e: Exception) {
             // Fallback to general settings if app settings fail
             try {
                 val intent = Intent().apply {
                     action = Settings.ACTION_SETTINGS
                     flags = Intent.FLAG_ACTIVITY_NEW_TASK
                 }
                 activity.startActivity(intent)
             } catch (e2: Exception) {
                 android.util.Log.e("PermissionManager", "Failed to open settings", e2)
             }
         }
     }
     
     /**
      * Debug method to log current permission state
      * Useful for troubleshooting permission issues
      */
     fun logPermissionState() {
         android.util.Log.d("PermissionManager", "=== Permission State Debug ===")
         android.util.Log.d("PermissionManager", "Has any location permission: ${hasAnyLocationPermission()}")
         android.util.Log.d("PermissionManager", "Can request permissions: ${canRequestPermissions()}")
         android.util.Log.d("PermissionManager", "Is ask every time mode: ${isAskEveryTimeMode()}")
         
         LOCATION_PERMISSIONS.forEach { permission ->
             val isGranted = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
             val shouldShowRationale = activity.shouldShowRequestPermissionRationale(permission)
             val wasRequested = wasPermissionRequested(permission)
             android.util.Log.d("PermissionManager", "$permission: granted=$isGranted, rationale=$shouldShowRationale, requested=$wasRequested")
         }
         android.util.Log.d("PermissionManager", "============================")
     }
}

/**
 * CompositionLocal to provide PermissionManager throughout the app
 */
val LocalPermissionManager = compositionLocalOf<PermissionManager?> { null }

/**
 * Composable function that provides permission state compatible with Accompanist API
 * Direct replacement for rememberMultiplePermissionsState from Accompanist
 */
@Composable
fun rememberMultiplePermissionsState(
    permissions: List<String>
): PermissionState {
    val permissionManager = LocalPermissionManager.current
        ?: error("PermissionManager not found. Make sure to provide it via LocalPermissionManager.")
    
    val state by permissionManager.permissionState
    
    // Return the current state - it already has the launch function built-in
    return state
} 