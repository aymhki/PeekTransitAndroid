package com.aymanhki.peektransit.data.cache

import android.content.Context
import android.content.SharedPreferences
import com.aymanhki.peektransit.data.models.Stop
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.aymanhki.peektransit.data.cache.VariantsCacheManager


class BookmarkManager private constructor(context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: BookmarkManager? = null

        fun getInstance(context: Context): BookmarkManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BookmarkManager(context.applicationContext).also { 
                    INSTANCE = it
                    android.util.Log.d("BookmarkManager", "BookmarkManager instance created")


                }
            }
        }
    }
    
    private val preferences: SharedPreferences = context.getSharedPreferences(
        "bookmarked_stops", Context.MODE_PRIVATE
    )
    private val gson = Gson()
    private val bookmarksKey = "saved_stops"
    
    private val _bookmarkedStops = MutableStateFlow<List<Stop>>(loadBookmarkedStops())
    val bookmarkedStops: Flow<List<Stop>> = _bookmarkedStops.asStateFlow()


    private fun loadBookmarkedStops(): List<Stop> {
        val bookmarksJson = preferences.getString(bookmarksKey, null)
        if (bookmarksJson == null) {
            android.util.Log.d("BookmarkManager", "No bookmarks found in preferences")
            return emptyList()
        }
        
        return try {
            val type = object : TypeToken<List<Stop>>() {}.type
            val stops: List<Stop> = gson.fromJson(bookmarksJson, type) ?: emptyList()
            android.util.Log.d("BookmarkManager", "Loaded ${stops.size} bookmarked stops")
            stops
        } catch (e: Exception) {
            android.util.Log.e("BookmarkManager", "Error parsing bookmarked stops", e)
            emptyList()
        }
    }
    
    private fun saveBookmarkedStops(stops: List<Stop>) {
        try {
            val bookmarksJson = gson.toJson(stops)
            val success = preferences.edit()
                .putString(bookmarksKey, bookmarksJson)
                .commit() // Use commit() instead of apply() to ensure immediate persistence
            android.util.Log.d("BookmarkManager", "Saved ${stops.size} bookmarked stops, success: $success")
        } catch (e: Exception) {
            android.util.Log.e("BookmarkManager", "Error saving bookmarked stops", e)
            e.printStackTrace()
        }
    }
    
    fun isBookmarked(stopNumber: Int): Boolean {
        return _bookmarkedStops.value.any { it.number == stopNumber }
    }
    
    fun addBookmark(stop: Stop) {

        val currentBookmarks = _bookmarkedStops.value.toMutableList()
        
        // Remove if already exists (to update data)
        currentBookmarks.removeAll { it.number == stop.number }
        
        // Add to beginning of list
        currentBookmarks.add(0, stop)
        
        _bookmarkedStops.value = currentBookmarks
        saveBookmarkedStops(currentBookmarks)
    }
    
    fun removeBookmark(stopNumber: Int) {
        val currentBookmarks = _bookmarkedStops.value.toMutableList()
        currentBookmarks.removeAll { it.number == stopNumber }
        
        _bookmarkedStops.value = currentBookmarks
        saveBookmarkedStops(currentBookmarks)
    }
    
    fun toggleBookmark(stop: Stop): Boolean {


        return if (isBookmarked(stop.number)) {
            android.util.Log.d("BookmarkManager", "Removing bookmark for stop ${stop.number}")
            removeBookmark(stop.number)
            false
        } else {
            android.util.Log.d("BookmarkManager", "Adding bookmark for stop ${stop.number}")
            addBookmark(stop)
            true
        }
    }
    
    fun clearAllBookmarks() {
        _bookmarkedStops.value = emptyList()
        preferences.edit()
            .remove(bookmarksKey)
            .commit()
    }
    
    fun getBookmarkedStops(): List<Stop> {
        return _bookmarkedStops.value
    }
    
    fun reloadBookmarks() {
        val reloadedStops = loadBookmarkedStops()
        _bookmarkedStops.value = reloadedStops
        android.util.Log.d("BookmarkManager", "Bookmarks reloaded: ${reloadedStops.size} stops")
    }
}