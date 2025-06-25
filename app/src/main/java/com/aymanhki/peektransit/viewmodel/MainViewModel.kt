package com.aymanhki.peektransit.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aymanhki.peektransit.MainActivity
import com.aymanhki.peektransit.data.models.Stop
import com.aymanhki.peektransit.data.models.TransitError
import com.aymanhki.peektransit.data.repository.StopsDataStore
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val stopsDataStore = StopsDataStore.getInstance().apply {
        initialize(application.applicationContext)
    }
    
    // Track initialization state to prevent redundant API calls
    private val _isInitialized = MutableLiveData(false)
    val isInitialized: LiveData<Boolean> = _isInitialized
    
    // Location monitoring
    private val locationManager = MainActivity.getLocationManager(application)
    private var isLocationMonitoringActive = false
    
    // Live location updates
    private val _currentLocation = MutableLiveData<Location?>()
    val currentLocation: LiveData<Location?> = _currentLocation
    
    val stops: LiveData<List<Stop>> = stopsDataStore.stops
    val isLoading: LiveData<Boolean> = stopsDataStore.isLoading
    val error: LiveData<TransitError?> = stopsDataStore.error
    val searchResults: LiveData<List<Stop>> = stopsDataStore.searchResults
    val isSearching: LiveData<Boolean> = stopsDataStore.isSearching
    val searchError: LiveData<TransitError?> = stopsDataStore.searchError
    
    fun loadStops(userLocation: Location, loadingFromWidgetSetup: Boolean = false, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            stopsDataStore.loadStops(userLocation, loadingFromWidgetSetup, forceRefresh)
            if (_isInitialized.value != true) {
                _isInitialized.postValue(true)
            }
        }
    }
    
    fun initializeWithLocation(userLocation: Location) {
        if (_isInitialized.value == false) {
            loadStops(userLocation)
            startLocationMonitoring()
        }
    }
    
    private fun startLocationMonitoring() {
        if (!isLocationMonitoringActive && locationManager.hasLocationPermission()) {
            isLocationMonitoringActive = true
            locationManager.startLocationUpdates(
                updateInterval = 5000L, // Check every 5 seconds for immediate detection like iOS
                callback = { newLocation ->
                    // Update current location for UI
                    _currentLocation.postValue(newLocation)
                    // Automatically reload stops when location changes significantly
                    loadStops(newLocation, forceRefresh = false)
                }
            )
        }
    }
    
    fun stopLocationMonitoring() {
        if (isLocationMonitoringActive) {
            isLocationMonitoringActive = false
            locationManager.stopLocationUpdates()
        }
    }
    
    fun updateCurrentLocation(location: Location) {
        _currentLocation.postValue(location)
    }
    
    fun searchForStops(query: String, userLocation: Location? = null) {
        viewModelScope.launch {
            stopsDataStore.searchForStops(query, userLocation)
        }
    }
    
    fun getStop(stopNumber: Int, callback: (Stop?) -> Unit) {
        viewModelScope.launch {
            val stop = stopsDataStore.getStop(stopNumber)
            callback(stop)
        }
    }
    
    fun clearError() {
        stopsDataStore.clearError()
    }
    
    fun clearSearchError() {
        stopsDataStore.clearSearchError()
    }
    
    fun clearSearchResults() {
        stopsDataStore.clearSearchResults()
    }
    
    override fun onCleared() {
        super.onCleared()
        stopLocationMonitoring()
        stopsDataStore.cancelAllOperations()
    }
    
}