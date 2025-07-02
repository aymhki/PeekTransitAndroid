package com.aymanhki.peektransit.data.repository

import android.content.Context
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.aymanhki.peektransit.data.models.Stop
import com.aymanhki.peektransit.data.models.TransitError
import com.aymanhki.peektransit.data.cache.VariantsCacheManager
import com.aymanhki.peektransit.data.network.WinnipegTransitAPI
import com.aymanhki.peektransit.utils.PeekTransitConstants
import kotlinx.coroutines.*

class StopsDataStore private constructor() {
    private val api = WinnipegTransitAPI.getInstance()
    private var variantsCache: VariantsCacheManager? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var lastFetchTime: Long? = null
    private var lastFetchLocation: Location? = null
    private var cachedStops: List<Stop> = emptyList()
    private val cacheDuration: Long = 30000
    private val locationDistanceThreshold: Float = PeekTransitConstants.DISTANCE_CHANGE_ALLOWED_BEFORE_REFRESHING_STOPS.toFloat()
    
    private var searchJob: Job? = null
    private var loadStopsJob: Job? = null
    private var enrichmentJob: Job? = null
    private var isCurrentlyLoading = false
    
    private val _stops = MutableLiveData<List<Stop>>(emptyList())
    val stops: LiveData<List<Stop>> = _stops
    
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<TransitError?>(null)
    val error: LiveData<TransitError?> = _error
    
    private val _searchResults = MutableLiveData<List<Stop>>(emptyList())
    val searchResults: LiveData<List<Stop>> = _searchResults
    
    private val _isSearching = MutableLiveData(false)
    val isSearching: LiveData<Boolean> = _isSearching
    
    private val _searchError = MutableLiveData<TransitError?>(null)
    val searchError: LiveData<TransitError?> = _searchError
    
    fun initialize(context: Context) {
        if (variantsCache == null) {
            variantsCache = VariantsCacheManager.getInstance(context)
        }
    }
    
    private fun updateCache(location: Location) {
        lastFetchTime = System.currentTimeMillis()
        lastFetchLocation = location
        cachedStops = _stops.value ?: emptyList()
    }
    
    private fun isCacheValid(location: Location): Boolean {
        val lastTime = lastFetchTime ?: return false
        val lastLocation = lastFetchLocation ?: return false
        
        val timeValid = System.currentTimeMillis() - lastTime < cacheDuration
        val distanceValid = lastLocation.distanceTo(location) < locationDistanceThreshold
        
        return timeValid && distanceValid && cachedStops.isNotEmpty()
    }
    
    suspend fun loadStops(userLocation: Location, loadingFromWidgetSetup: Boolean = false, forceRefresh: Boolean = false) {
        if (isCurrentlyLoading) return
        
//        if (!forceRefresh && isCacheValid(userLocation)) {
//            _stops.postValue(cachedStops)
//            _error.postValue(null)
//            return
//        }
        
        loadStopsJob?.cancel()
        isCurrentlyLoading = true
        
        loadStopsJob = scope.launch {
            try {
                _searchResults.postValue(emptyList())
                _isLoading.postValue(true)
                _error.postValue(null)
                
                val nearbyStops = api.getNearbyStops(userLocation, PeekTransitConstants.GLOBAL_API_FOR_SHORT_USAGE)
                
                _stops.postValue(nearbyStops)
                
                if (!loadingFromWidgetSetup) {
                    _isLoading.postValue(false)
                    if (nearbyStops.isEmpty()) {
                        _error.postValue(TransitError.ParseError("No stops could be loaded"))
                    }
                }
                
                if (loadingFromWidgetSetup) {
                    enrichStops(nearbyStops)
                    _isLoading.postValue(false)
                    updateCache(userLocation)
                    
                    if (nearbyStops.isEmpty()) {
                        _error.postValue(TransitError.ParseError("No stops could be loaded"))
                    }
                } else {
                    enrichmentJob?.cancel()
                    enrichmentJob = scope.launch {
                        try {
                            enrichStops(nearbyStops)
                            updateCache(userLocation)
                        } catch (e: Exception) {
                            if (e !is CancellationException) {
                                println("StopsDataStore: Error in background enrichment: ${e.message}")
                            }
                        }
                    }
                }
                
            } catch (e: Exception) {
                val transitError = when (e) {
                    is TransitError -> e
                    is CancellationException -> return@launch
                    else -> TransitError.NetworkError(e)
                }
                
                if (_stops.value?.isEmpty() == true) {
                    _error.postValue(transitError)
                }
                _isLoading.postValue(false)
            } finally {
                isCurrentlyLoading = false
            }
        }
    }
    
    private suspend fun enrichStops(stops: List<Stop>) {
        val stopsNeedingEnrichment = stops.filter { stop ->
            stop.variants.isEmpty()
        }
        
        if (stopsNeedingEnrichment.isEmpty()) return
        
        try {
            getVariantsForStops(stopsNeedingEnrichment) { enrichedStop ->
                withContext(Dispatchers.Main) {
                    val currentStops = _stops.value?.toMutableList() ?: mutableListOf()
                    val index = currentStops.indexOfFirst { it.number == enrichedStop.number }
                    if (index != -1) {
                        currentStops[index] = enrichedStop
                        val newStopsList = currentStops.toList()
                        _stops.postValue(newStopsList)
                    }
                    
                    val currentSearchResults = _searchResults.value?.toMutableList() ?: mutableListOf()
                    val searchIndex = currentSearchResults.indexOfFirst { it.number == enrichedStop.number }
                    if (searchIndex != -1) {
                        currentSearchResults[searchIndex] = enrichedStop
                        val newSearchResults = currentSearchResults.toList()
                        _searchResults.postValue(newSearchResults)
                    }
                }
            }
            
        } catch (e: Exception) {
            if (e !is CancellationException) {
                println("Error enriching stops: ${e.message}")

            }
        }
    }

