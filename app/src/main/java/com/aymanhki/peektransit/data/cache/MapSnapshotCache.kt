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

object MapSnapshotCache {
    private val memoryCache = ConcurrentHashMap<String, Bitmap>()
    private const val MAX_MEMORY_CACHE_SIZE = 500
    private const val MAX_DISK_CACHE_SIZE_MB = 300
    private const val CACHE_DIR_NAME = "map_snapshots"
    
    private var cacheDir: File? = null
    private var isInitialized = false
    
    fun initialize(context: Context) {
        if (isInitialized) return
        
        cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
        if (!cacheDir!!.exists()) {
            cacheDir!!.mkdirs()
        }
        isInitialized = true
        
        cleanupDiskCache()
    }
    
    private fun generateKey(latitude: Double, longitude: Double, direction: String, isDarkMode: Boolean): String {
        val roundedLat = "%.4f".format(latitude)
        val roundedLng = "%.4f".format(longitude)
        val theme = if (isDarkMode) "dark" else "light"
        return "${roundedLat}_${roundedLng}_${direction.lowercase()}_$theme"
    }
    
    private fun generateFileName(key: String): String {
        val md = MessageDigest.getInstance("MD5")
        val hashBytes = md.digest(key.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) } + ".png"
    }
    
    suspend fun getCachedSnapshot(latitude: Double, longitude: Double, direction: String, isDarkMode: Boolean): Bitmap? {
        if (!isInitialized) return null
        
        val key = generateKey(latitude, longitude, direction, isDarkMode)
        
        memoryCache[key]?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val fileName = generateFileName(key)
                val file = File(cacheDir, fileName)
                
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
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
    
    suspend fun cacheSnapshot(latitude: Double, longitude: Double, direction: String, isDarkMode: Boolean, bitmap: Bitmap) {
        if (!isInitialized) return
        
        val key = generateKey(latitude, longitude, direction, isDarkMode)
        
        addToMemoryCache(key, bitmap)
        withContext(Dispatchers.IO) {
            try {
                val fileName = generateFileName(key)
                val file = File(cacheDir, fileName)
                
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                }
            } catch (e: Exception) {
            }
        }
    }
    
    private fun addToMemoryCache(key: String, bitmap: Bitmap) {
        if (memoryCache.size >= MAX_MEMORY_CACHE_SIZE) {
            val keysToRemove = memoryCache.keys.take(10)
            keysToRemove.forEach { memoryCache.remove(it) }
        }
        
        memoryCache[key] = bitmap
    }
    
    private fun cleanupDiskCache() {
        try {
            val dir = cacheDir ?: return
            val files = dir.listFiles() ?: return
            
            val totalSize = files.sumOf { it.length() }
            val maxSizeBytes = MAX_DISK_CACHE_SIZE_MB * 1024 * 1024
            
            if (totalSize > maxSizeBytes) {
                val sortedFiles = files.sortedBy { it.lastModified() }
                var currentSize = totalSize
                
                for (file in sortedFiles) {
                    if (currentSize <= maxSizeBytes) break
                    currentSize -= file.length()
                    file.delete()
                }
            }
        } catch (e: Exception) {
            println(e.message)
        }
    }
}