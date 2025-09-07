package com.aymanhki.peektransit.data.models

import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.google.gson.annotations.SerializedName
import java.util.*
import java.util.UUID

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
    var selectedVariants: List<Variant> = emptyList()
) {

    fun getDistance(): Double {
         return distances.direct
    }

    fun getEffectiveFromDate(): Date? {
        return if (effectiveFrom.isNullOrBlank()) {
            null
        } else {
            try {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(effectiveFrom)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun getEffectiveToDate(): Date? {
        return if (effectiveTo.isNullOrBlank()) {
            null
        } else {
            try {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(effectiveTo)
            } catch (e: Exception) {
                null
            }
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
    fun getEffectiveFromDate(): Date? {
        return if (effectiveFrom.isNullOrBlank()) {
            null
        } else {
            try {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(effectiveFrom)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun getEffectiveToDate(): Date? {
        return if (effectiveTo.isNullOrBlank()) {
            null
        } else {
            try {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(effectiveTo)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    fun getRouteKey(): String {
        return key.split(PeekTransitConstants.VARIANT_KEY_SEPARATOR).firstOrNull() ?: key
    }
}



data class SavedStop(
    val id: String,
    val stopData: Stop,
    val folderCategories: List<String>? = null
) {
    constructor(stopData: Stop, folderCategories: List<String>? = null) : this(
        id = stopData.number.toString(),
        stopData = stopData,
        folderCategories = folderCategories
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SavedStop
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

data class FolderCategory(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icons: List<String> = listOf(),
    val stopOrder: List<String> = emptyList(),
    val viewMode: SavedStopsViewMode = SavedStopsViewMode.DEFAULT
)

enum class SavedStopsViewMode(val displayName: String, val columns: Int) {
    LIST("List", 1),
    GRID_2("Grid (2)", 2),
    GRID_3("Grid (3)", 3);

    companion object {
        val DEFAULT = LIST

        fun fromString(value: String?): SavedStopsViewMode {
            return entries.find { it.name == value } ?: DEFAULT
        }
    }
}
