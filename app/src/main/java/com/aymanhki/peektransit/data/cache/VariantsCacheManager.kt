package com.aymanhki.peektransit.data.cache

import android.content.Context
import android.content.SharedPreferences
import com.aymanhki.peektransit.data.models.Variant
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.util.*
import androidx.core.content.edit

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
                preferences.edit {
                    putString(cacheKey, cacheJson)
                }
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
        preferences.edit {
            remove(cacheKey)
                .remove(lastUpdateKey)
        }
    }

    fun clearAllData() {
        preferences.edit { clear() }
    }
}

