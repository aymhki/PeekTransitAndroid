package com.aymanhki.peektransit.utils.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class PermissionState(
    val allPermissionsGranted: Boolean,
    val permissions: List<PermissionInfo>,
    val shouldShowRationale: Boolean,
    private val onLaunchPermissionRequest: () -> Unit,
    private val onOpenSettings: () -> Unit
) {
    fun launchMultiplePermissionRequest() {
        if (hasRequestedPermissions()) {
            onOpenSettings()
        } else {
            onLaunchPermissionRequest()
        }
    }


    private fun hasRequestedPermissions(): Boolean {
        return permissions.any { 
            it.status == PermissionStatus.DENIED || 
            it.status == PermissionStatus.PERMANENTLY_DENIED 
        }
    }
}

data class PermissionInfo(
    val permission: String,
    val status: PermissionStatus
)


enum class PermissionStatus {
    GRANTED,
    NOT_REQUESTED,
    DENIED,
    PERMANENTLY_DENIED
}

class PermissionManager(private val activity: ComponentActivity) : DefaultLifecycleObserver {
    
    companion object {
        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        private const val PERMISSION_PREFS = "permission_prefs"
        private const val PERMISSION_REQUESTED_PREFIX = "requested_"
    }
    
    private val prefs: SharedPreferences = activity.getSharedPreferences(PERMISSION_PREFS, Context.MODE_PRIVATE)
    
    private val _permissionState = mutableStateOf(createInitialPermissionState())
    val permissionState: State<PermissionState> = _permissionState
    
    val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        markPermissionsAsRequested(permissions.keys)
        updatePermissionState(permissions)
    }
    
    init {
        activity.lifecycle.addObserver(this)
    }
    

    private fun markPermissionsAsRequested(permissions: Collection<String>) {
        prefs.edit().apply {
            permissions.forEach { permission ->
                putBoolean(PERMISSION_REQUESTED_PREFIX + permission, true)
            }
        }.apply()
    }

    private fun wasPermissionRequested(permission: String): Boolean {
        return prefs.getBoolean(PERMISSION_REQUESTED_PREFIX + permission, false)
    }

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

    private fun hasAnyLocationPermission(): Boolean {
        return LOCATION_PERMISSIONS.any { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    

    fun requestPermissions() {
        if (hasAnyLocationPermission()) {
            refreshPermissionState()
            return
        }
        
        if (!canRequestPermissions()) {
            return
        }
        
        try {
            markPermissionsAsRequested(LOCATION_PERMISSIONS.toList())
            permissionLauncher.launch(LOCATION_PERMISSIONS)
        } catch (e: Exception) {
            android.util.Log.e("PermissionManager", "Failed to launch permission request", e)
        }
    }

    fun refreshPermissionState() {
        _permissionState.value = createInitialPermissionState()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        refreshPermissionState()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        activity.lifecycle.removeObserver(this)
    }

    fun canRequestPermissions(): Boolean {
        if (hasAnyLocationPermission()) {
            return false
        }
        
        return LOCATION_PERMISSIONS.any { permission ->
            val isGranted = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
            val shouldShowRationale = activity.shouldShowRequestPermissionRationale(permission)
            val wasRequested = wasPermissionRequested(permission)
            
            when {
                isGranted -> false
                shouldShowRationale -> true
                !wasRequested -> true
                else -> false
            }
        }
    }

     fun hasLocationAccess(): Boolean {
         return hasAnyLocationPermission()
     }
     

     fun isAskEveryTimeMode(): Boolean {
         if (hasAnyLocationPermission()) return false
         
         return LOCATION_PERMISSIONS.any { permission ->
             val shouldShowRationale = activity.shouldShowRequestPermissionRationale(permission)
             val wasRequested = wasPermissionRequested(permission)
             
             shouldShowRationale || !wasRequested
         }
     }

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

     fun openAppSettings() {
         try {
             val intent = Intent().apply {
                 action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                 data = Uri.fromParts("package", activity.packageName, null)
                 flags = Intent.FLAG_ACTIVITY_NEW_TASK
             }
             activity.startActivity(intent)
         } catch (e: Exception) {
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
}

val LocalPermissionManager = compositionLocalOf<PermissionManager?> { null }

@Composable
fun rememberMultiplePermissionsState(
    permissions: List<String>
): PermissionState {
    val permissionManager = LocalPermissionManager.current
        ?: error("PermissionManager not found. Make sure to provide it via LocalPermissionManager.")
    
    val state by permissionManager.permissionState
    
    return state
} 