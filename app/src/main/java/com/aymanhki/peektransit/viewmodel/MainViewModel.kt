package com.aymanhki.peektransit.viewmodel

import android.app.Activity
import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aymanhki.peektransit.data.models.Stop
import com.aymanhki.peektransit.data.models.TransitError
import com.aymanhki.peektransit.data.repository.StopsDataStore
import com.aymanhki.peektransit.managers.RateAppBannerManager
import com.aymanhki.peektransit.managers.SavedStopsManager
import com.aymanhki.peektransit.managers.TipBannerManager
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.location.LocationManagerProvider
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull



class MainViewModel(application: Application) : AndroidViewModel(application) {
    val stopsDataStore = StopsDataStore.getInstance().apply { initialize(application.applicationContext) }

    private val _isInitialized = MutableLiveData(false)
    val isInitialized: LiveData<Boolean> = _isInitialized
    private val _isLoadingLocation = MutableLiveData(false)
    val isLoadingLocation: LiveData<Boolean> = _isLoadingLocation
    private val _isLoadingStops = MutableLiveData(false)
    val isLoadingStops: LiveData<Boolean> = _isLoadingStops
    private val _locationError = MutableLiveData<TransitError?>(null)
    val locationError: LiveData<TransitError?> = _locationError
    private val _currentLocation = MutableLiveData<Location?>()
    val currentLocation: LiveData<Location?> = _currentLocation
    private val _cameraLocation = MutableLiveData<Location?>()
    val cameraLocation: LiveData<Location?> = _cameraLocation
    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery
    private val _lastSearchedQuery = MutableLiveData("")
    val lastSearchedQuery: LiveData<String> = _lastSearchedQuery
    private val _hasPerformedInitialCameraAnimation = MutableLiveData(false)
    val hasPerformedInitialCameraAnimation: LiveData<Boolean> = _hasPerformedInitialCameraAnimation
    private val _showSupportSheet = MutableLiveData(false)
    val showSupportSheet: LiveData<Boolean> = _showSupportSheet
    private val _isCameraPositioned = MutableLiveData(false)
    val isCameraPositioned: LiveData<Boolean> = _isCameraPositioned
    private val _hasInitialLocation = MutableLiveData(false)
    val hasInitialLocation: LiveData<Boolean> = _hasInitialLocation
    private val _lastKnownCameraPosition = MutableLiveData<CameraPosition?>(null)
    val lastKnownCameraPosition: LiveData<CameraPosition?> = _lastKnownCameraPosition
    private val _thereIsAnUpdateAvailable = MutableLiveData(false)
    val thereIsAnUpdateAvailable: LiveData<Boolean> = _thereIsAnUpdateAvailable
    private val _theUserHasClickedOnTheUpdateAvailableBanner = MutableLiveData(false)
    val theUserHasClickedOnTheUpdateAvailableBanner: LiveData<Boolean> = _theUserHasClickedOnTheUpdateAvailableBanner
    private val _startUpdateFlow = MutableLiveData<Boolean>()
    val startUpdateFlow: LiveData<Boolean> = _startUpdateFlow
    private lateinit var appUpdateManager: AppUpdateManager
    val tipBannerManager = TipBannerManager.getInstance(application.applicationContext)
    val rateAppBannerManager = RateAppBannerManager.getInstance(application.applicationContext)
    val shouldShowRateAppBanner: LiveData<Boolean> = rateAppBannerManager.shouldShowRateAppBanner
    val hasShownRateAppBannerThisSession: LiveData<Boolean> = rateAppBannerManager.hasShownRateAppBannerThisSession
    val wasRateAppBannerManuallyHidden: LiveData<Boolean> = rateAppBannerManager.wasRateAppBannerManuallyHidden
    val shouldShowTipBanner: LiveData<Boolean> = tipBannerManager.shouldShowTipBanner
    val hasShownTipBannerThisSession: LiveData<Boolean> = tipBannerManager.hasShownTipBannerThisSession
    val wasTipBannerManuallyHidden: LiveData<Boolean> = tipBannerManager.wasTipBannerManuallyHidden
    val attemptedToStartTipBannerUsageTracking: LiveData<Boolean> = tipBannerManager.attemptedToStartUsageTracking
    val attemptedToStartRateAppBannerUsageTracking: LiveData<Boolean> = rateAppBannerManager.attemptedToStartUsageTracking
    private val _showInAppReview = MutableLiveData(false)
    val showInAppReview: LiveData<Boolean> = _showInAppReview
    private val _isSearchingDestination = MutableLiveData(false)
    val isSearchingDestination: LiveData<Boolean> = _isSearchingDestination

