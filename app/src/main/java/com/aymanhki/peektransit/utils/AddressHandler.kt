package com.aymanhki.peektransit.utils

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.aymanhki.peektransit.data.models.TransitError
import com.aymanhki.peektransit.services.PlacesApiService
import com.google.android.libraries.places.api.model.AutocompletePrediction
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds

class AddressSearchHandler(context: Context) {
    private val placesApiService = PlacesApiService.getInstance(context)
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var searchJob: Job? = null
    private val _isSearching = MutableLiveData<Boolean>(false)
    val isSearching: LiveData<Boolean> = _isSearching
    private val _searchResults = MutableLiveData<List<AutocompletePrediction>>(emptyList())
    val searchResults: LiveData<List<AutocompletePrediction>> = _searchResults
    private val _error = MutableLiveData<TransitError?>(null)
    val error: LiveData<TransitError?> = _error
    private val debounceTime = 1000L
    private var lastSearchedQuery: String = ""


    fun updateSearchQuery(query: String) {
        searchJob?.cancel()
        _error.value = null
        _error.postValue(null)

        if (query.isEmpty()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            lastSearchedQuery = ""
            return
        }



        val newQueryNonSpaceCount = query.count { !it.isWhitespace() }
        val lastQueryNonSpaceCount = lastSearchedQuery.count { !it.isWhitespace() }

        val isFirstSearch = lastSearchedQuery.isEmpty() && query.length >= PeekTransitConstants.NUM_CHARS_TO_UPDATE_ADDRESS_SEARCH_QUERY_AFTER
        val hasSufficientNewChars = !lastSearchedQuery.isEmpty() && (newQueryNonSpaceCount - lastQueryNonSpaceCount >= PeekTransitConstants.NUM_CHARS_TO_UPDATE_ADDRESS_SEARCH_QUERY_AFTER)

        if (isFirstSearch || hasSufficientNewChars) {
            _isSearching.value = true
            searchJob = coroutineScope.launch {
                delay(debounceTime)
                lastSearchedQuery = query
                performSearch(query)
            }
        }
    }

    private suspend fun performSearch(query: String) {
        try {
            val bounds = placesApiService.createWinnipegBounds()
            val predictions = placesApiService.getAutocompletePredictions(query, bounds)
            _searchResults.postValue(predictions)
        } catch (e: Exception) {
            placesApiService.completeSession()

            if (e is CancellationException) {
                throw e
            }

            val error = if (e is TransitError) e else TransitError.NetworkError(e)
            _error.postValue(error)
        } finally {
            _isSearching.postValue(false)
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun onCleared() {
        searchJob?.cancel()
        coroutineScope.cancel()
    }
}
