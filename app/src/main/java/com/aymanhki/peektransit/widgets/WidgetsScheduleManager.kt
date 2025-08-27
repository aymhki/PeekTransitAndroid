package com.aymanhki.peektransit.widgets

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.*
import java.lang.reflect.Type
import androidx.core.content.edit
import com.aymanhki.peektransit.utils.PeekTransitConstants


data class WidgetSchedule(
    val widgetAppId: String,
    val widgetConfigId: String,
    var userLocationLon: String,
    var userLocationLat: String,
    var lastUpdatedTime: String,
    var scheduleData: Map<String, List<String>>,
    var errorMsg: String,

) {
    fun getWidgetScheduldeKey(): String {
        return "${widgetAppId}${PeekTransitConstants.COMPOSITE_KEY_LINKER_FOR_DICTIONARIES}${widgetConfigId}"
    }

    companion object {
        fun theTwoHaveDifferentSchedules(
            first: WidgetSchedule?,
            second: WidgetSchedule?
        ): Boolean {
            if (first == null && second == null) return false
            if (first == null || second == null) return true

            for ((key, value) in first.scheduleData) {
                if (second.scheduleData[key] != value && value.contains(PeekTransitConstants.MINUTES_REMAINING_TEXT)) {
                    return true
                }
            }

            for ((key, value) in second.scheduleData) {
                if (first.scheduleData[key] != value && value.contains(PeekTransitConstants.MINUTES_REMAINING_TEXT)) {
                    return true
                }
            }

            return true
        }
    }
}




class SavedWidgetSchedulesManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun saveWidgetSchedule(widgetSchedule: WidgetSchedule) {
        val key = KEY_WIDGET_SCHEDULES + widgetSchedule.getWidgetScheduldeKey()
        val gson = GsonBuilder()
            .registerTypeAdapter(WidgetSchedule::class.java, WidgetScheduleTypeAdapter())
            .create()
        val json = gson.toJson(widgetSchedule)

        sharedPreferences.edit { putString(key, json) }
    }

    fun getWidgetSchedule(widgetAppId: String, widgetConfigId: String): WidgetSchedule? {
        val key = KEY_WIDGET_SCHEDULES + "${widgetAppId}${PeekTransitConstants.COMPOSITE_KEY_LINKER_FOR_DICTIONARIES}${widgetConfigId}"
        val json = sharedPreferences.getString(key, null) ?: return null

        val gson = GsonBuilder()
            .registerTypeAdapter(WidgetSchedule::class.java, WidgetScheduleTypeAdapter())
            .create()

        return gson.fromJson(json, WidgetSchedule::class.java)
    }

    fun deleteWidgetSchedule(widgetAppId: String, widgetConfigId: String) {
        val key = KEY_WIDGET_SCHEDULES + "${widgetAppId}${PeekTransitConstants.COMPOSITE_KEY_LINKER_FOR_DICTIONARIES}${widgetConfigId}"
        sharedPreferences.edit { remove(key) }
    }

    companion object {
        private const val PREFS_NAME = "peek_transit_widget_schedules"
        private const val KEY_WIDGET_SCHEDULES = "widget_schedules_"

        @Volatile
        private var instance: SavedWidgetSchedulesManager? = null

        fun getInstance(context: Context): SavedWidgetSchedulesManager {
            return instance ?: synchronized(this) {
                instance ?: SavedWidgetSchedulesManager(context).also { instance = it }
            }
        }
    }
}

class WidgetScheduleTypeAdapter : JsonDeserializer<WidgetSchedule>, JsonSerializer<WidgetSchedule> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): WidgetSchedule {
        val jsonObject = json.asJsonObject

        val widgetAppId = jsonObject.get("widgetAppId").asString
        val widgetConfigId = jsonObject.get("widgetConfigId").asString
        val userLocationLon = jsonObject.get("userLocationLon").asString
        val userLocationLat = jsonObject.get("userLocationLat").asString
        val lastUpdatedTime = jsonObject.get("lastUpdatedTime").asString
        val errorMsg = jsonObject.get("errorMsg")?.asString ?: ""

        val scheduleData = mutableMapOf<String, List<String>>()
        val scheduleDataJson = jsonObject.getAsJsonObject("scheduleData")

        for ((key, value) in scheduleDataJson.entrySet()) {
            scheduleData[key] = context.deserialize(value, List::class.java) as List<String>
        }

        return WidgetSchedule(
            widgetAppId,
            widgetConfigId,
            userLocationLon,
            userLocationLat,
            lastUpdatedTime,
            scheduleData,
            errorMsg
        )
    }

    override fun serialize(src: WidgetSchedule, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()

        jsonObject.addProperty("widgetAppId", src.widgetAppId)
        jsonObject.addProperty("widgetConfigId", src.widgetConfigId)
        jsonObject.addProperty("userLocationLon", src.userLocationLon)
        jsonObject.addProperty("userLocationLat", src.userLocationLat)
        jsonObject.addProperty("lastUpdatedTime", src.lastUpdatedTime)
        jsonObject.addProperty("errorMsg", src.errorMsg)

        val scheduleDataJson = JsonObject()
        for ((key, value) in src.scheduleData) {
            scheduleDataJson.add(key, context.serialize(value))
        }

        jsonObject.add("scheduleData", scheduleDataJson)

        return jsonObject
    }
}