    val stops: LiveData<List<Stop>> = stopsDataStore.stops
    val isLoading: LiveData<Boolean> = stopsDataStore.isLoading
    val error: LiveData<TransitError?> = stopsDataStore.error
    val searchResults: LiveData<List<Stop>> = stopsDataStore.searchResults
    val isSearching: LiveData<Boolean> = stopsDataStore.isSearching
    val searchError: LiveData<TransitError?> = stopsDataStore.searchError
    private val locationMutex = Mutex()
    private var locationFetchJob: Job? = null
    private var cameraLocationJob: Job? = null
    private val locationManager = LocationManagerProvider.getInstance(application)
    private var isLocationMonitoringActive = false
    private var previousLocation: Location? = null

    fun setInitialCameraAnimationPerformed() {
        _hasPerformedInitialCameraAnimation.postValue(true)
    }

    fun saveLastCameraPosition(position: CameraPosition) {
        _lastKnownCameraPosition.postValue(position)
    }

    private fun checkIfThereIsAnUpdate(): Boolean {
        return try {
            appUpdateManager = AppUpdateManagerFactory.create(getApplication())
            val appUpdateInfoTask = appUpdateManager.appUpdateInfo

            appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                val isUpdateAvailable = appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                val isImmediateUpdateAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                val isFlexibleUpdateAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

                val updateAvailable = isUpdateAvailable && (isImmediateUpdateAllowed || isFlexibleUpdateAllowed)
                _thereIsAnUpdateAvailable.postValue(updateAvailable)
            }.addOnFailureListener {
                Log.d("MainViewModel", "Failed to check for update: ${it.message}")
                _thereIsAnUpdateAvailable.postValue(false)
            }

            false
        } catch (e: Exception) {
            Log.d("MainViewModel", "Error checking for update: ${e.message}")
            _thereIsAnUpdateAvailable.postValue(false)
            false
        }
    }


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

    fun showSupportSheet() {
        _showSupportSheet.postValue(true)
    }

    fun hideSupportSheet() {
        _showSupportSheet.postValue(false)
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
            fetchLocationOnce(false)
        }

        checkIfThereIsAnUpdate()

        if (attemptedToStartRateAppBannerUsageTracking.value == false) {
            // rateAppBannerManager.startTrackingAppUsage()
        }

        if (attemptedToStartTipBannerUsageTracking.value == false) {
            // tipBannerManager.startTrackingAppUsage()
        }

        // initialize saved stops manager to trigger any migrations if needed
        SavedStopsManager.getInstance(getApplication())

        // _isInitialized.postValue(true)
    }

    fun onUpdateFlowStarted() {
        _startUpdateFlow.postValue(false)
    }

    fun fetchLocationForCamera() {
        if (cameraLocationJob?.isActive == true) return

        cameraLocationJob = viewModelScope.launch {
            locationMutex.withLock {
                if (_isCameraPositioned.value == true) return@withLock

                try {
                    _isLoadingLocation.postValue(true)

                    val cachedLocation = getCachedLocationSync()
                    if (cachedLocation != null) {
                        _cameraLocation.postValue(cachedLocation)
                        _isCameraPositioned.postValue(true)
                        _hasInitialLocation.postValue(true)

                        if (!isLocationRecent(cachedLocation)) {
                            fetchFreshLocationInBackground()
                        }
                        return@withLock
                    }

                    val location = withTimeoutOrNull(PeekTransitConstants.MAIN_VIEW_MODEL_INITIAL_LOCATION_REQUEST_TIMEOUT_MS) {
                        locationManager.getCurrentLocation(forceRefresh = true)
                    }

                    if (location != null) {
                        _cameraLocation.postValue(location)
                        _currentLocation.postValue(location)
                        _isCameraPositioned.postValue(true)
                        _hasInitialLocation.postValue(true)

                        if (_isInitialized.value != true) {
                            initializeWithLocation(location)
                        }
                    } else {
                        _hasInitialLocation.postValue(false)
                    }
                } catch (e: Exception) {
                    _locationError.postValue(TransitError.NetworkError(e))
                    _hasInitialLocation.postValue(false)
                } finally {
                    _isLoadingLocation.postValue(false)
                }
            }
        }
    }

    private suspend fun getCachedLocationSync(): Location? {
        return try {
            locationManager.getCachedLocation()
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchFreshLocationInBackground() {
        viewModelScope.launch {
            try {
                val location = locationManager.getCurrentLocation(forceRefresh = true)
                if (location != null) {
                    _currentLocation.postValue(location)
                    if (shouldUpdateStopsForLocation(location)) {
                        loadStops(location, forceRefresh = false)
                        previousLocation = location
                    }
                }
            } catch (e: Exception) {
                System.out.println("Error fetching fresh location in background: ${e.message}")
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
                        _hasInitialLocation.postValue(true)

                        if (_isInitialized.value != true) {
                            initializeWithLocation(location)
                        } else if (shouldUpdateStopsForLocation(location)) {
                            loadStops(location, forceRefresh = forceRefresh)
                        }
                        previousLocation = location
                    } else {
                        _locationError.postValue(TransitError.NetworkError(Exception("Unable to get location. Please check location permissions and settings.")))
                        _hasInitialLocation.postValue(false)
                    }
                } catch (e: Exception) {
                    _locationError.postValue(TransitError.NetworkError(e))
                    _hasInitialLocation.postValue(false)
                } finally {
                    _isLoadingLocation.postValue(false)
                }
            }
        }
    }

    private fun isLocationRecent(location: Location): Boolean {
        val ageInMinutes = (System.currentTimeMillis() - location.time) / (1000 * 60)
        return ageInMinutes < 1
    }

    private fun shouldUpdateStopsForLocation(location: Location): Boolean {
        return previousLocation?.let {
            val distance = it.distanceTo(location)
            distance > PeekTransitConstants.DISTANCE_CHANGE_ALLOWED_BEFORE_REFRESHING_STOPS_IN_METERS
        } ?: true
    }

    fun retryLocationFetch() {
        clearError()
        clearLocationError()
        fetchLocationOnce(forceRefresh = true)
    }

    fun tipBannerWasTapped() {
        tipBannerManager.tipBannerWasTapped()
        _showSupportSheet.postValue(true)
    }

    fun rateAppBannerWasTapped() {
        rateAppBannerManager.rateAppBannerWasTapped()
        _showInAppReview.postValue(true)
    }

    fun onInAppReviewFlowStarted() {
        _showInAppReview.postValue(false)
    }

    fun updateBannerWasTapped() {
        _theUserHasClickedOnTheUpdateAvailableBanner.postValue(true)
        _startUpdateFlow.postValue(true)
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

    suspend fun fetchLocationWithTimeout(timeoutMs: Long): Location? {
        return withTimeoutOrNull(timeoutMs) {
            locationManager.getCurrentLocation(forceRefresh = true)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.postValue(query.trim())
    }

    fun clearSearchQuery() {
        _searchQuery.postValue("")
        _lastSearchedQuery.postValue("")
    }

    fun searchForStops(query: String) {
        viewModelScope.launch {
            stopsDataStore.searchForStops(query.trim())
            _lastSearchedQuery.postValue(query.trim())
        }
    }

    fun setIsSearchingDestination(searching: Boolean) {
        _isSearchingDestination.postValue(searching)
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
        rateAppBannerManager.stopTrackingAppUsage()
        tipBannerManager.stopTrackingAppUsage()
    }

    fun setCameraPositioned(positioned: Boolean) {
        _isCameraPositioned.postValue(positioned)
    }
}
