package com.aymanhki.peektransit.data.models

data class SavedStop(
    val id: String,
    val stopData: Stop
) {
    constructor(stopData: Stop) : this(
        id = stopData.number.toString(),
        stopData = stopData
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