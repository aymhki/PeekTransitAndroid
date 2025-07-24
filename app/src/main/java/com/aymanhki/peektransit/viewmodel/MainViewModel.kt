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
    val stopsDataStore = StopsDataStore.getInstance().apply {
        initialize(application.applicationContext)
    }
    
    private val _isInitialized = MutableLiveData(false)
    val isInitialized: LiveData<Boolean> = _isInitialized
    
    private val _isLoadingLocation = MutableLiveData(false)
    val isLoadingLocation: LiveData<Boolean> = _isLoadingLocation
    
    private val _isLoadingStops = MutableLiveData(false)
    val isLoadingStops: LiveData<Boolean> = _isLoadingStops
    
    private val _locationError = MutableLiveData<TransitError?>(null)
    val locationError: LiveData<TransitError?> = _locationError
    
    private val locationManager = LocationManagerProvider.getInstance(application)
    private var isLocationMonitoringActive = false
    private var previousLocation: Location? = null
    
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
            _isLoadingStops.postValue(true)
            try {
                stopsDataStore.loadStops(userLocation, loadingFromWidgetSetup, forceRefresh)
                if (_isInitialized.value != true) {
                    _isInitialized.postValue(true)
                }
            } finally {
                _isLoadingStops.postValue(false)
            }
        }
    }
    
    fun initializeWithLocation(userLocation: Location) {
        if (_isInitialized.value == false) {
            loadStops(userLocation)
            startLocationMonitoring()
        }
    }
    
    fun initializeGlobal() {
        if (_isInitialized.value == false && locationManager.hasLocationPermission()) {
            fetchLocationForCamera()
            fetchLocationAndLoadStops()
        }
    }
    
    private fun fetchLocationForCamera() {
        viewModelScope.launch {
            try {
                val location = locationManager.getCurrentLocation(forceRefresh = true)
                if (location != null) {
                    _currentLocation.postValue(location)
                } else {
                    val fallbackLocation = locationManager.getCurrentLocation(forceRefresh = false)
                    if (fallbackLocation != null) {
                        _currentLocation.postValue(fallbackLocation)
                    } else {
                    }
                }
            } catch (e: Exception) {
                try {
                    val simpleLocation = locationManager.getCurrentLocation(forceRefresh = false)
                    if (simpleLocation != null) {
                        _currentLocation.postValue(simpleLocation)
                    }
                } catch (e2: Exception) {

                }
            }
        }
    }
    
    private fun fetchLocationAndLoadStops(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoadingLocation.postValue(true)
            try {
                val location = locationManager.getCurrentLocation(forceRefresh)
                if (location != null) {
                    _currentLocation.postValue(location)
                    
                    val shouldUpdate = if (previousLocation != null) {
                        val distance = previousLocation!!.distanceTo(location)
                        distance > PeekTransitConstants.DISTANCE_CHANGE_ALLOWED_BEFORE_REFRESHING_STOPS_IN_METERS
                    } else {
                        true
                    }
                    
                    if (shouldUpdate || forceRefresh) {
                        if (_isInitialized.value != true) {
                            initializeWithLocation(location)
                        } else {
                            loadStops(location, forceRefresh = forceRefresh)
                        }
                    }
                    previousLocation = location
                } else {
                    _locationError.postValue(TransitError.NetworkError(Exception("Unable to get location. Please check location permissions and settings.")))
                }
            } catch (e: Exception) {
                _locationError.postValue(TransitError.NetworkError(e))
            } finally {
                _isLoadingLocation.postValue(false)
            }
        }
    }
    
    fun retry() {
        clearError()
        clearLocationError()
        fetchLocationAndLoadStops(forceRefresh = true)
    }
    
    fun clearLocationError() {
        _locationError.postValue(null)
    }
    
    private fun startLocationMonitoring() {
        if (!isLocationMonitoringActive && locationManager.hasLocationPermission()) {
            isLocationMonitoringActive = true
            locationManager.startLocationUpdates(
                updateInterval = PeekTransitConstants.LOCATION_UPDATE_INTERVAL_MS,
                minDistanceThreshold = PeekTransitConstants.LOCATION_UPDATE_MIN_DISTANCE_METERS,
                callback = { newLocation ->
                    println("MainViewModel: Location update callback - ${newLocation.latitude}, ${newLocation.longitude}")
                    _currentLocation.postValue(newLocation)
                    
                    val shouldRefreshStops = if (previousLocation != null) {
                        val distance = previousLocation!!.distanceTo(newLocation)
                        println("MainViewModel: Distance from previous location: ${distance}m (threshold: ${PeekTransitConstants.DISTANCE_CHANGE_ALLOWED_BEFORE_REFRESHING_STOPS_IN_METERS}m)")
                        distance > PeekTransitConstants.DISTANCE_CHANGE_ALLOWED_BEFORE_REFRESHING_STOPS_IN_METERS
                    } else {
                        println("MainViewModel: No previous location, will refresh stops")
                        true
                    }
                    
                    if (shouldRefreshStops) {
                        println("MainViewModel: Refreshing stops for new location")
                        loadStops(newLocation, forceRefresh = false)
                        previousLocation = newLocation
                    } else {
                        println("MainViewModel: Distance threshold not met, not refreshing stops")
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
    
    suspend fun getCurrentLocationForCamera(): Location? {
        return try {
            locationManager.getCurrentLocation()
        } catch (e: Exception) {
            println("MainViewModel: Failed to get current location for camera: ${e.message}")
            null
        }
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