package com.aymanhki.peektransit.data.adapters

import com.aymanhki.peektransit.data.models.Stop
import com.aymanhki.peektransit.data.models.Variant
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class WidgetDataTypeAdapter : JsonDeserializer<Map<String, Any>>, JsonSerializer<Map<String, Any>> {
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        
        if (json.isJsonObject) {
            val jsonObject = json.asJsonObject
            
            for ((key, value) in jsonObject.entrySet()) {
                result[key] = deserializeValue(value, context)
            }
        }
        
        return result
    }
    
    private fun deserializeValue(element: JsonElement, context: JsonDeserializationContext): Any {
        return when {
            element.isJsonNull -> ""
            element.isJsonPrimitive -> {
                val primitive = element.asJsonPrimitive
                when {
                    primitive.isBoolean -> primitive.asBoolean
                    primitive.isNumber -> {
                        val number = primitive.asNumber
                        if (number.toString().contains('.')) {
                            number.toDouble()
                        } else {
                            number.toLong()
                        }
                    }
                    primitive.isString -> primitive.asString
                    else -> primitive.asString
                }
            }
            element.isJsonArray -> {
                val array = element.asJsonArray
                val list = mutableListOf<Any>()
                
                for (item in array) {
                    list.add(deserializeValue(item, context))
                }
                
                if (list.isNotEmpty() && list[0] is Map<*, *>) {
                    val firstItem = list[0] as Map<*, *>
                    when {
                        isStopObject(firstItem) -> {
                            return list.mapNotNull { item ->
                                if (item is Map<*, *>) {
                                    parseStopFromMap(item as Map<String, Any>)
                                } else null
                            }
                        }
                        isVariantObject(firstItem) -> {
                            return list.mapNotNull { item ->
                                if (item is Map<*, *>) {
                                    parseVariantFromMap(item as Map<String, Any>)
                                } else null
                            }
                        }
                    }
                }
                
                list
            }
            element.isJsonObject -> {
                val obj = element.asJsonObject
                val map = mutableMapOf<String, Any>()
                
                for ((key, value) in obj.entrySet()) {
                    map[key] = deserializeValue(value, context)
                }
                
                when {
                    isStopObject(map) -> parseStopFromMap(map)
                    isVariantObject(map) -> parseVariantFromMap(map)
                    else -> map
                }
            }
            else -> element.toString()
        }
    }
    
    private fun isStopObject(map: Map<*, *>): Boolean {
        return map.containsKey("number") && map.containsKey("street")
    }
    
    private fun isVariantObject(map: Map<*, *>): Boolean {
        return map.containsKey("key") && map.containsKey("name") && map.containsKey("effective-from")
    }
    
    private fun parseStopFromMap(map: Map<String, Any>): Stop {
        return try {
            val gson = GsonBuilder()
                .serializeSpecialFloatingPointValues()
                .create()
            val json = gson.toJson(map)
            gson.fromJson(json, Stop::class.java)
        } catch (e: Exception) {
            Stop()
        }
    }
    
    private fun parseVariantFromMap(map: Map<String, Any>): Variant {
        return try {
            val gson = GsonBuilder()
                .serializeSpecialFloatingPointValues()
                .create()
            val json = gson.toJson(map)
            gson.fromJson(json, Variant::class.java)
        } catch (e: Exception) {
            Variant()
        }
    }
    
    override fun serialize(src: Map<String, Any>, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        
        for ((key, value) in src) {
            jsonObject.add(key, serializeValue(value, context))
        }
        
        return jsonObject
    }
    
    private fun serializeValue(value: Any, context: JsonSerializationContext): JsonElement {
        return when (value) {
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Stop -> context.serialize(value)
            is Variant -> context.serialize(value)
            is List<*> -> {
                val array = JsonArray()
                for (item in value) {
                    if (item != null) {
                        array.add(serializeValue(item, context))
                    }
                }
                array
            }
            is Map<*, *> -> {
                val obj = JsonObject()
                for ((k, v) in value) {
                    if (k is String && v != null) {
                        obj.add(k, serializeValue(v, context))
                    }
                }
                obj
            }
            else -> context.serialize(value)
        }
    }
}