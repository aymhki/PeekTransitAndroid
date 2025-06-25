package com.aymanhki.peektransit.data.models

import com.google.gson.annotations.SerializedName
import java.util.*

data class DistanceInfo(
    val direct: Double = Double.POSITIVE_INFINITY,
)

data class Stop(
    val key: Int = -1,
    val name: String = "Unknown Stop",
    val number: Int = -1,
    @SerializedName("effective-from")
    val effectiveFrom: String = "",
    @SerializedName("effective-to")
    val effectiveTo: String = "",
    val direction: String = "Unknown Direction",
    val side: String = "Unknown Side",
    val street: Street = Street(),
    @SerializedName("cross-street")
    val crossStreet: Street = Street(),
    val centre: Centre = Centre(),
    val distances: DistanceInfo = DistanceInfo(),
    var variants: List<Variant> = emptyList(),
    val selectedVariants: List<Variant> = emptyList()
) {

    fun getDistance(): Double {
         return distances.direct
    }

    fun getEffectiveFromDate(): Date {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(effectiveFrom) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }
    
    fun getEffectiveToDate(): Date {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(effectiveTo) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }
}

data class Street(
    val key: Int = -1,
    val name: String = "Unknown Street",
    val type: String = "Unknown Type"
)

data class Centre(
    val utm: UTM = UTM(),
    val geographic: Geographic = Geographic()
)

data class UTM(
    val zone: String = "Unknown Zone",
    val x: Int = 0,
    val y: Int = 0
)

data class Geographic(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class Variant(
    val key: String = "Undefined Key",
    val name: String = "Unknown Name",
    @SerializedName("effective-from")
    val effectiveFrom: String = "",
    @SerializedName("effective-to")
    val effectiveTo: String = "",
    @SerializedName("background-color")
    val backgroundColor: String? = null,
    @SerializedName("border-color")
    val borderColor: String? = null,
    @SerializedName("text-color")
    val textColor: String? = null
) {
    fun getEffectiveFromDate(): Date {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(effectiveFrom) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }
    
    fun getEffectiveToDate(): Date {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(effectiveTo) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }
    
    fun getRouteKey(): String {
        return key.split("-").firstOrNull() ?: key
    }
}

data class Route(
    val key: String,
    val name: String,
    val textColor: String,
    val backgroundColor: String,
    val borderColor: String,
    val variants: List<Variant>? = null
)

