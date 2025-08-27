package com.aymanhki.peektransit.data.models

import com.aymanhki.peektransit.utils.SegmentType
import java.text.SimpleDateFormat
import java.util.*

class TripPlan(
    val planNumber: Int,
    val startTime: Date,
    val endTime: Date,
    val startTimeString: String,
    val endTimeString: String,
    val duration: Int,
    val walkingDuration: Int,
    val waitingDuration: Int,
    val ridingDuration: Int,
    val segments: List<TripSegment>,
    val tripPlanDict: Map<String, Any>
) {
    private object RouteWeights {
        const val durationWeight: Double = 0.45
        const val transfersWeight: Double = 0.25
        const val walkingWeight: Double = 0.20
        const val waitingWeight: Double = 0.10
        const val longWalkingPenalty: Double = 1.5
        const val manyTransfersPenalty: Double = 1.4
        const val longWalkingThreshold = 12
        const val highTransferThreshold = 2
    }

    companion object {
        private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

        fun fromDict(planDict: Map<String, Any?>): TripPlan? {
            return try {
                val planNumber = planDict["number"] as Int
                val times = planDict["times"] as Map<String, Any>
                val startTimeStr = times["start"] as String
                val endTimeStr = times["end"] as String
                val durations = times["durations"] as Map<String, Any>
                val totalDuration = durations["total"] as Int
                val walkingDuration = durations["walking"] as Int
                val waitingDuration = durations["waiting"] as Int
                val ridingDuration = durations["riding"] as Int
                val segmentsArray = planDict["segments"] as List<Map<String, Any?>>

                val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

                val startTime = dateFormatter.parse(startTimeStr) ?: Date()
                val endTime = dateFormatter.parse(endTimeStr) ?: Date()

                val startTimeFormatted = timeFormatter.format(startTime)
                val endTimeFormatted = timeFormatter.format(endTime)

                val parsedSegments = mutableListOf<TripSegment>()

                segmentsArray.forEachIndexed { index, segmentDict ->
                    TripSegment.fromDict(
                        segmentDict,
                        parsedSegments,
                        index,
                        segmentsArray
                    )?.let {
                        parsedSegments.add(it)
                    }
                }

                TripPlan(
                    planNumber = planNumber,
                    startTime = startTime,
                    endTime = endTime,
                    startTimeString = startTimeFormatted,
                    endTimeString = endTimeFormatted,
                    duration = totalDuration,
                    walkingDuration = walkingDuration,
                    waitingDuration = waitingDuration,
                    ridingDuration = ridingDuration,
                    segments = parsedSegments,
                    tripPlanDict = planDict as Map<String, Any>
                )
            } catch (e: Exception) {
                null
            }
        }

        fun calculateRouteScore(route: TripPlan): Double {
            val normalizedDuration = route.duration / 90.0
            val transferCount = route.segments.size - 1
            val normalizedTransfers = maxOf(0, transferCount) / 3.0
            val normalizedWalking = route.walkingDuration / 20.0
            val normalizedWaiting = route.waitingDuration / 15.0

            var score = 0.0

            score += normalizedDuration * RouteWeights.durationWeight
            score += normalizedTransfers * RouteWeights.transfersWeight
            score += normalizedWalking * RouteWeights.walkingWeight
            score += normalizedWaiting * RouteWeights.waitingWeight

            if (route.walkingDuration > RouteWeights.longWalkingThreshold) {
                val excessWalking = (route.walkingDuration - RouteWeights.longWalkingThreshold) / 10.0
                score *= (1.0 + (excessWalking * (RouteWeights.longWalkingPenalty - 1.0)))
            }

            if (transferCount > RouteWeights.highTransferThreshold) {
                val excessTransfers = (transferCount - RouteWeights.highTransferThreshold).toDouble()
                score *= (1.0 + (excessTransfers * (RouteWeights.manyTransfersPenalty - 1.0) / 2.0))
            }

            return score
        }

        fun getTopRecommendedRoutes(availableRoutes: List<TripPlan>, limit: Int = 5): List<TripPlan> {
            if (availableRoutes.isEmpty()) return emptyList()

            val walkingGroups = mutableMapOf<Boolean, MutableList<TripPlan>>()
            walkingGroups[true] = mutableListOf()
            walkingGroups[false] = mutableListOf()

            for (route in availableRoutes) {
                val isFirstSegmentWalking = route.segments.firstOrNull()?.type == SegmentType.WALK
                walkingGroups[isFirstSegmentWalking]?.add(route)
            }

            walkingGroups[true]?.sortWith { route1, route2 ->
                val walkSegment1 = route1.segments.firstOrNull()
                val walkSegment2 = route2.segments.firstOrNull()
                if (walkSegment1 != null && walkSegment2 != null) {
                    walkSegment1.duration.compareTo(walkSegment2.duration)
                } else {
                    0
                }
            }

            val finalSortedWalkingGroups = mutableMapOf<Boolean, List<TripPlan>>()
            finalSortedWalkingGroups[true] = mutableListOf()
            finalSortedWalkingGroups[false] = mutableListOf()

            for ((isWalking, routes) in walkingGroups) {
                val segmentCountGroups = routes.groupBy { it.segments.size }
                    .toSortedMap()

                val sortedBySegmentCount = mutableListOf<TripPlan>()

                for ((_, routesWithSameSegmentCount) in segmentCountGroups) {
                    val sortedByStartTime = routesWithSameSegmentCount.sortedBy { it.startTime }
                    val startTimeGroups = mutableListOf<List<TripPlan>>()
                    var currentTimeGroup = mutableListOf<TripPlan>()
                    var previousStartTime: Date? = null
                    val timeThreshold = 1 * 60 * 1000

                    for (route in sortedByStartTime) {
                        val prevTime = previousStartTime
                        if (prevTime != null &&
                            Math.abs(route.startTime.time - prevTime.time) <= timeThreshold) {
                            currentTimeGroup.add(route)
                        } else {
                            if (currentTimeGroup.isNotEmpty()) {
                                startTimeGroups.add(currentTimeGroup)
                            }
                            currentTimeGroup = mutableListOf(route)
                            previousStartTime = route.startTime
                        }
                    }

                    if (currentTimeGroup.isNotEmpty()) {
                        startTimeGroups.add(currentTimeGroup)
                    }

                    val segmentGroupRoutes = mutableListOf<TripPlan>()
                    for (group in startTimeGroups) {
                        val sortedByDuration = group.sortedWith { route1, route2 ->
                            if (Math.abs(route1.duration - route2.duration) <= 60) {
                                calculateRouteScore(route1).compareTo(calculateRouteScore(route2))
                            } else {
                                route1.duration.compareTo(route2.duration)
                            }
                        }
                        segmentGroupRoutes.addAll(sortedByDuration)
                    }

                    sortedBySegmentCount.addAll(segmentGroupRoutes)
                }

                finalSortedWalkingGroups[isWalking] = sortedBySegmentCount
            }

            val finalRoutes = mutableListOf<TripPlan>()
            finalSortedWalkingGroups[false]?.let { finalRoutes.addAll(it) }
            finalSortedWalkingGroups[true]?.let { finalRoutes.addAll(it) }

            val actualLimit = minOf(limit, finalRoutes.size)
            return finalRoutes.take(actualLimit)
        }

        private fun sortWalkingSegment(routes: List<TripPlan>): List<TripPlan> {
            return routes.sortedWith { route1, route2 ->
                val isFirstSegmentWalking1 = route1.segments.firstOrNull()?.type == SegmentType.WALK
                val isFirstSegmentWalking2 = route2.segments.firstOrNull()?.type == SegmentType.WALK

                when {
                    isFirstSegmentWalking1 && isFirstSegmentWalking2 -> {
                        route1.segments.first().duration.compareTo(route2.segments.first().duration)
                    }
                    isFirstSegmentWalking1 && !isFirstSegmentWalking2 -> 1
                    !isFirstSegmentWalking1 && isFirstSegmentWalking2 -> -1
                    else -> calculateRouteScore(route1).compareTo(calculateRouteScore(route2))
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TripPlan) return false

        return startTime == other.startTime &&
                endTime == other.endTime &&
                duration == other.duration &&
                walkingDuration == other.walkingDuration &&
                waitingDuration == other.waitingDuration &&
                ridingDuration == other.ridingDuration &&
                segments.size == other.segments.size &&
                segments == other.segments
    }

    override fun hashCode(): Int {
        var result = startTime.hashCode()
        result = 31 * result + endTime.hashCode()
        result = 31 * result + duration
        result = 31 * result + walkingDuration
        result = 31 * result + waitingDuration
        result = 31 * result + ridingDuration
        result = 31 * result + segments.hashCode()
        return result
    }
}
