package com.aymanhki.peektransit.data.models

import com.google.android.gms.maps.model.LatLng

data class StopInfo(
    val key: Int,
    val name: String,
    val location: LatLng?
) {
    constructor(name: String) : this(-1, name.replace("@", " @ "), null)

    companion object {
        fun fromDict(dict: Map<String, Any?>): StopInfo? {
            return try {
                val key = dict["key"] as Int
                val name = dict["name"] as String
                val centre = dict["centre"] as Map<String, Any>
                val geographic = centre["geographic"] as Map<String, Any>
                val lat = geographic["latitude"] as Double
                val lon = geographic["longitude"] as Double

                StopInfo(
                    key = key,
                    name = name.replace("@", " @ "),
                    location = LatLng(lat, lon)
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
