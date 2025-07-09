package com.aymanhki.peektransit.data.adapters

import com.aymanhki.peektransit.data.models.Stop
import com.aymanhki.peektransit.data.models.Variant
import com.aymanhki.peektransit.data.models.WidgetModel
import com.google.gson.*
import java.lang.reflect.Type

class WidgetModelTypeAdapter : JsonDeserializer<WidgetModel>, JsonSerializer<WidgetModel> {
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): WidgetModel {
        val jsonObject = json.asJsonObject
        
        val id = jsonObject.get("id")?.asString ?: ""
        val widgetDataElement = jsonObject.get("widgetData")
        
        val widgetData = if (widgetDataElement != null && widgetDataElement.isJsonObject) {
            parseWidgetData(widgetDataElement.asJsonObject)
        } else {
            emptyMap()
        }
        
        return WidgetModel(id = id, widgetData = widgetData)
    }
    
    private fun parseWidgetData(jsonObject: JsonObject): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        
        for ((key, value) in jsonObject.entrySet()) {
            result[key] = parseValue(value)
        }
        
        return result
    }
    
    private fun parseValue(element: JsonElement): Any {
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
                    list.add(parseValue(item))
                }
                
                // Check if this is an array of stops or variants
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
                    map[key] = parseValue(value)
                }
                
                // Check if this is a stop or variant object
                when {
                    isStopObject(map) -> parseStopFromMap(map)
                    isVariantObject(map) -> parseVariantFromMap(map)
                    // Check if this is a selectedVariants map (Map<String, List<Variant>>)
                    isSelectedVariantsMap(map) -> {
                        val result = mutableMapOf<String, List<Variant>>()
                        for ((k, v) in map) {
                            if (v is List<*>) {
                                val variants = v.mapNotNull { item ->
                                    if (item is Map<*, *> && isVariantObject(item)) {
                                        parseVariantFromMap(item as Map<String, Any>)
                                    } else null
                                }
                                result[k] = variants
                            }
                        }
                        result
                    }
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
    
    private fun isSelectedVariantsMap(map: Map<*, *>): Boolean {
        // Check if all values are lists and at least one contains variant objects
        return map.values.all { it is List<*> } && 
               map.values.any { value ->
                   val list = value as List<*>
                   list.isNotEmpty() && list[0] is Map<*, *> && isVariantObject(list[0] as Map<*, *>)
               }
    }
    
    private fun parseStopFromMap(map: Map<String, Any>): Stop {
        return try {
            val gson = Gson()
            val json = gson.toJson(map)
            gson.fromJson(json, Stop::class.java)
        } catch (e: Exception) {
            Stop()
        }
    }
    
    private fun parseVariantFromMap(map: Map<String, Any>): Variant {
        return try {
            val gson = Gson()
            val json = gson.toJson(map)
            gson.fromJson(json, Variant::class.java)
        } catch (e: Exception) {
            Variant()
        }
    }
    
    override fun serialize(src: WidgetModel, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("id", src.id)
        jsonObject.add("widgetData", serializeWidgetData(src.widgetData, context))
        return jsonObject
    }
    
    private fun serializeWidgetData(widgetData: Map<String, Any>, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        
        for ((key, value) in widgetData) {
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