package com.aymanhki.peektransit.data.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Global cache for map preview snapshots with both memory and disk caching.
 * Uses a combination of location coordinates and direction as the cache key.
 * Snapshots are persisted to disk for faster loading on app restart.
 */
object MapSnapshotCache {
    private val memoryCache = ConcurrentHashMap<String, Bitmap>()
    private const val MAX_MEMORY_CACHE_SIZE = 300
    private const val MAX_DISK_CACHE_SIZE_MB = 100
    private const val CACHE_DIR_NAME = "map_snapshots"
    
    private var cacheDir: File? = null
    private var isInitialized = false
    
    /**
     * Initialize the cache with application context
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
        if (!cacheDir!!.exists()) {
            cacheDir!!.mkdirs()
        }
        isInitialized = true
        
        // Clean up old cache files if needed
        cleanupDiskCache()
    }
    
    /**
     * Generate a cache key based on location and direction
     */
    private fun generateKey(latitude: Double, longitude: Double, direction: String, isDarkMode: Boolean): String {
        // Round coordinates to 4 decimal places for reasonable precision while allowing caching
        val roundedLat = "%.4f".format(latitude)
        val roundedLng = "%.4f".format(longitude)
        val theme = if (isDarkMode) "dark" else "light"
        return "${roundedLat}_${roundedLng}_${direction.lowercase()}_$theme"
    }
    
    /**
     * Generate a safe filename from the cache key
     */
    private fun generateFileName(key: String): String {
        // Create MD5 hash for safe filename
        val md = MessageDigest.getInstance("MD5")
        val hashBytes = md.digest(key.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) } + ".png"
    }
    
    /**
     * Get cached snapshot if available (checks memory first, then disk)
     */
    suspend fun getCachedSnapshot(latitude: Double, longitude: Double, direction: String, isDarkMode: Boolean): Bitmap? {
        if (!isInitialized) return null
        
        val key = generateKey(latitude, longitude, direction, isDarkMode)
        
        // Check memory cache first
        memoryCache[key]?.let { return it }
        
        // Check disk cache
        return withContext(Dispatchers.IO) {
            try {
                val fileName = generateFileName(key)
                val file = File(cacheDir, fileName)
                
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        // Add to memory cache for faster future access
                        addToMemoryCache(key, bitmap)
                        return@withContext bitmap
                    }
                }
                null
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Cache a snapshot (saves to both memory and disk)
     */
    suspend fun cacheSnapshot(latitude: Double, longitude: Double, direction: String, isDarkMode: Boolean, bitmap: Bitmap) {
        if (!isInitialized) return
        
        val key = generateKey(latitude, longitude, direction, isDarkMode)
        
        // Add to memory cache
        addToMemoryCache(key, bitmap)
        
        // Save to disk cache
        withContext(Dispatchers.IO) {
            try {
                val fileName = generateFileName(key)
                val file = File(cacheDir, fileName)
                
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                }
            } catch (e: Exception) {
                // Handle disk write errors silently
            }
        }
    }
    
    /**
     * Add bitmap to memory cache with size management
     */
    private fun addToMemoryCache(key: String, bitmap: Bitmap) {
        // If memory cache is getting too large, remove oldest entries
        if (memoryCache.size >= MAX_MEMORY_CACHE_SIZE) {
            val keysToRemove = memoryCache.keys.take(10)
            keysToRemove.forEach { memoryCache.remove(it) }
        }
        
        memoryCache[key] = bitmap
    }
    
    /**
     * Clean up disk cache if it exceeds size limit
     */
    private fun cleanupDiskCache() {
        try {
            val dir = cacheDir ?: return
            val files = dir.listFiles() ?: return
            
            // Calculate total size
            val totalSize = files.sumOf { it.length() }
            val maxSizeBytes = MAX_DISK_CACHE_SIZE_MB * 1024 * 1024
            
            if (totalSize > maxSizeBytes) {
                // Sort by last modified (oldest first) and delete until under limit
                val sortedFiles = files.sortedBy { it.lastModified() }
                var currentSize = totalSize
                
                for (file in sortedFiles) {
                    if (currentSize <= maxSizeBytes) break
                    currentSize -= file.length()
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // Handle cleanup errors silently
        }
    }
    
    /**
     * Clear all cached snapshots (both memory and disk)
     */
    fun clearCache() {
        memoryCache.clear()
        
        try {
            cacheDir?.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            // Handle cleanup errors silently
        }
    }
    
    /**
     * Get current memory cache size
     */
    fun getMemoryCacheSize(): Int = memoryCache.size
    
    /**
     * Get disk cache size in MB
     */
    fun getDiskCacheSizeMB(): Double {
        return try {
            val dir = cacheDir ?: return 0.0
            val files = dir.listFiles() ?: return 0.0
            val totalBytes = files.sumOf { it.length() }
            totalBytes / (1024.0 * 1024.0)
        } catch (e: Exception) {
            0.0
        }
    }
    
    /**
     * Check if a snapshot is cached (checks both memory and disk)
     */
    suspend fun isSnapshotCached(latitude: Double, longitude: Double, direction: String, isDarkMode: Boolean): Boolean {
        if (!isInitialized) return false
        
        val key = generateKey(latitude, longitude, direction, isDarkMode)
        
        // Check memory cache first
        if (memoryCache.containsKey(key)) return true
        
        // Check disk cache
        return withContext(Dispatchers.IO) {
            try {
                val fileName = generateFileName(key)
                val file = File(cacheDir, fileName)
                file.exists()
            } catch (e: Exception) {
                false
            }
        }
    }
}