package com.aymanhki.peektransit.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aymanhki.peektransit.data.models.Stop
import com.aymanhki.peektransit.data.models.TransitError
import com.aymanhki.peektransit.data.repository.StopsDataStore
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.location.LocationManagerProvider
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val stopsDataStore = StopsDataStore.getInstance().apply {
        initialize(application.applicationContext)
    }
    
    private val _isInitialized = MutableLiveData(false)
    val isInitialized: LiveData<Boolean> = _isInitialized
    
    private val locationManager = LocationManagerProvider.getInstance(application)
    private var isLocationMonitoringActive = false
    
    private val _currentLocation = MutableLiveData<Location?>()
    val currentLocation: LiveData<Location?> = _currentLocation
    
    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery
    
    private val _lastSearchedQuery = MutableLiveData("")
    val lastSearchedQuery: LiveData<String> = _lastSearchedQuery
    
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
                updateInterval = PeekTransitConstants.LOCATION_UPDATE_INTERVAL_MS,
                minDistanceThreshold = PeekTransitConstants.LOCATION_UPDATE_MIN_DISTANCE_METERS,
                callback = { newLocation ->
                    _currentLocation.postValue(newLocation)
                    val currentLocation = _currentLocation.value
                    val shouldRefreshStops = if (currentLocation != null) {
                        val distance = currentLocation.distanceTo(newLocation)
                        distance > PeekTransitConstants.DISTANCE_CHANGE_ALLOWED_BEFORE_REFRESHING_STOPS
                    } else {
                        true
                    }
                    
                    if (shouldRefreshStops) {
                        loadStops(newLocation, forceRefresh = false)
                    }
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
    
    fun updateSearchQuery(query: String) {
        _searchQuery.postValue(query)
    }
    
    fun clearSearchQuery() {
        _searchQuery.postValue("")
        _lastSearchedQuery.postValue("")
    }
    
    fun searchForStops(query: String, userLocation: Location? = null) {
        viewModelScope.launch {
            stopsDataStore.searchForStops(query, userLocation)
            _lastSearchedQuery.postValue(query)
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
    
    override fun onCleared() {
        super.onCleared()
        stopLocationMonitoring()
        stopsDataStore.cancelAllOperations()
    }
    
}