package com.aymanhki.peektransit.data.repository

import android.content.Context
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.aymanhki.peektransit.data.models.Stop
import com.aymanhki.peektransit.data.models.TransitError
import com.aymanhki.peektransit.data.models.Variant
import com.aymanhki.peektransit.data.cache.VariantsCacheManager
import com.aymanhki.peektransit.data.network.WinnipegTransitAPI
import com.aymanhki.peektransit.utils.PeekTransitConstants
import kotlinx.coroutines.*
import java.util.*

class StopsDataStore private constructor() {
    private val api = WinnipegTransitAPI.getInstance()
    private var variantsCache: VariantsCacheManager? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Cache configuration (matching iOS implementation)
    private var lastFetchTime: Long? = null
    private var lastFetchLocation: Location? = null
    private var cachedStops: List<Stop> = emptyList()
    private val cacheDuration: Long = 30000 // 30 seconds like iOS
    private val locationDistanceThreshold: Float = PeekTransitConstants.DISTANCE_CHANGE_ALLOWED_BEFORE_REFRESHING_STOPS.toFloat() // Use consistent threshold
    
    // Current loading tasks
    private var searchJob: Job? = null
    private var loadStopsJob: Job? = null
    private var enrichmentJob: Job? = null
    private var isCurrentlyLoading = false
    
    // LiveData for UI observation
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
        
        // Check cache first (matching iOS behavior)
        if (!forceRefresh && isCacheValid(userLocation)) {
            _stops.postValue(cachedStops)
            _error.postValue(null)
            return
        }
        
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
                    // Start enrichment in background using the main scope, not the loadStopsJob scope
                    enrichmentJob?.cancel() // Cancel any previous enrichment
                    enrichmentJob = scope.launch {
                        try {
                            println("StopsDataStore: Starting background enrichment for ${nearbyStops.size} stops")
                            enrichStops(nearbyStops)
                            updateCache(userLocation)
                            println("StopsDataStore: Background enrichment completed")
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
                // Ensure UI updates happen on main thread
                withContext(Dispatchers.Main) {
                    // Real-time UI update - find and replace the stop in current list
                    val currentStops = _stops.value?.toMutableList() ?: mutableListOf()
                    val index = currentStops.indexOfFirst { it.number == enrichedStop.number }
                    if (index != -1) {
                        println("StopsDataStore: Updating stop ${enrichedStop.number} with ${enrichedStop.variants.size} variants")
                        currentStops[index] = enrichedStop
                        // Create a completely new list to ensure reference change and post it
                        val newStopsList = currentStops.toList()
                        _stops.postValue(newStopsList)
                        println("StopsDataStore: Updated stops list with enriched data")
                    } else {
                        println("StopsDataStore: Could not find stop ${enrichedStop.number} in current list")
                    }
                    
                    // Also update search results if applicable
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
                // Log error but don't fail the whole operation
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
                // Check cache first (matching iOS cache-first strategy)
                val cachedVariants = cache?.getCachedVariants(stop.number)
                
                val stopVariants = if (cachedVariants != null && cachedVariants.isNotEmpty()) {
                    cachedVariants
                } else {
                    // Fetch from API if not cached
                    val variants = api.getVariantsForStop(stop.number)
                    
                    // Filter out unwanted variants (matching iOS logic)
                    val filteredVariants = variants.filter { variant ->
                        val key = variant.key
                        !(key.startsWith("S") || key.startsWith("W") || key.startsWith("I"))
                    }
                    
                    // Cache the results (matching iOS implementation)
                    if (filteredVariants.isNotEmpty() && cache != null) {
                        cache.cacheVariants(filteredVariants, stop.number)
                    }
                    
                    filteredVariants
                }
                
                // Create enriched stop and notify callback for real-time UI update
                val enrichedStop = stop.copy(variants = stopVariants)
                onStopEnriched(enrichedStop)
                
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    println("Error fetching variants for stop ${stop.number}: ${e.message}")
                    // Still call callback with original stop to update UI
                    onStopEnriched(stop)
                }
            }
        }
        
        // After all stops are enriched, validate caches (matching iOS pattern)
        try {
            validateVariantCaches(stops.map { it.number })
        } catch (e: Exception) {
            println("Error validating variant caches: ${e.message}")
        }
    }
    
    // Add cache validation method like iOS
    private suspend fun validateVariantCaches(stopNumbers: List<Int>) {
        try {
            val bulkVariants = api.getBulkVariantsForStops(stopNumbers)
            val cache = variantsCache ?: return

            // Extract route identifiers from bulk variants (first part before "-")
            val bulkVariantIdentifiers = bulkVariants.mapNotNull { variant ->
                variant.key.split("-").firstOrNull()
            }.toSet()

            // Check each stop's cached variants
            for (stopNumber in stopNumbers) {
                val cachedVariants = cache.getCachedVariants(stopNumber) ?: continue

                for (variant in cachedVariants) {
                    val variantIdentifier = variant.key.split("-").firstOrNull() ?: ""

                    if (!bulkVariantIdentifiers.contains(variantIdentifier)) {
                        println("\n**********\n ${variant.key} from stop $stopNumber was not found in the bulk variants request \n**********\n")
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
                
                println("StopsDataStore: Starting search for query: '$query'")
                
                // Add debounce delay (matching iOS)
                delay(1000)
                
                // First, filter local stops (matching iOS combined search behavior)
                val currentStops = _stops.value ?: emptyList()
                println("StopsDataStore: Current local stops count: ${currentStops.size}")
                
                val filteredLocalStops = currentStops.filter { stop ->
                    stop.name.contains(query, ignoreCase = true) ||
                    stop.number.toString().contains(query) ||
                    stop.variants.any { variant -> 
                        variant.key.contains(query, ignoreCase = true) ||
                        variant.name.contains(query, ignoreCase = true)
                    }
                }
                println("StopsDataStore: Filtered local stops count: ${filteredLocalStops.size}")
                
                // Then, search API for additional stops
                println("StopsDataStore: Starting API search...")
                val searchedStops = api.searchStops(query, PeekTransitConstants.GLOBAL_API_FOR_SHORT_USAGE)
                println("StopsDataStore: API search returned ${searchedStops.size} stops")
                
                // Combine results, avoiding duplicates (matching iOS logic)
                val combinedStops = mutableListOf<Stop>()
                combinedStops.addAll(filteredLocalStops)
                
                val existingStopNumbers = filteredLocalStops.map { it.number }.toSet()
                for (stop in searchedStops) {
                    if (stop.number != -1 && !existingStopNumbers.contains(stop.number)) {
                        combinedStops.add(stop)
                    }
                }
                
                println("StopsDataStore: Combined search results count: ${combinedStops.size}")
                _searchResults.postValue(combinedStops)
                
                // Start enrichment for search results that need it
                launch {
                    try {
                        enrichStops(combinedStops.filter { it.variants.isEmpty() })
                    } catch (e: Exception) {
                        // Log but don't fail
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
        // First check if we already have this stop
        _stops.value?.find { it.number == stopNumber }?.let { return it }
        _searchResults.value?.find { it.number == stopNumber }?.let { return it }
        
        return try {
            _isLoading.postValue(true)
            
            // This would make an API call to get the specific stop
            // For now, return null to indicate not found
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