package com.aymanhki.peektransit.data.network

import android.location.Location
import com.aymanhki.peektransit.data.models.*
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.TimeFormat
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

interface WinnipegTransitApiService {
    @GET("stops.json")
    suspend fun getNearbyStops(
        @Query("lat") latitude: String,
        @Query("lon") longitude: String,
        @Query("distance") distance: String,
        @Query("walking") walking: String = "false",
        @Query("usage") usage: String,
        @Query("api-key") apiKey: String
    ): Response<JsonObject>
    
    @GET
    suspend fun searchStops(
        @Url url: String
    ): Response<JsonObject>
    
    @GET("routes.json")
    suspend fun getRoutesForStop(
        @Query("stop") stopNumber: Int,
        @Query("usage") usage: String,
        @Query("api-key") apiKey: String
    ): Response<JsonObject>
    
    @GET("variants.json")
    suspend fun getVariantsForStops(
        @Query("start") startTime: String,
        @Query("end") endTime: String,
        @Query("stops") stops: String,
        @Query("usage") usage: String,
        @Query("api-key") apiKey: String
    ): Response<JsonObject>
    
    @GET("variants.json")
    suspend fun getVariantsForStop(
        @Query("stop") stopNumber: Int,
        @Query("api-key") apiKey: String,
        @Query("usage") usage: String = "short"
    ): Response<JsonObject>
    
    @GET("stops/{stopNumber}/schedule.json")
    suspend fun getStopSchedule(
        @Path("stopNumber") stopNumber: Int,
        @Query("start") startTime: String,
        @Query("end") endTime: String,
        @Query("usage") usage: String = "short",
        @Query("api-key") apiKey: String
    ): Response<JsonObject>
    
    @GET("stops/{stopNumber}.json")
    suspend fun getStop(
        @Path("stopNumber") stopNumber: Int,
        @Query("usage") usage: String = "long",
        @Query("api-key") apiKey: String
    ): Response<JsonObject>

    @GET("locations.json")
    suspend fun getLocationKey(
        @Query("lat") latitude: String,
        @Query("lon") longitude: String,
        @Query("usage") usage: String,
        @Query("api-key") apiKey: String
    ): Response<JsonObject>

    @GET("trip-planner.json")
    suspend fun findTrip(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("max-transfers") maxTransfers: Int? = null,
        @Query("walk-speed") walkSpeed: Double? = null,
        @Query("max-walk-time") maxWalkTime: Int? = null,
        @Query("min-transfer-wait") minTransferWait: Int? = null,
        @Query("max-transfer-wait") maxTransferWait: Int? = null,
        @Query("mode") mode: String? = null,
        @Query("date") date: String? = null,
        @Query("time") time: String? = null,
        @Query("usage") usage: String,
        @Query("api-key") apiKey: String
    ): Response<JsonObject>
}