    private suspend fun getVariantsForStops(
        stops: List<Stop>, 
        onStopEnriched: suspend (Stop) -> Unit
    ) {
        val cache = variantsCache
        
        for (stop in stops) {
            try {
                val cachedVariants = cache?.getCachedVariants(stop.number)
                
                val stopVariants = if (cachedVariants != null && cachedVariants.isNotEmpty()) {
                    cachedVariants
                } else {
                    val variants = api.getVariantsForStop(stop.number)
                    
                    val filteredVariants = variants.filter { variant ->
                        val key = variant.key
                        !(key.startsWith("S") || key.startsWith("W") || key.startsWith("I"))
                    }
                    
                    if (filteredVariants.isNotEmpty() && cache != null) {
                        cache.cacheVariants(filteredVariants, stop.number)
                    }
                    
                    filteredVariants
                }
                
                val enrichedStop = stop.copy(variants = stopVariants)
                onStopEnriched(enrichedStop)
                
            } catch (e: Exception) {
                if (e !is CancellationException) {

                    println("Error fetching variants for stop ${stop.number}: ${e.message}")


                    onStopEnriched(stop)
                }
            }
        }
        
        try {
            validateVariantCaches(stops.map { it.number })
        } catch (e: Exception) {
            println("Error validating variant caches: ${e.message}")
        }
    }
    
    private suspend fun validateVariantCaches(stopNumbers: List<Int>) {
        try {
            val bulkVariants = api.getBulkVariantsForStops(stopNumbers)
            val cache = variantsCache ?: return

            val bulkVariantIdentifiers = bulkVariants.mapNotNull { variant ->
                variant.key.split("-").firstOrNull()
            }.toSet()

            for (stopNumber in stopNumbers) {
                val cachedVariants = cache.getCachedVariants(stopNumber) ?: continue

                for (variant in cachedVariants) {
                    val variantIdentifier = variant.key.split("-").firstOrNull() ?: ""

                    if (!bulkVariantIdentifiers.contains(variantIdentifier)) {

                        println("Cache validation failed: ${variant.key} from stop $stopNumber not found in bulk variants")

                        cache.clearAllCaches()
                        return
                    }
                }
            }


            println("Variant cache validation passed")

        } catch (e: Exception) {

            println("Error during cache validation: ${e.message}")

        }
    }

    suspend fun searchForStops(query: String, userLocation: Location? = null) {
        searchJob?.cancel()
        
        if (query.isEmpty()) {
            _searchResults.postValue(emptyList())
            _isSearching.postValue(false)
            _searchError.postValue(null)
            return
        }
        
        searchJob = scope.launch {
            try {
                _isSearching.postValue(true)
                _searchError.postValue(null)
                
                delay(PeekTransitConstants.SEARCH_DEBOUNCE_DELAY_MS)
                
                val searchedStops = api.searchStops(query, PeekTransitConstants.GLOBAL_API_FOR_SHORT_USAGE)
                _searchResults.postValue(searchedStops)
                
                launch {
                    try {
                        enrichStops(searchedStops.filter { it.variants.isEmpty() })
                    } catch (e: Exception) {
                        println("Error enriching search results: ${e.message}")
                    }
                }
                
                _isSearching.postValue(false)
                
            } catch (e: Exception) {
                val transitError = when (e) {
                    is TransitError -> e
                    is CancellationException -> return@launch
                    else -> TransitError.NetworkError(e)
                }
                
                _searchError.postValue(transitError)
                _isSearching.postValue(false)
            }
        }
    }
    
    suspend fun getStop(stopNumber: Int): Stop? {
        _stops.value?.find { it.number == stopNumber }?.let { return it }
        _searchResults.value?.find { it.number == stopNumber }?.let { return it }
        
        return try {
            _isLoading.postValue(true)
            _error.postValue(null)
            null
            
        } catch (e: Exception) {
            val transitError = when (e) {
                is TransitError -> e
                else -> TransitError.NetworkError(e)
            }
            _error.postValue(transitError)
            null
        } finally {
            _isLoading.postValue(false)
        }
    }
    
    fun clearError() {
        _error.postValue(null)
    }
    
    fun clearSearchError() {
        _searchError.postValue(null)
    }
    
    fun clearSearchResults() {
        _searchResults.postValue(emptyList())
    }
    
    fun cancelAllOperations() {
        searchJob?.cancel()
        loadStopsJob?.cancel()
        enrichmentJob?.cancel()
        isCurrentlyLoading = false
    }
    
    companion object {
        @Volatile
        private var INSTANCE: StopsDataStore? = null
        
        fun getInstance(): StopsDataStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StopsDataStore().also { INSTANCE = it }
            }
        }
    }
}