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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

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

    private val locationMutex = Mutex()

    private var locationFetchJob: Job? = null
    private var cameraLocationJob: Job? = null

    private val _isCameraPositioned = MutableLiveData(false)
    val isCameraPositioned: LiveData<Boolean> = _isCameraPositioned

    fun loadStops(userLocation: Location, loadingFromWidgetSetup: Boolean = false, forceRefresh: Boolean = false) {
        if (_isLoadingStops.value == true) return

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
        if (_isInitialized.value == true) return

        if (locationManager.hasLocationPermission()) {
            fetchLocationOnce(true)
        }
    }

    fun fetchLocationForCamera() {
        if (cameraLocationJob?.isActive == true) return

        cameraLocationJob = viewModelScope.launch {
            locationMutex.withLock {
                if (_isCameraPositioned.value == true) return@withLock

                try {
                    val location = locationManager.getCurrentLocation(forceRefresh = true)
                    if (location != null) {
                        _currentLocation.postValue(location)
                        _isCameraPositioned.postValue(true)
                    }
                } catch (e: Exception) {

                }
            }
        }
    }

    private fun fetchLocationOnce(forceRefresh: Boolean = false) {
        if (locationFetchJob?.isActive == true) return

        locationFetchJob = viewModelScope.launch {
            locationMutex.withLock {
                _isLoadingLocation.postValue(true)
                try {
                    val location = locationManager.getCurrentLocation(forceRefresh)
                    if (location != null) {
                        _currentLocation.postValue(location)

                        if (_isInitialized.value != true) {
                            initializeWithLocation(location)
                        } else if (shouldUpdateStopsForLocation(location)) {
                            loadStops(location, forceRefresh = forceRefresh)
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
    }

    private fun shouldUpdateStopsForLocation(location: Location): Boolean {
        return previousLocation?.let {
            val distance = it.distanceTo(location)
            distance > PeekTransitConstants.DISTANCE_CHANGE_ALLOWED_BEFORE_REFRESHING_STOPS_IN_METERS
        } ?: true
    }

    fun retry() {
        clearError()
        clearLocationError()
        fetchLocationOnce(forceRefresh = true)
    }

    fun clearLocationError() {
        _locationError.postValue(null)
    }

    fun resetCameraPosition() {
        _isCameraPositioned.postValue(false)
    }

    private fun startLocationMonitoring() {
        if (!isLocationMonitoringActive && locationManager.hasLocationPermission()) {
            isLocationMonitoringActive = true
            locationManager.startLocationUpdates(
                updateInterval = PeekTransitConstants.LOCATION_UPDATE_INTERVAL_MS,
                minDistanceThreshold = PeekTransitConstants.LOCATION_UPDATE_MIN_DISTANCE_METERS,
                callback = { newLocation ->
                    _currentLocation.postValue(newLocation)

                    if (shouldUpdateStopsForLocation(newLocation)) {
                        loadStops(newLocation, forceRefresh = false)
                        previousLocation = newLocation
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
        return locationManager.getCurrentLocation(forceRefresh = true)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.postValue(query.trim())
    }

    fun clearSearchQuery() {
        _searchQuery.postValue("")
        _lastSearchedQuery.postValue("")
    }

    fun searchForStops(query: String, userLocation: Location? = null) {
        viewModelScope.launch {
            stopsDataStore.searchForStops(query.trim(), userLocation)
            _lastSearchedQuery.postValue(query.trim())
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

    fun setCameraPositioned(positioned: Boolean) {
        _isCameraPositioned.postValue(positioned)
    }

    suspend fun fetchLocationWithTimeout(timeoutMs: Long): Location? {
        return withTimeoutOrNull(timeoutMs) {
            locationManager.getCurrentLocation(forceRefresh = true)
        }
    }
}
