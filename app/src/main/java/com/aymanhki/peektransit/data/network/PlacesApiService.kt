package com.aymanhki.peektransit.services

import android.content.Context
import com.aymanhki.peektransit.data.models.TransitError
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.*
import com.google.android.libraries.places.api.net.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class PlacesApiService private constructor(context: Context) {
    private var placesClient: PlacesClient
    private var sessionToken: AutocompleteSessionToken = AutocompleteSessionToken.newInstance()

    init {
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(context.applicationContext, PeekTransitConstants.GOOGLE_PLACES_API_KEY)
        }
        placesClient = Places.createClient(context)
    }

    suspend fun getAutocompletePredictions(
        query: String,
        bounds: RectangularBounds? = null
    ): List<AutocompletePrediction> = withContext(Dispatchers.IO) {
        try {
            val request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(sessionToken)
                .setQuery(query)
                .setCountries("CA")
                .also { builder ->
                    bounds?.let { builder.setLocationBias(it) }
                }
                .build()

            val response = placesClient.findAutocompletePredictions(request).await()
            return@withContext response.autocompletePredictions
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            throw TransitError.NetworkError(e)
        }
    }

    fun completeSession() {
        sessionToken = AutocompleteSessionToken.newInstance()
    }

    suspend fun getPlaceDetails(placeId: String): Place = withContext(Dispatchers.IO) {
        try {
            val placeFields = listOf(
                Place.Field.LOCATION
            )

            val request = FetchPlaceRequest.builder(placeId, placeFields)
                .setSessionToken(sessionToken)
                .build()

            val response = placesClient.fetchPlace(request).await()
            return@withContext response.place
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            throw TransitError.NetworkError(e)
        }
    }

    fun createWinnipegBounds(): RectangularBounds {
        val winnipegNE = LatLng(50.0, -96.7)
        val winnipegSW = LatLng(49.7, -97.4)
        return RectangularBounds.newInstance(winnipegSW, winnipegNE)
    }

    companion object {
        @Volatile
        private var INSTANCE: PlacesApiService? = null

        fun getInstance(context: Context): PlacesApiService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlacesApiService(context).also { INSTANCE = it }
            }
        }
    }
}
