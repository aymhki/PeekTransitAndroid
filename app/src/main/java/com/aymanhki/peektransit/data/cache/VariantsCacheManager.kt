package com.aymanhki.peektransit.data.cache

import android.content.Context
import android.content.SharedPreferences
import com.aymanhki.peektransit.data.models.Variant
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.util.*

class VariantsCacheManager private constructor(context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: VariantsCacheManager? = null
        
        fun getInstance(context: Context): VariantsCacheManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VariantsCacheManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val preferences: SharedPreferences = context.getSharedPreferences(
        "transit_variants_cache", Context.MODE_PRIVATE
    )
    
    private val gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        .create()
    
    private val cacheKey = "transit_variants_cache"
    private val lastUpdateKey = "transit_variants_last_update"
    
    private var cache: MutableMap<String, List<Variant>>
        get() {
            val cacheJson = preferences.getString(cacheKey, null)
            return if (cacheJson != null) {
                try {
                    val type = object : TypeToken<MutableMap<String, List<Variant>>>() {}.type
                    gson.fromJson(cacheJson, type) ?: mutableMapOf()
                } catch (e: Exception) {
                    println("VariantsCacheManager decode error: ${e.message}")
                    mutableMapOf()
                }
            } else {
                mutableMapOf()
            }
        }
        set(value) {
            try {
                val cacheJson = gson.toJson(value)
                preferences.edit()
                    .putString(cacheKey, cacheJson)
                    .apply()
            } catch (e: Exception) {
                println("VariantsCacheManager encode error: ${e.message}")
            }
        }
    
    fun getCachedVariants(stopNumber: Int): List<Variant>? {
        return cache[stopNumber.toString()]
    }
    
    fun cacheVariants(variants: List<Variant>, stopNumber: Int) {
        val currentCache = cache.toMutableMap()
        currentCache[stopNumber.toString()] = variants
        cache = currentCache
    }
    
    fun clearAllCaches() {
        preferences.edit()
            .remove(cacheKey)
            .remove(lastUpdateKey)
            .apply()
    }
}

class RouteCacheManager private constructor(context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: RouteCacheManager? = null
        
        fun getInstance(context: Context): RouteCacheManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RouteCacheManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val preferences: SharedPreferences = context.getSharedPreferences(
        "transit_route_cache", Context.MODE_PRIVATE
    )
    private val gson = Gson()
    
    private val cacheKey = "route_cache"
    private val lastUpdateKey = "route_last_update"
    
    data class Route(
        val key: String,
        val name: String,
        val number: String,
        val backgroundColor: String?,
        val textColor: String?,
        val borderColor: String?
    )
    
    fun getCachedRoute(routeKey: String): Route? {
        val cacheJson = preferences.getString(cacheKey, null) ?: return null
        
        return try {
            val type = object : TypeToken<Map<String, Route>>() {}.type
            val cache: Map<String, Route> = gson.fromJson(cacheJson, type)
            cache[routeKey]
        } catch (e: Exception) {
            null
        }
    }
    
    fun cacheRoute(route: Route) {
        try {
            val cacheJson = preferences.getString(cacheKey, "{}")
            val type = object : TypeToken<MutableMap<String, Route>>() {}.type
            val cache: MutableMap<String, Route> = gson.fromJson(cacheJson, type) ?: mutableMapOf()
            
            cache[route.key] = route
            
            val updatedCacheJson = gson.toJson(cache)
            preferences.edit()
                .putString(cacheKey, updatedCacheJson)
                .apply()
                
        } catch (e: Exception) {
            println(e.message)
        }
    }
    
    fun getAllCachedRoutes(): Map<String, Route> {
        val cacheJson = preferences.getString(cacheKey, null) ?: return emptyMap()
        
        return try {
            val type = object : TypeToken<Map<String, Route>>() {}.type
            gson.fromJson(cacheJson, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    fun clearAllCaches() {
        preferences.edit()
            .remove(cacheKey)
            .remove(lastUpdateKey)
            .apply()
    }
    
    fun getLastUpdateTime(): Date? {
        val timestamp = preferences.getLong(lastUpdateKey, -1)
        return if (timestamp != -1L) Date(timestamp) else null
    }
    
    fun updateLastUpdateTime() {
        preferences.edit()
            .putLong(lastUpdateKey, System.currentTimeMillis())
            .apply()
    }
}