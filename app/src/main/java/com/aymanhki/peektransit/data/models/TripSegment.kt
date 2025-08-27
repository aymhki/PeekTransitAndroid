package com.aymanhki.peektransit.data.models

import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.*
import com.aymanhki.peektransit.utils.SegmentType

class TripSegment(
    val type: SegmentType,
    val startTime: Date,
    val endTime: Date,
    val startTimeStr: String,
    val endTimeStr: String,
    val duration: Int,
    val routeKey: Int?,
    val routeNumber: String?,
    val routeName: String?,
    val variantKey: String?,
    val variantName: String?,
    val fromStop: StopInfo?,
    val toStop: StopInfo?
) {
    companion object {
        private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

        fun fromDict(
            dict: Map<String, Any?>,
            parsedSegments: List<TripSegment>,
            currentSegmentIndex: Int,
            segmentsArray: List<Map<String, Any?>>
        ): TripSegment? {
            return try {
                val typeString = dict["type"] as String
                val type = SegmentType.values().find { it.value == typeString }
                    ?: throw Exception("Invalid segment type")

                val times = dict["times"] as Map<String, Any>
                val startTimeStr = times["start"] as String
                val endTimeStr = times["end"] as String
                val durations = times["durations"] as Map<String, Any>
                val totalDuration = durations["total"] as Int

                val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

                val startTime = dateFormatter.parse(startTimeStr) ?: Date()
                val endTime = dateFormatter.parse(endTimeStr) ?: Date()

                val formattedStartTime = timeFormatter.format(startTime)
                val formattedEndTime = timeFormatter.format(endTime)

                var routeKey: Int? = null
                var routeNumber: String? = null
                var routeName: String? = null
                var variantKey: String? = null
                var variantName: String? = null

                if (type == SegmentType.RIDE) {
                    val routeDict = dict["route"] as? Map<String, Any>
                    if (routeDict != null) {
                        routeKey = routeDict["key"] as? Int

                        routeNumber = when (val num = routeDict["number"]) {
                            is String -> num
                            is Int -> num.toString()
                            else -> null
                        }

                        routeName = routeDict["name"] as? String
                    }

                    val variantDict = dict["variant"] as? Map<String, Any>
                    if (variantDict != null) {
                        variantKey = variantDict["key"] as? String
                        variantName = variantDict["name"] as? String
                    }
                }

                var fromStopInfo: StopInfo? = null
                val from = dict["from"] as? Map<String, Any>
                if (from != null) {
                    val stopDict = from["stop"] as? Map<String, Any>
                    if (stopDict != null) {
                        fromStopInfo = StopInfo.fromDict(stopDict)
                    } else {
                        val origin = from["origin"] as? Map<String, Any>
                        if (origin != null) {
                            fromStopInfo = parseAddress(origin, "Current Location")
                        } else {
                            fromStopInfo = lookupPreviousStop(segmentsArray, currentSegmentIndex)
                        }
                    }
                } else {
                    fromStopInfo = lookupPreviousStop(segmentsArray, currentSegmentIndex)
                }

                var toStopInfo: StopInfo? = null
                val to = dict["to"] as? Map<String, Any>
                if (to != null) {
                    val stopDict = to["stop"] as? Map<String, Any>
                    if (stopDict != null) {
                        toStopInfo = StopInfo.fromDict(stopDict)
                    } else {
                        val destination = to["destination"] as? Map<String, Any>
                        if (destination != null) {
                            toStopInfo = parseAddress(destination, "Destination")
                        } else {
                            toStopInfo = lookupNextStop(segmentsArray, currentSegmentIndex)
                        }
                    }
                } else {
                    toStopInfo = lookupNextStop(segmentsArray, currentSegmentIndex)
                }

                TripSegment(
                    type = type,
                    startTime = startTime,
                    endTime = endTime,
                    startTimeStr = formattedStartTime,
                    endTimeStr = formattedEndTime,
                    duration = totalDuration,
                    routeKey = routeKey,
                    routeNumber = routeNumber,
                    routeName = routeName,
                    variantKey = variantKey,
                    variantName = variantName,
                    fromStop = fromStopInfo,
                    toStop = toStopInfo
                )
            } catch (e: Exception) {
                null
            }
        }

        private fun parseAddress(destination: Map<String, Any>, type: String): StopInfo {
            (destination["monument"] as? Map<String, Any>)?.let { monument ->
                (monument["address"] as? Map<String, Any>)?.let { address ->
                    (address["street"] as? Map<String, Any>)?.let { street ->
                        val streetName = street["name"] as? String
                        val streetNumber = address["street-number"] as? Int
                        if (streetName != null && streetNumber != null) {
                            return StopInfo(name = "$streetNumber $streetName")
                        }
                    }
                }
            }

            (destination["address"] as? Map<String, Any>)?.let { address ->
                (address["street"] as? Map<String, Any>)?.let { street ->
                    val streetName = street["name"] as? String
                    val streetNumber = address["street-number"] as? Int
                    if (streetName != null && streetNumber != null) {
                        return StopInfo(name = "$streetNumber $streetName")
                    }
                }

                (address["address"] as? Map<String, Any>)?.let { addressSub ->
                    (addressSub["street"] as? Map<String, Any>)?.let { street ->
                        val streetName = street["name"] as? String
                        val streetNumber = address["street-number"] as? Int
                        if (streetName != null && streetNumber != null) {
                            return StopInfo(name = "$streetNumber $streetName")
                        }
                    }
                }
            }

            (destination["intersection"] as? Map<String, Any>)?.let { intersection ->
                (intersection["street"] as? Map<String, Any>)?.let { street ->
                    val streetName = street["name"] as? String
                    if (streetName != null) {
                        return StopInfo(name = "Closest intersection to destination at $streetName")
                    }
                }
            }

            return StopInfo(name = type)
        }

        private fun lookupPreviousStop(segments: List<Map<String, Any?>>, currentIndex: Int): StopInfo? {
            for (index in (currentIndex - 1) downTo 0) {
                val segment = segments[index]
                (segment["to"] as? Map<String, Any>)?.let { to ->
                    (to["stop"] as? Map<String, Any>)?.let { stopDict ->
                        StopInfo.fromDict(stopDict)?.let { stopInfo ->
                            if (stopInfo.key != -1 && stopInfo.location != null) {
                                return stopInfo
                            }
                        }
                    }
                }
            }
            return StopInfo(name = "Location")
        }

        private fun lookupNextStop(segments: List<Map<String, Any?>>, currentIndex: Int): StopInfo? {
            for (index in (currentIndex + 1) until segments.size) {
                val segment = segments[index]
                (segment["from"] as? Map<String, Any>)?.let { from ->
                    (from["stop"] as? Map<String, Any>)?.let { stopDict ->
                        StopInfo.fromDict(stopDict)?.let { stopInfo ->
                            if (stopInfo.key != -1 && stopInfo.location != null) {
                                return stopInfo
                            }
                        }
                    }
                }
            }
            return StopInfo(name = "Destination")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TripSegment) return false

        return type == other.type &&
                startTime == other.startTime &&
                endTime == other.endTime &&
                duration == other.duration &&
                routeKey == other.routeKey &&
                routeNumber == other.routeNumber &&
                variantKey == other.variantKey &&
                fromStop == other.fromStop &&
                toStop == other.toStop
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + startTime.hashCode()
        result = 31 * result + endTime.hashCode()
        result = 31 * result + duration
        result = 31 * result + (routeKey ?: 0)
        result = 31 * result + (routeNumber?.hashCode() ?: 0)
        result = 31 * result + (variantKey?.hashCode() ?: 0)
        result = 31 * result + (fromStop?.hashCode() ?: 0)
        result = 31 * result + (toStop?.hashCode() ?: 0)
        return result
    }
}
