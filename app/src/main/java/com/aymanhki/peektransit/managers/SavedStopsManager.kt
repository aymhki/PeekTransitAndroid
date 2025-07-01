package com.aymanhki.peektransit.managers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.aymanhki.peektransit.data.cache.VariantsCacheManager
import com.aymanhki.peektransit.data.models.SavedStop
import com.aymanhki.peektransit.data.models.Stop
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SavedStopsManager private constructor(context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: SavedStopsManager? = null

        @Volatile
        private var variantsCacheManager: VariantsCacheManager? = null


        fun getInstance(context: Context): SavedStopsManager {
            if (variantsCacheManager == null) {
                variantsCacheManager = VariantsCacheManager.getInstance(context.applicationContext)
            }

            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SavedStopsManager(context.applicationContext).also { 
                    INSTANCE = it 
                }
            }


        }
    }
    
    private val preferences: SharedPreferences = context.getSharedPreferences(
        "savedStops", Context.MODE_PRIVATE
    )
    private val gson = GsonBuilder()
        .serializeSpecialFloatingPointValues()
        .create()
    private val userDefaultsKey = "savedStops"
    
    private val _savedStops = MutableStateFlow<List<SavedStop>>(emptyList())
    val savedStops: StateFlow<List<SavedStop>> = _savedStops.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadSavedStops()
    }
    
    fun loadSavedStops() {
        _isLoading.value = true
        
        try {
            val savedStopsJson = preferences.getString(userDefaultsKey, null)
            if (savedStopsJson != null) {
                val type = object : TypeToken<List<SavedStop>>() {}.type
                val stops: List<SavedStop> = gson.fromJson(savedStopsJson, type) ?: emptyList()
                _savedStops.value = stops
                Log.d("SavedStopsManager", "Loaded ${stops.size} saved stops")
            } else {
                _savedStops.value = emptyList()
                Log.d("SavedStopsManager", "No saved stops found")
            }
        } catch (e: Exception) {
            Log.e("SavedStopsManager", "Error loading saved stops: ${e.message}", e)
            _savedStops.value = emptyList()
        } finally {
            _isLoading.value = false
        }
    }
    
    private fun saveToDisk() {
        try {
            val savedStopsJson = gson.toJson(_savedStops.value)
            preferences.edit()
                .putString(userDefaultsKey, savedStopsJson)
                .commit()
            Log.d("SavedStopsManager", "Saved ${_savedStops.value.size} stops to disk")
        } catch (e: Exception) {
            Log.e("SavedStopsManager", "Error saving stops to disk: ${e.message}", e)
        }
    }
    
    fun isStopSaved(stop: Stop): Boolean {
        return _savedStops.value.any { it.id == stop.number.toString() }
    }
    
    fun toggleSavedStatus(stop: Stop) {
        if (stop.variants.isEmpty()) {
            stop.variants = variantsCacheManager?.getCachedVariants(stop.number) ?: emptyList()
        }

        val stopId = stop.number.toString()
        val currentStops = _savedStops.value.toMutableList()
        
        val existingIndex = currentStops.indexOfFirst { it.id == stopId }
        if (existingIndex != -1) {
            currentStops.removeAt(existingIndex)
            Log.d("SavedStopsManager", "Removed stop ${stop.number} from saved stops")
        } else {
            currentStops.add(SavedStop(stopData = stop))
            Log.d("SavedStopsManager", "Added stop ${stop.number} to saved stops")
        }
        
        _savedStops.value = currentStops
        saveToDisk()
    }
    
    fun removeStop(indexSet: Set<Int>) {
        val currentStops = _savedStops.value.toMutableList()
        
        val sortedIndices = indexSet.sortedDescending()
        
        for (index in sortedIndices) {
            if (index in currentStops.indices) {
                val removedStop = currentStops.removeAt(index)
                Log.d("SavedStopsManager", "Removed stop ${removedStop.stopData.number} at index $index")
            }
        }
        
        _savedStops.value = currentStops
        saveToDisk()
    }
    
    fun addStop(stop: Stop) {
        val currentStops = _savedStops.value.toMutableList()
        val stopId = stop.number.toString()
        
        currentStops.removeAll { it.id == stopId }
        
        currentStops.add(0, SavedStop(stopData = stop))
        
        _savedStops.value = currentStops
        saveToDisk()
        Log.d("SavedStopsManager", "Added/updated stop ${stop.number}")
    }
    
    fun removeStop(stop: Stop) {
        val stopId = stop.number.toString()
        val currentStops = _savedStops.value.toMutableList()
        
        currentStops.removeAll { it.id == stopId }
        
        _savedStops.value = currentStops
        saveToDisk()
        Log.d("SavedStopsManager", "Removed stop ${stop.number}")
    }
    
    fun clearAllSavedStops() {
        _savedStops.value = emptyList()
        preferences.edit().remove(userDefaultsKey).commit()
        Log.d("SavedStopsManager", "Cleared all saved stops")
    }
}