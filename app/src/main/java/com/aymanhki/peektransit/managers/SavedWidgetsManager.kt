package com.aymanhki.peektransit.managers

import android.content.Context
import android.content.SharedPreferences
import com.aymanhki.peektransit.data.models.WidgetModel
import com.aymanhki.peektransit.data.adapters.WidgetModelTypeAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit
import com.aymanhki.peektransit.utils.PeekTransitConstants

class SavedWidgetsManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val gson = GsonBuilder()
        .registerTypeAdapter(WidgetModel::class.java, WidgetModelTypeAdapter())
        .serializeSpecialFloatingPointValues()
        .create()
    
    private val _savedWidgets = MutableStateFlow<List<WidgetModel>>(emptyList())
    val savedWidgets: StateFlow<List<WidgetModel>> = _savedWidgets.asStateFlow()
    
    init {
        loadSavedWidgets()
    }
    
    fun loadSavedWidgets() {
        val json = sharedPreferences.getString(KEY_SAVED_WIDGETS, null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<WidgetModel>>() {}.type
                val widgets: List<WidgetModel> = gson.fromJson(json, type)
                _savedWidgets.value = widgets
            } catch (e: Exception) {
                _savedWidgets.value = emptyList()
            }
        } else {
            _savedWidgets.value = emptyList()
        }
    }
    
    fun addWidget(widget: WidgetModel) {
        val currentWidgets = _savedWidgets.value.toMutableList()
        currentWidgets.add(widget)
        saveWidgets(currentWidgets)
    }
    
    fun updateWidget(widgetId: String, updatedWidget: WidgetModel) {
        val currentWidgets = _savedWidgets.value.toMutableList()
        val index = currentWidgets.indexOfFirst { it.id == widgetId }
        if (index != -1) {
            currentWidgets[index] = updatedWidget
            saveWidgets(currentWidgets)
        }

       PeekTransitConstants.removeWidgetConfigurationConnectionIfNeeded(context)
    }
    
    fun deleteWidget(widgetId: String) {
        val currentWidgets = _savedWidgets.value.toMutableList()
        currentWidgets.removeAll { it.id == widgetId }
        saveWidgets(currentWidgets)
    }
    
    fun deleteWidgets(widgetIds: Set<String>) {
        val currentWidgets = _savedWidgets.value.toMutableList()
        currentWidgets.removeAll { it.id in widgetIds }
        saveWidgets(currentWidgets)
    }
    
    fun getWidget(widgetId: String): WidgetModel? {
        return _savedWidgets.value.find { it.id == widgetId }
    }
    
    fun isNameUnique(name: String, excludeId: String? = null): Boolean {
        return _savedWidgets.value.none { widget ->
            widget.widgetData["name"] as? String == name && widget.id != excludeId
        }
    }
    
    private fun saveWidgets(widgets: List<WidgetModel>) {
        try {
            val json = gson.toJson(widgets)
            sharedPreferences.edit { putString(KEY_SAVED_WIDGETS, json) }
            _savedWidgets.value = widgets
            PeekTransitConstants.triggerWidgetCoreUpdatesManagerWithUserSettings(context, true, true)
        } catch (e: Exception) {

        }
    }
    
    fun cacheWidgetData(widgetId: String, scheduleData: List<String>, lastUpdatedTime: Long) {
        try {
            val scheduleJson = gson.toJson(scheduleData)
            sharedPreferences.edit {
                putString("widget_cache_schedule_$widgetId", scheduleJson)
                    .putLong("widget_cache_updated_time_$widgetId", lastUpdatedTime)
            }
        } catch (e: Exception) {

        }
    }
    
    fun getCachedWidgetData(widgetId: String): Pair<List<String>?, Long?> {
        return try {
            val scheduleJson = sharedPreferences.getString("widget_cache_schedule_$widgetId", null)
            val lastUpdatedTime = sharedPreferences.getLong("widget_cache_updated_time_$widgetId", 0L)
            
            val scheduleData = if (scheduleJson != null) {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson<List<String>>(scheduleJson, type)
            } else {
                null
            }
            
            Pair(scheduleData, if (lastUpdatedTime > 0) lastUpdatedTime else null)
        } catch (e: Exception) {
            Pair(null, null)
        }
    }
    
    companion object {
        private const val PREFS_NAME = "peek_transit_widgets"
        private const val KEY_SAVED_WIDGETS = "saved_widgets"
        
        @Volatile
        private var INSTANCE: SavedWidgetsManager? = null
        
        fun getInstance(context: Context): SavedWidgetsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SavedWidgetsManager(context).also { INSTANCE = it }
            }
        }
    }
}