class WinnipegTransitAPI private constructor() {
    private val rateLimiter = RequestRateLimiter.getInstance()
    private val gson = Gson()
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }
    
    private val errorHandlingInterceptor = Interceptor { chain ->
        try {
            chain.proceed(chain.request())
        } catch (e: Exception) {
            when {
                e.message?.contains("Unable to resolve host") == true -> {
                    throw IOException("Network error: Unable to connect to Winnipeg Transit API. Please check your internet connection.")
                }
                e.message?.contains("timeout") == true -> {
                    throw IOException("Network timeout: The request took too long to complete.")
                }
                else -> throw e
            }
        }
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(errorHandlingInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(PeekTransitConstants.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val apiService = retrofit.create(WinnipegTransitApiService::class.java)
    
    suspend fun getNearbyStops(userLocation: Location, forShort: Boolean): List<Stop> = withContext(Dispatchers.IO) {
        rateLimiter.waitIfNeeded()
        
        try {
            val response = apiService.getNearbyStops(
                latitude = userLocation.latitude.toString(),
                longitude = userLocation.longitude.toString(),
                distance = PeekTransitConstants.STOPS_DISTANCE_RADIUS_IN_METERS.toInt().toString(),
                usage = if (forShort) "short" else "long",
                apiKey = PeekTransitConstants.TRANSIT_API_KEY
            )
            
            if (!response.isSuccessful) {
                throw TransitError.NetworkError(IOException("HTTP ${response.code()}"))
            }
            
            val jsonResponse = response.body() ?: throw TransitError.InvalidData
            val stopsArray = jsonResponse.getAsJsonArray("stops") ?: throw TransitError.ParseError("No stops array found")
            
            val stops = mutableListOf<Stop>()
            for (element in stopsArray) {
                try {
                    val stop = gson.fromJson(element, Stop::class.java)
                    val processedStop = if (forShort) {
                        stop.copy(name = stop.name.replace("@", " @ "))
                    } else {
                        stop
                    }
                    stops.add(processedStop)
                } catch (e: Exception) {
                    continue
                }
            }
            
            val currentDate = Date()
            val filteredStops = stops.filter { stop ->
                val effectiveFrom = stop.getEffectiveFromDate()
                val effectiveTo = stop.getEffectiveToDate()

                (effectiveFrom == null || currentDate >= effectiveFrom) &&
                        (effectiveTo == null || currentDate <= effectiveTo)
            }.sortedBy { it.getDistance() }
                .take(PeekTransitConstants.MAX_STOPS_ALLOWED_TO_FETCH)
            
            filteredStops
        } catch (e: Exception) {
            when (e) {
                is TransitError -> throw e
                else -> throw TransitError.NetworkError(e)
            }
        }
    }
    
    suspend fun searchStops(query: String, forShort: Boolean): List<Stop> = withContext(Dispatchers.IO) {
        rateLimiter.waitIfNeeded()
        
        try {
            val encodedQuery = java.net.URLEncoder.encode(query.trim(), "UTF-8")
            val usage = if (forShort) "short" else "long"
            val fullUrl = "${PeekTransitConstants.BASE_URL}stops:${encodedQuery}.json?usage=${usage}&api-key=${PeekTransitConstants.TRANSIT_API_KEY}"
            
            val response = apiService.searchStops(fullUrl)
            
            if (!response.isSuccessful) {
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                throw TransitError.NetworkError(IOException(errorMsg))
            }
            
            val jsonResponse = response.body() ?: throw TransitError.InvalidData
            val stopsArray = jsonResponse.getAsJsonArray("stops") ?: throw TransitError.ParseError("No stops array found")
            
            val stops = mutableListOf<Stop>()
            for (element in stopsArray) {
                try {
                    val stop = gson.fromJson(element, Stop::class.java)
                    val processedStop = if (forShort) {
                        stop.copy(name = stop.name.replace("@", " @ "))
                    } else {
                        stop
                    }
                    stops.add(processedStop)
                } catch (e: Exception) {
                    continue
                }
            }
            
            stops.take(PeekTransitConstants.MAX_STOPS_ALLOWED_TO_FETCH_FOR_SEARCH)
        } catch (e: Exception) {
            when (e) {
                is TransitError -> throw e
                else -> throw TransitError.NetworkError(e)
            }
        }
    }
    
    suspend fun getStopSchedule(stopNumber: Int): JsonObject = withContext(Dispatchers.IO) {
        rateLimiter.waitIfNeeded()
        
        try {
            val currentDate = Date()
            val calendar = Calendar.getInstance()
            calendar.time = currentDate
            calendar.add(Calendar.MINUTE, -5)
            val startDate = calendar.time
            
            calendar.time = currentDate
            calendar.add(Calendar.HOUR, PeekTransitConstants.TIME_PERIOD_ALLOWED_FOR_NEXT_BUS_ROUTES_IN_HOURS)
            val endDate = calendar.time
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val startTime = dateFormat.format(startDate)
            val endTime = dateFormat.format(endDate)
            
            val response = apiService.getStopSchedule(
                stopNumber = stopNumber,
                startTime = startTime,
                endTime = endTime,
                apiKey = PeekTransitConstants.TRANSIT_API_KEY
            )
            
            if (!response.isSuccessful) {
                throw TransitError.NetworkError(IOException("HTTP ${response.code()}"))
            }
            
            response.body() ?: throw TransitError.InvalidData
        } catch (e: Exception) {
            when (e) {
                is TransitError -> throw e
                else -> throw TransitError.NetworkError(e)
            }
        }
    }

    fun cleanStopSchedule(schedule: JsonObject, timeFormat: TimeFormat): List<String> {
        val busScheduleList = mutableListOf<String>()
        val currentDate = Date()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

        try {
            val stopSchedule = schedule.getAsJsonObject("stop-schedule")
            val routeSchedules = stopSchedule.getAsJsonArray("route-schedules")

            for (routeScheduleElement in routeSchedules) {
                val routeSchedule = routeScheduleElement.asJsonObject
                val scheduledStops = routeSchedule.getAsJsonArray("scheduled-stops")

                for (stopElement in scheduledStops) {
                    val stop = stopElement.asJsonObject
                    val variant = stop.getAsJsonObject("variant")
                    var variantKey = variant.get("key")?.asString ?: continue
                    val variantName = variant.get("name")?.asString ?: continue
                    val cancelled = stop.get("cancelled")?.asString
                    val times = stop.getAsJsonObject("times")
                    val arrival = times.getAsJsonObject("departure")

                    val estimatedTime = arrival.get("estimated")?.asString
                    val scheduledTime = arrival.get("scheduled")?.asString
                    var finalArrivalText = ""
                    var arrivalState = PeekTransitConstants.OK_STATUS_TEXT

                    if (estimatedTime != null && scheduledTime != null) {
                        try {
                            val estimatedTimeParsedDateAndTime = estimatedTime.split("T")
                            val scheduledTimeParsedDateAndTime = scheduledTime.split("T")
                            val estimatedTimeParsedDate = estimatedTimeParsedDateAndTime[0].split("-")
                            val estimatedTimeParsedTime = estimatedTimeParsedDateAndTime[1].split(":")
                            val scheduledTimeParsedDate = scheduledTimeParsedDateAndTime[0].split("-")
                            val scheduledTimeParsedTime = scheduledTimeParsedDateAndTime[1].split(":")

                            val estimatedTotalMinutes = (estimatedTimeParsedDate[0].toInt() * 525600) +
                                    (estimatedTimeParsedDate[1].toInt() * 43800) +
                                    (estimatedTimeParsedDate[2].toInt() * 1440) +
                                    (estimatedTimeParsedTime[0].toInt() * 60) +
                                    estimatedTimeParsedTime[1].toInt()

                            val scheduledTotalMinutes = (scheduledTimeParsedDate[0].toInt() * 525600) +
                                    (scheduledTimeParsedDate[1].toInt() * 43800) +
                                    (scheduledTimeParsedDate[2].toInt() * 1440) +
                                    (scheduledTimeParsedTime[0].toInt() * 60) +
                                    scheduledTimeParsedTime[1].toInt()

                            val calendar = Calendar.getInstance()
                            calendar.time = currentDate
                            val currentTotalMinutes = (calendar.get(Calendar.YEAR) * 525600) +
                                    (calendar.get(Calendar.MONTH) * 43800) +
                                    (calendar.get(Calendar.DAY_OF_MONTH) * 1440) +
                                    (calendar.get(Calendar.HOUR_OF_DAY) * 60) +
                                    calendar.get(Calendar.MINUTE)

                            val estimatedDate = dateFormatter.parse(estimatedTime) ?: continue
                            val scheduledDate = dateFormatter.parse(scheduledTime) ?: continue

                            val timeDifferenceSeconds = estimatedDate.time - currentDate.time
                            val timeDifference = kotlin.math.ceil(timeDifferenceSeconds / 60000.0).toInt()
                            val delay = kotlin.math.ceil((estimatedDate.time - scheduledDate.time) / 60000.0).toInt()

                            if (timeDifference < -PeekTransitConstants.MINUTES_ALLOWED_TO_KEEP_DUE_BUSES_IN_SCHEDULE) {
                                continue
                            }

                            if (cancelled == "true") {
                                arrivalState = PeekTransitConstants.CANCELLED_STATUS_TEXT
                                finalArrivalText = ""
                            } else {
                                if (timeDifference < 0 && timeFormat != TimeFormat.CLOCK_TIME) {
                                    finalArrivalText = "${-timeDifference} ${PeekTransitConstants.MINUTES_PASSED_TEXT}"
                                } else if (timeDifference <= PeekTransitConstants.PERIOD_BEFORE_SHOWING_MINUTES_UNTIL_NEXT_BUS_IN_MINUTES && timeFormat != TimeFormat.CLOCK_TIME) {
                                    finalArrivalText = "$timeDifference ${PeekTransitConstants.MINUTES_REMAINING_TEXT}"
                                } else {
                                    var finalHour = estimatedTimeParsedTime[0].toInt()
                                    val am = finalHour < 12

                                    when {
                                        finalHour == 0 -> finalHour = 12
                                        finalHour > 12 -> finalHour -= 12
                                    }

                                    finalArrivalText = "$finalHour:${estimatedTimeParsedTime[1]}"
                                    if (am) {
                                        finalArrivalText += " ${PeekTransitConstants.GLOBAL_AM_TEXT}"
                                    } else {
                                        finalArrivalText += " ${PeekTransitConstants.GLOBAL_PM_TEXT}"
                                    }
                                }

                                when {
                                    delay > 0 && timeDifference <= PeekTransitConstants.PERIOD_BEFORE_SHOWING_MINUTES_UNTIL_NEXT_BUS_IN_MINUTES && timeFormat != TimeFormat.CLOCK_TIME -> {
                                        arrivalState = PeekTransitConstants.LATE_STATUS_TEXT
                                        finalArrivalText = "$timeDifference ${PeekTransitConstants.MINUTES_REMAINING_TEXT}"
                                    }
                                    delay < 0 && timeDifference <= PeekTransitConstants.PERIOD_BEFORE_SHOWING_MINUTES_UNTIL_NEXT_BUS_IN_MINUTES -> {
                                        arrivalState = PeekTransitConstants.EARLY_STATUS_TEXT
                                        finalArrivalText = "$timeDifference ${PeekTransitConstants.MINUTES_REMAINING_TEXT}"
                                    }
                                    else -> {
                                        arrivalState = PeekTransitConstants.OK_STATUS_TEXT
                                    }
                                }

                                if (timeDifference <= 0 && timeDifference >= -PeekTransitConstants.MINUTES_ALLOWED_TO_KEEP_DUE_BUSES_IN_SCHEDULE) {
                                    finalArrivalText = PeekTransitConstants.DUE_STATUS_TEXT
                                }
                            }
                        } catch (e: Exception) {
                            continue
                        }
                    } else {
                        finalArrivalText = "Time Unavailable"
                    }

                    variantKey = variantKey.split("-").firstOrNull() ?: variantKey

                    if (variantKey.contains("BLUE")) {
                        variantKey = "B"
                    }

                    busScheduleList.add(
                        "$variantKey${PeekTransitConstants.SCHEDULE_STRING_SEPARATOR}$variantName${PeekTransitConstants.SCHEDULE_STRING_SEPARATOR}$arrivalState${PeekTransitConstants.SCHEDULE_STRING_SEPARATOR}$finalArrivalText"
                    )
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }

        return busScheduleList.sortedWith { str1, str2 ->
            val componentsA = str1.split(PeekTransitConstants.SCHEDULE_STRING_SEPARATOR)
            val componentsB = str2.split(PeekTransitConstants.SCHEDULE_STRING_SEPARATOR)

            val timeA = componentsA[3]
            val timeB = componentsB[3]

            when {
                timeA == PeekTransitConstants.DUE_STATUS_TEXT && timeB != PeekTransitConstants.DUE_STATUS_TEXT -> -1
                timeB == PeekTransitConstants.DUE_STATUS_TEXT && timeA != PeekTransitConstants.DUE_STATUS_TEXT -> 1
                timeA == PeekTransitConstants.DUE_STATUS_TEXT && timeB == PeekTransitConstants.DUE_STATUS_TEXT -> 0
                else -> {
                    val isMinutesA = timeA.endsWith(PeekTransitConstants.MINUTES_REMAINING_TEXT)
                    val isMinutesB = timeB.endsWith(PeekTransitConstants.MINUTES_REMAINING_TEXT)

                    when {
                        isMinutesA && isMinutesB -> {
                            val minutesA = timeA.split(" ")[0].toIntOrNull() ?: 0
                            val minutesB = timeB.split(" ")[0].toIntOrNull() ?: 0

                            if (minutesA != minutesB) {
                                minutesA.compareTo(minutesB)
                            } else {
                                val stateA = componentsA[2]
                                val stateB = componentsB[2]

                                compareByStatus(stateA, stateB)
                            }
                        }
                        isMinutesA -> -1
                        isMinutesB -> 1
                        else -> {
                            val timeComponentsA = timeA.split(" ")
                            val timeComponentsB = timeB.split(" ")

                            if (timeComponentsA.size == 2 && timeComponentsB.size == 2) {
                                compareClockTimes(timeA, timeB, currentDate)
                            } else {
                                0
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun compareByStatus(statusA: String, statusB: String): Int {
        return when {
            statusA == statusB -> 0
            statusA == PeekTransitConstants.OK_STATUS_TEXT -> -1
            statusB == PeekTransitConstants.OK_STATUS_TEXT -> 1
            statusA == PeekTransitConstants.EARLY_STATUS_TEXT -> -1
            statusB == PeekTransitConstants.EARLY_STATUS_TEXT -> 1
            statusA == PeekTransitConstants.LATE_STATUS_TEXT -> -1
            statusB == PeekTransitConstants.LATE_STATUS_TEXT -> 1
            else -> 0
        }
    }
    
    private fun compareClockTimes(timeA: String, timeB: String, currentDate: Date): Int {
        try {
            val calendar = Calendar.getInstance()
            calendar.time = currentDate
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            
            val timeFormatA = parseClockTime(timeA)
            val timeFormatB = parseClockTime(timeB)
            
            if (timeFormatA == null || timeFormatB == null) return 0
            
            var totalMinutesA = timeFormatA.first * 60 + timeFormatA.second
            var totalMinutesB = timeFormatB.first * 60 + timeFormatB.second
            
            val isAMA = timeA.lowercase().contains("am")
            val isAMB = timeB.lowercase().contains("am")
            
            if (currentHour >= 12) {
                if (isAMA) {
                    totalMinutesA += 24 * 60
                }
                if (isAMB) {
                    totalMinutesB += 24 * 60
                }
            }
            
            return totalMinutesA.compareTo(totalMinutesB)
        } catch (e: Exception) {
            return 0
        }
    }
    
    private fun parseClockTime(time: String): Pair<Int, Int>? {
        try {
            val cleanTime = time.replace(Regex("[ap]m", RegexOption.IGNORE_CASE), "").trim()
            val parts = cleanTime.split(":")
            if (parts.size != 2) return null
            
            val hour = parts[0].toIntOrNull() ?: return null
            val minute = parts[1].toIntOrNull() ?: return null
            
            return Pair(hour, minute)
        } catch (e: Exception) {
            return null
        }
    }
    
    suspend fun getStop(stopNumber: Int): Stop? = withContext(Dispatchers.IO) {
        rateLimiter.waitIfNeeded()
        
        try {
            val response = apiService.getStop(
                stopNumber = stopNumber,
                usage = "long",
                apiKey = PeekTransitConstants.TRANSIT_API_KEY
            )
            
            if (!response.isSuccessful) {
                throw TransitError.NetworkError(IOException("HTTP ${response.code()}"))
            }
            
            val jsonResponse = response.body() ?: throw TransitError.InvalidData
            val stopObject = jsonResponse.getAsJsonObject("stop") ?: throw TransitError.ParseError("No stop object found")
            
            val stop = gson.fromJson(stopObject, Stop::class.java)

            stop
        } catch (e: Exception) {
            when (e) {
                is TransitError -> throw e
                else -> throw TransitError.NetworkError(e)
            }
        }
    }
    
    private suspend fun enrichStopWithVariants(stop: Stop): Stop {
        return try {
            val variants = getVariantsForStop(stop.number)
            
            val filteredVariants = variants.filter { variant ->
                val key = variant.key
                !(key.startsWith("S") || key.startsWith("W") || key.startsWith("I"))
            }
            
            stop.copy(variants = filteredVariants)
        } catch (e: Exception) {
            stop
        }
    }
    
    suspend fun getVariantsForStop(stopNumber: Int): List<Variant> = withContext(Dispatchers.IO) {
        rateLimiter.waitIfNeeded()
        
        try {
            val response = apiService.getRoutesForStop(
                stopNumber = stopNumber,
                usage = "short",
                apiKey = PeekTransitConstants.TRANSIT_API_KEY
            )
            
            if (!response.isSuccessful) {
                throw TransitError.NetworkError(IOException("HTTP ${response.code()}"))
            }
            
            val jsonResponse = response.body() ?: throw TransitError.InvalidData
            val routesArray = jsonResponse.getAsJsonArray("routes") ?: return@withContext emptyList()
            
            val variants = mutableListOf<Variant>()
            
            for (routeElement in routesArray) {
                try {
                    val routeObj = routeElement.asJsonObject
                    val routeVariants = routeObj.getAsJsonArray("variants")
                    val badgeStyle = routeObj.getAsJsonObject("badge-style")


                    val routeBackgroundColor = badgeStyle.get("background-color")?.asString
                    val routeBorderColor = badgeStyle.get("border-color")?.asString
                    val routeTextColor = badgeStyle.get("color")?.asString

                    val effectiveFrom = routeObj.get("effective-from")?.asString ?: ""
                    val effectiveTo = routeObj.get("effective-to")?.asString ?: ""
                    
                    if (routeVariants != null) {
                        for (variantElement in routeVariants) {
                            try {
                                val variantObj = variantElement.asJsonObject
                                
                                val variant = Variant(
                                    key = variantObj.get("key")?.asString ?: "Unknown",
                                    name = variantObj.get("name")?.asString ?: "Unknown",
                                    effectiveFrom = effectiveFrom,
                                    effectiveTo = effectiveTo,
                                    backgroundColor = routeBackgroundColor,
                                    borderColor = routeBorderColor,
                                    textColor = routeTextColor
                                )
                                
                                variants.add(variant)
                            } catch (e: Exception) {
                                continue
                            }
                        }
                    }
                } catch (e: Exception) {
                    continue
                }
            }
            
            variants
        } catch (e: Exception) {
            when (e) {
                is TransitError -> throw e
                else -> throw TransitError.NetworkError(e)
            }
        }
    }

    suspend fun getBulkVariantsForStops(stopNumbers: List<Int>): List<Variant> = withContext(Dispatchers.IO) {
        rateLimiter.waitIfNeeded()

        try {
            val currentDate = Date()
            val calendar = Calendar.getInstance()
            calendar.time = currentDate
            val startDate = calendar.time

            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val endDate = calendar.time

            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val startTime = dateFormat.format(startDate)
            val endTime = dateFormat.format(endDate)

            val stopsString = stopNumbers.joinToString(",")

            val response = apiService.getVariantsForStops(
                startTime = startTime,
                endTime = endTime,
                stops = stopsString,
                usage = "short",
                apiKey = PeekTransitConstants.TRANSIT_API_KEY
            )

            if (!response.isSuccessful) {
                throw TransitError.NetworkError(IOException("HTTP ${response.code()}"))
            }

            val jsonResponse = response.body() ?: throw TransitError.InvalidData
            val variantsArray = jsonResponse.getAsJsonArray("variants") ?: return@withContext emptyList()

            val variants = mutableListOf<Variant>()

            for (element in variantsArray) {
                try {
                    val variantObj = element.asJsonObject
                    val variant = gson.fromJson(variantObj, Variant::class.java)
                    variants.add(variant)
                } catch (e: Exception) {
                    continue
                }
            }

            variants
        } catch (e: Exception) {
            when (e) {
                is TransitError -> throw e
                else -> throw TransitError.NetworkError(e)
            }
        }
    }

    suspend fun cleanScheduleMixedTimeFormat(
        schedule: JsonObject
    ): List<String> = withContext(Dispatchers.IO) {
        val busScheduleList = mutableListOf<String>()
        val currentDate = Date()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val variantMinutesAdded = mutableMapOf<String, Boolean>()
        val tempScheduleEntries = mutableListOf<ScheduleEntry>()

        try {
            val stopSchedule = schedule.getAsJsonObject("stop-schedule")
            val routeSchedules = stopSchedule.getAsJsonArray("route-schedules")

            for (routeScheduleElement in routeSchedules) {
                val routeSchedule = routeScheduleElement.asJsonObject
                val scheduledStops = routeSchedule.getAsJsonArray("scheduled-stops")

                for (stopElement in scheduledStops) {
                    val stop = stopElement.asJsonObject
                    val variant = stop.getAsJsonObject("variant")
                    var variantKey = variant.get("key")?.asString ?: continue
                    val variantName = variant.get("name")?.asString ?: continue
                    val cancelled = stop.get("cancelled")?.asString == "true"
                    val times = stop.getAsJsonObject("times")
                    val arrival = times.getAsJsonObject("departure")

                    val estimatedTime = arrival.get("estimated")?.asString
                    val scheduledTime = arrival.get("scheduled")?.asString
                    var finalArrivalText = ""
                    var arrivalState = PeekTransitConstants.OK_STATUS_TEXT
                    var sortValue = 0

                    if (estimatedTime != null && scheduledTime != null) {
                        try {
                            val estimatedTimeParsedDateAndTime = estimatedTime.split("T")
                            val scheduledTimeParsedDateAndTime = scheduledTime.split("T")
                            val estimatedTimeParsedDate = estimatedTimeParsedDateAndTime[0].split("-")
                            val estimatedTimeParsedTime = estimatedTimeParsedDateAndTime[1].split(":")
                            val scheduledTimeParsedDate = scheduledTimeParsedDateAndTime[0].split("-")
                            val scheduledTimeParsedTime = scheduledTimeParsedDateAndTime[1].split(":")

                            val estimatedTotalMinutes = (estimatedTimeParsedDate[0].toInt() * 525600) +
                                    (estimatedTimeParsedDate[1].toInt() * 43800) +
                                    (estimatedTimeParsedDate[2].toInt() * 1440) +
                                    (estimatedTimeParsedTime[0].toInt() * 60) +
                                    estimatedTimeParsedTime[1].toInt()

                            val scheduledTotalMinutes = (scheduledTimeParsedDate[0].toInt() * 525600) +
                                    (scheduledTimeParsedDate[1].toInt() * 43800) +
                                    (scheduledTimeParsedDate[2].toInt() * 1440) +
                                    (scheduledTimeParsedTime[0].toInt() * 60) +
                                    scheduledTimeParsedTime[1].toInt()

                            val calendar = Calendar.getInstance()
                            calendar.time = currentDate
                            val currentTotalMinutes = (calendar.get(Calendar.YEAR) * 525600) +
                                    (calendar.get(Calendar.MONTH) * 43800) +
                                    (calendar.get(Calendar.DAY_OF_MONTH) * 1440) +
                                    (calendar.get(Calendar.HOUR_OF_DAY) * 60) +
                                    calendar.get(Calendar.MINUTE)

                            val estimatedDate = dateFormatter.parse(estimatedTime) ?: continue
                            val scheduledDate = dateFormatter.parse(scheduledTime) ?: continue

                            val timeDifferenceSeconds = estimatedDate.time - currentDate.time
                            val timeDifference = kotlin.math.ceil(timeDifferenceSeconds / 60000.0).toInt()
                            val delay = kotlin.math.round((estimatedDate.time - scheduledDate.time) / 60000.0).toInt()

                            if (timeDifference < -PeekTransitConstants.MINUTES_ALLOWED_TO_KEEP_DUE_BUSES_IN_SCHEDULE) {
                                continue
                            }

                            if (cancelled) {
                                arrivalState = PeekTransitConstants.CANCELLED_STATUS_TEXT
                                finalArrivalText = ""
                                sortValue = Int.MAX_VALUE
                            } else {
                                var timeIn12HourFormat = ""
                                var timeInMinutes = ""
                                var finalHour = estimatedTimeParsedTime[0].toInt()
                                val am = finalHour < 12

                                when {
                                    finalHour == 0 -> finalHour = 12
                                    finalHour > 12 -> finalHour -= 12
                                }

                                timeIn12HourFormat = "$finalHour:${estimatedTimeParsedTime[1]} ${if (am) PeekTransitConstants.GLOBAL_AM_TEXT else PeekTransitConstants.GLOBAL_PM_TEXT}"

                                if (timeDifference <= PeekTransitConstants.PERIOD_BEFORE_SHOWING_MINUTES_UNTIL_NEXT_BUS_IN_MINUTES) {
                                    timeInMinutes = "$timeDifference ${PeekTransitConstants.MINUTES_REMAINING_TEXT}"
                                }

                                when {
                                    delay > 0 && timeDifference <= PeekTransitConstants.PERIOD_BEFORE_SHOWING_MINUTES_UNTIL_NEXT_BUS_IN_MINUTES -> {
                                        arrivalState = PeekTransitConstants.LATE_STATUS_TEXT
                                    }
                                    delay < 0 && timeDifference <= PeekTransitConstants.PERIOD_BEFORE_SHOWING_MINUTES_UNTIL_NEXT_BUS_IN_MINUTES -> {
                                        arrivalState = PeekTransitConstants.EARLY_STATUS_TEXT
                                    }
                                    else -> {
                                        arrivalState = PeekTransitConstants.OK_STATUS_TEXT
                                    }
                                }

                                sortValue = if (timeDifference <= 0 && timeDifference >= -PeekTransitConstants.MINUTES_ALLOWED_TO_KEEP_DUE_BUSES_IN_SCHEDULE) {
                                    timeInMinutes = PeekTransitConstants.DUE_STATUS_TEXT
                                    -1
                                } else {
                                    timeDifference
                                }

                                variantKey = variantKey.split("-").firstOrNull() ?: variantKey
                                if (variantKey.contains("BLUE")) {
                                    variantKey = "B"
                                }

                                val variantIdentifier = "$variantKey${PeekTransitConstants.SCHEDULE_STRING_SEPARATOR}$variantName"

                                finalArrivalText = if (!variantMinutesAdded.getOrDefault(variantIdentifier, false) &&
                                    timeDifference <= PeekTransitConstants.PERIOD_BEFORE_SHOWING_MINUTES_UNTIL_NEXT_BUS_IN_MINUTES) {
                                    variantMinutesAdded[variantIdentifier] = true
                                    timeInMinutes
                                } else {
                                    timeIn12HourFormat
                                }
                            }
                        } catch (e: Exception) {
                            continue
                        }
                    } else {
                        finalArrivalText = "Time Unavailable"
                        sortValue = Int.MAX_VALUE
                    }

                    tempScheduleEntries.add(
                        ScheduleEntry(
                            key = variantKey,
                            name = variantName,
                            state = arrivalState,
                            time = finalArrivalText,
                            sortValue = sortValue
                        )
                    )
                }
            }
        } catch (e: Exception) {
            return@withContext emptyList()
        }

        val sortedEntries = tempScheduleEntries.sortedWith { entry1, entry2 ->
            when {
                entry1.time == PeekTransitConstants.DUE_STATUS_TEXT -> -1
                entry2.time == PeekTransitConstants.DUE_STATUS_TEXT -> 1
                entry1.sortValue != entry2.sortValue -> entry1.sortValue.compareTo(entry2.sortValue)
                else -> {
                    val route1 = entry1.key.toIntOrNull()
                    val route2 = entry2.key.toIntOrNull()
                    when {
                        route1 != null && route2 != null -> route1.compareTo(route2)
                        else -> entry1.key.compareTo(entry2.key)
                    }
                }
            }
        }

        busScheduleList.addAll(
            sortedEntries.map { entry ->
                "${entry.key}${PeekTransitConstants.SCHEDULE_STRING_SEPARATOR}${entry.name}${PeekTransitConstants.SCHEDULE_STRING_SEPARATOR}${entry.state}${PeekTransitConstants.SCHEDULE_STRING_SEPARATOR}${entry.time}"
            }
        )

        return@withContext busScheduleList
    }

    private data class ScheduleEntry(
        val key: String,
        val name: String,
        val state: String,
        val time: String,
        val sortValue: Int
    )
    
    suspend fun getVariantsForStops(
        stops: List<Stop>,
        onStopEnriched: ((Stop) -> Unit)? = null
    ): List<Stop> = withContext(Dispatchers.IO) {
        val enrichedStops = mutableListOf<Stop>()
        
        for (stop in stops) {
            try {
                val enrichedStop = enrichStopWithVariants(stop)
                enrichedStops.add(enrichedStop)
                onStopEnriched?.invoke(enrichedStop)
            } catch (e: Exception) {
                enrichedStops.add(stop)
                onStopEnriched?.invoke(stop)
            }
        }
        
        enrichedStops
    }
    
    suspend fun getOnlyVariantsForStop(stop: Stop): List<Variant> = withContext(Dispatchers.IO) {
        if (stop.number == -1) {
            return@withContext emptyList()
        }
        
        try {
            val response = apiService.getVariantsForStop(
                stopNumber = stop.number,
                apiKey = PeekTransitConstants.TRANSIT_API_KEY
            )
            
            if (response.isSuccessful) {
                val jsonObject = response.body()
                val variantsArray = jsonObject?.getAsJsonArray("variants")
                
                if (variantsArray != null) {
                    val stopVariants = mutableListOf<Variant>()
                    val currentDate = Date()
                    
                    for (element in variantsArray) {
                        val variantObject = element.asJsonObject
                        val variant = Variant(
                            key = variantObject.get("key")?.asString ?: "Unknown",
                            name = variantObject.get("name")?.asString ?: "Unknown",
                            effectiveFrom = variantObject.get("effective-from")?.asString ?: "",
                            effectiveTo = variantObject.get("effective-to")?.asString ?: "",
                            backgroundColor = variantObject.get("background-color")?.asString,
                            borderColor = variantObject.get("border-color")?.asString,
                            textColor = variantObject.get("color")?.asString
                        )
                        stopVariants.add(variant)
                    }
                    
                    val filteredVariants = stopVariants.filter { variant ->
                        val keyPrefix = variant.key.take(1)
                        val validPrefix = keyPrefix !in listOf("S", "W", "I")
                        
                        val validEffectiveDate = run {
                            val effectiveFrom = variant.getEffectiveFromDate()
                            val effectiveTo = variant.getEffectiveToDate()
                            
                            (effectiveFrom == null || currentDate >= effectiveFrom) &&
                            (effectiveTo == null || currentDate <= effectiveTo)
                        }
                        
                        validPrefix && validEffectiveDate
                    }
                    
                    return@withContext filteredVariants.distinctBy { "${it.key}${PeekTransitConstants.COMPOSITE_KEY_LINKER_FOR_DICTIONARIES}${it.name}" }
                } else {
                    return@withContext emptyList()
                }
            } else {
                throw TransitError.NetworkError(IOException("API call failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            when (e) {
                is TransitError -> throw e
                else -> throw TransitError.NetworkError(IOException("Failed to fetch variants: ${e.message}"))
            }
        }
    }

    suspend fun getLocationKey(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        rateLimiter.waitIfNeeded()

        try {
            val response = apiService.getLocationKey(
                latitude = latitude.toString(),
                longitude = longitude.toString(),
                usage = if (PeekTransitConstants.GLOBAL_API_FOR_SHORT_USAGE) "short" else "long",
                apiKey = PeekTransitConstants.TRANSIT_API_KEY
            )

            if (!response.isSuccessful) {
                throw TransitError.NetworkError(IOException("HTTP ${response.code()}"))
            }

            val jsonResponse = response.body() ?: throw TransitError.InvalidData
            val locationsJson = jsonResponse.getAsJsonArray("locations")
                ?: throw TransitError.ParseError("No locations found")

            if (locationsJson.size() == 0) {
                return@withContext null
            }

            val firstLocation = locationsJson.get(0).asJsonObject
            val locationType = firstLocation.get("type").asString

            val key = when {
                firstLocation.has("key") && firstLocation.get("key").isJsonPrimitive -> {
                    val keyElement = firstLocation.get("key")
                    if (keyElement.isJsonPrimitive) {
                        if (keyElement.asJsonPrimitive.isString) {
                            keyElement.asString
                        } else {
                            keyElement.asInt.toString()
                        }
                    } else {
                        throw TransitError.ParseError("Invalid key format")
                    }
                }
                else -> throw TransitError.ParseError("No key found in location")
            }

            when (locationType) {
                "intersection" -> "intersections/$key"
                "monument" -> "monuments/$key"
                "address" -> "addresses/$key"
                else -> null
            }
        } catch (e: Exception) {
            when (e) {
                is TransitError -> throw e
                else -> throw TransitError.NetworkError(e)
            }
        }
    }

    suspend fun findTrip(origin: Location, destination: Location): List<TripPlan> = withContext(Dispatchers.IO) {
        val allPlans = mutableListOf<TripPlan>()

        try {
            val originParam = "geo/${origin.latitude},${origin.longitude}"
            val destinationParam = "geo/${destination.latitude},${destination.longitude}"

            val initialResponse = apiService.findTrip(
                origin = originParam,
                destination = destinationParam,
                usage = if (PeekTransitConstants.GLOBAL_API_FOR_SHORT_USAGE) "short" else "long",
                apiKey = PeekTransitConstants.TRANSIT_API_KEY
            )

            if (initialResponse.isSuccessful) {
                val jsonResponse = initialResponse.body()
                jsonResponse?.getAsJsonArray("plans")?.let { plansArray ->
                    for (i in 0 until plansArray.size()) {
                        try {
                            val planJson = plansArray.get(i).asJsonObject
                            val planMap = convertJsonToMap(planJson)
                            TripPlan.fromDict(planMap)?.let { allPlans.add(it) }
                        } catch (e: Exception) {
                            continue
                        }
                    }
                }
            }

            var foundAtTransfers: Int? = null

            for (transfers in 0..5) {
                val transferResponse = apiService.findTrip(
                    origin = originParam,
                    destination = destinationParam,
                    maxTransfers = transfers,
                    usage = if (PeekTransitConstants.GLOBAL_API_FOR_SHORT_USAGE) "short" else "long",
                    apiKey = PeekTransitConstants.TRANSIT_API_KEY
                )

                if (!transferResponse.isSuccessful) {
                    continue
                }

                val transferJsonResponse = transferResponse.body() ?: continue
                val transferPlansArray = transferJsonResponse.getAsJsonArray("plans") ?: continue

                val plansForThisTransfer = mutableListOf<TripPlan>()
                for (i in 0 until transferPlansArray.size()) {
                    try {
                        val planJson = transferPlansArray.get(i).asJsonObject
                        val planMap = convertJsonToMap(planJson)
                        TripPlan.fromDict(planMap)?.let { plansForThisTransfer.add(it) }
                    } catch (e: Exception) {
                        continue
                    }
                }

                if (plansForThisTransfer.isNotEmpty()) {
                    if (foundAtTransfers == null) {
                        foundAtTransfers = transfers
                        allPlans.addAll(plansForThisTransfer)
                    } else {
                        allPlans.addAll(plansForThisTransfer)
                        break
                    }
                } else if (foundAtTransfers != null) {
                    continue
                }

                if (transfers < 5) {
                    delay(1000)
                }
            }

            return@withContext allPlans.distinct()
        } catch (e: Exception) {
            when (e) {
                is TransitError -> throw e
                else -> throw TransitError.NetworkError(e)
            }
        }
    }

    suspend fun findTripWithLocationKey(
        currentLocationKey: String,
        toLocationKey: String,
        walkSpeed: Double = 5.0,
        maxWalkTime: Int = 15,
        minTransferWait: Int = 2,
        maxTransferWait: Int = 15,
        maxTransfers: Int = 3,
        mode: String = "depart-after",
        date: Date? = null
    ): List<TripPlan> = withContext(Dispatchers.IO) {
        val allPlans = mutableListOf<TripPlan>()


        try {
            val initialResponse = apiService.findTrip(
                origin = currentLocationKey,
                destination = toLocationKey,
                usage = if (PeekTransitConstants.GLOBAL_API_FOR_SHORT_USAGE) "short" else "long",
                apiKey = PeekTransitConstants.TRANSIT_API_KEY
            )

            if (initialResponse.isSuccessful) {
                val jsonResponse = initialResponse.body()
                jsonResponse?.getAsJsonArray("plans")?.let { plansArray ->
                    for (i in 0 until plansArray.size()) {
                        try {
                            val planJson = plansArray.get(i).asJsonObject
                            val planMap = convertJsonToMap(planJson)
                            TripPlan.fromDict(planMap)?.let { allPlans.add(it) }
                        } catch (e: Exception) {
                            continue
                        }
                    }
                }
            }

            var foundAtTransfers: Int? = null

            for (transfers in 0..5) {
                val transferResponse = apiService.findTrip(
                    origin = currentLocationKey,
                    destination = toLocationKey,
                    maxTransfers = transfers,
//                    walkSpeed = walkSpeed,
//                    maxWalkTime = maxWalkTime,
//                    minTransferWait = minTransferWait,
//                    maxTransferWait = maxTransferWait,
//                    mode = mode,
//                    date = dateStr,
//                    time = timeStr,
                    usage = if (PeekTransitConstants.GLOBAL_API_FOR_SHORT_USAGE) "short" else "long",
                    apiKey = PeekTransitConstants.TRANSIT_API_KEY
                )

                if (!transferResponse.isSuccessful) {
                    continue
                }

                val transferJsonResponse = transferResponse.body() ?: continue
                val transferPlansArray = transferJsonResponse.getAsJsonArray("plans") ?: continue

                val plansForThisTransfer = mutableListOf<TripPlan>()
                for (i in 0 until transferPlansArray.size()) {
                    try {
                        val planJson = transferPlansArray.get(i).asJsonObject
                        val planMap = convertJsonToMap(planJson)
                        TripPlan.fromDict(planMap)?.let { plansForThisTransfer.add(it) }
                    } catch (e: Exception) {
                        continue
                    }
                }

                if (plansForThisTransfer.isNotEmpty()) {
                    if (foundAtTransfers == null) {
                        foundAtTransfers = transfers
                        allPlans.addAll(plansForThisTransfer)
                    } else {
                        allPlans.addAll(plansForThisTransfer)
                        break
                    }
                } else if (foundAtTransfers != null) {
                    continue
                }

                if (transfers < 5) {
                    delay(1000)
                }
            }

            return@withContext allPlans.distinct()
        } catch (e: Exception) {
            when (e) {
                is TransitError -> throw e
                else -> throw TransitError.NetworkError(e)
            }
        }
    }

    private fun convertJsonToMap(jsonObject: JsonObject): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()

        for ((key, element) in jsonObject.entrySet()) {
            when {
                element.isJsonNull -> result[key] = null
                element.isJsonPrimitive -> {
                    val primitive = element.asJsonPrimitive
                    when {
                        primitive.isBoolean -> result[key] = primitive.asBoolean
                        primitive.isNumber -> {
                            val number = primitive.asNumber
                            val doubleValue = number.toDouble()
                            if (doubleValue.toInt().toDouble() == doubleValue) {
                                result[key] = number.toInt()
                            } else {
                                result[key] = doubleValue
                            }
                        }
                        else -> result[key] = primitive.asString
                    }
                }
                element.isJsonArray -> {
                    val array = element.asJsonArray
                    val list = mutableListOf<Any?>()

                    for (arrayElement in array) {
                        when {
                            arrayElement.isJsonObject -> list.add(convertJsonToMap(arrayElement.asJsonObject))
                            arrayElement.isJsonPrimitive -> {
                                val primitive = arrayElement.asJsonPrimitive
                                when {
                                    primitive.isBoolean -> list.add(primitive.asBoolean)
                                    primitive.isNumber -> {
                                        val number = primitive.asNumber
                                        val doubleValue = number.toDouble()
                                        if (doubleValue.toInt().toDouble() == doubleValue) {
                                            list.add(number.toInt())
                                        } else {
                                            list.add(doubleValue)
                                        }
                                    }
                                    else -> list.add(primitive.asString)
                                }
                            }
                            arrayElement.isJsonNull -> list.add(null)
                            arrayElement.isJsonArray -> {
                                val nestedList = mutableListOf<Any?>()
                                for (nestedElement in arrayElement.asJsonArray) {
                                    if (nestedElement.isJsonObject) {
                                        nestedList.add(convertJsonToMap(nestedElement.asJsonObject))
                                    }
                                }
                                list.add(nestedList)
                            }
                        }
                    }
                    result[key] = list
                }
                element.isJsonObject -> result[key] = convertJsonToMap(element.asJsonObject)
            }
        }

        return result
    }

    companion object {
        @Volatile
        private var INSTANCE: WinnipegTransitAPI? = null
        
        fun getInstance(): WinnipegTransitAPI {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WinnipegTransitAPI().also { INSTANCE = it }
            }
        }
    }
}

