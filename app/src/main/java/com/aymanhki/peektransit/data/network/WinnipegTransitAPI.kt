package com.aymanhki.peektransit.data.network

import android.location.Location
import com.aymanhki.peektransit.data.models.*
import com.aymanhki.peektransit.utils.PeekTransitConstants
import com.aymanhki.peektransit.utils.TimeFormat
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
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
        @Query("api-key") apiKey: String
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
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS) 
        .writeTimeout(15, TimeUnit.SECONDS)
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
                distance = PeekTransitConstants.STOPS_DISTANCE_RADIUS.toInt().toString(),
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
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
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
            calendar.add(Calendar.HOUR, PeekTransitConstants.TIME_PERIOD_ALLOWED_FOR_NEXT_BUS_ROUTES)
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
                    val cancelled = stop.get("cancelled")?.asString == "true"
                    val times = stop.getAsJsonObject("times")
                    val arrival = times.getAsJsonObject("departure")
                    
                    val estimatedTime = arrival.get("estimated")?.asString
                    val scheduledTime = arrival.get("scheduled")?.asString

                    var finalArrivalText = ""
                    
                    if (estimatedTime != null && scheduledTime != null) {
                        try {
                            val estimatedDate = dateFormatter.parse(estimatedTime) ?: continue
                            val scheduledDate = dateFormatter.parse(scheduledTime) ?: continue
                            
                            val timeDifferenceMs = estimatedDate.time - currentDate.time
                            val timeDifference = (timeDifferenceMs / 60000).toInt()
                            val delay = ((estimatedDate.time - scheduledDate.time) / 60000).toInt()
                            
                            if (timeDifference < -PeekTransitConstants.MINUTES_ALLOWED_TO_KEEP_DUE_BUSES_IN_SCHEDULE) {
                                continue
                            }
                            

                            var arrivalState = PeekTransitConstants.OK_STATUS_TEXT
                            
                            if (cancelled) {
                                arrivalState = PeekTransitConstants.CANCELLED_STATUS_TEXT
                                finalArrivalText = ""
                            } else {
                                when {
                                    timeDifference < 0 && timeFormat != TimeFormat.CLOCK_TIME -> {
                                        finalArrivalText = "${-timeDifference} ${PeekTransitConstants.MINUTES_PASSED_TEXT}"
                                    }
                                    timeDifference <= PeekTransitConstants.PERIOD_BEFORE_SHOWING_MINUTES_UNTIL_NEXT_BUS && timeFormat != TimeFormat.CLOCK_TIME -> {
                                        finalArrivalText = "$timeDifference ${PeekTransitConstants.MINUTES_REMAINING_TEXT}"
                                    }
                                    else -> {
                                        val calendar = Calendar.getInstance()
                                        calendar.time = estimatedDate
                                        var hour = calendar.get(Calendar.HOUR_OF_DAY)
                                        val minute = calendar.get(Calendar.MINUTE)
                                        val am = hour < 12
                                        
                                        when {
                                            hour == 0 -> hour = 12
                                            hour > 12 -> hour -= 12
                                        }
                                        
                                        val minuteStr = if (minute < 10) "0$minute" else minute.toString()
                                        finalArrivalText = "$hour:$minuteStr ${if (am) PeekTransitConstants.GLOBAL_AM_TEXT else PeekTransitConstants.GLOBAL_PM_TEXT}"
                                    }
                                }
                                
                                when {
                                    delay > 0 && timeDifference <= PeekTransitConstants.PERIOD_BEFORE_SHOWING_MINUTES_UNTIL_NEXT_BUS && timeFormat != TimeFormat.CLOCK_TIME -> {
                                        arrivalState = PeekTransitConstants.LATE_STATUS_TEXT
                                        finalArrivalText = "$timeDifference ${PeekTransitConstants.MINUTES_REMAINING_TEXT}"
                                    }
                                    delay < 0 && timeDifference <= PeekTransitConstants.PERIOD_BEFORE_SHOWING_MINUTES_UNTIL_NEXT_BUS -> {
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
                            
                            variantKey = variantKey.split("-").firstOrNull() ?: variantKey
                            if (variantKey.contains("BLUE")) {
                                variantKey = "B"
                            }
                            
                            busScheduleList.add("$variantKey${PeekTransitConstants.SCHEDULE_STRING_SEPARATOR}$variantName${PeekTransitConstants.SCHEDULE_STRING_SEPARATOR}$arrivalState${PeekTransitConstants.SCHEDULE_STRING_SEPARATOR}$finalArrivalText")
                        } catch (e: Exception) {
                            continue
                        }
                    } else {
                        finalArrivalText = "Time Unavailable"
                        busScheduleList.add("$variantKey${PeekTransitConstants.SCHEDULE_STRING_SEPARATOR}$variantName${PeekTransitConstants.SCHEDULE_STRING_SEPARATOR}${PeekTransitConstants.OK_STATUS_TEXT}${PeekTransitConstants.SCHEDULE_STRING_SEPARATOR}$finalArrivalText")
                    }
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }
        
        return busScheduleList.sortedWith { str1, str2 ->
            val componentsA = str1.split(PeekTransitConstants.SCHEDULE_STRING_SEPARATOR)
            val componentsB = str2.split(PeekTransitConstants.SCHEDULE_STRING_SEPARATOR)
            
            if (componentsA.size < 4 || componentsB.size < 4) return@sortedWith 0
            
            val timeA = componentsA[3]
            val timeB = componentsB[3]
            val statusA = if (componentsA.size > 2) componentsA[2] else PeekTransitConstants.OK_STATUS_TEXT
            val statusB = if (componentsB.size > 2) componentsB[2] else PeekTransitConstants.OK_STATUS_TEXT
            
            when {
                timeA == PeekTransitConstants.DUE_STATUS_TEXT && timeB == PeekTransitConstants.DUE_STATUS_TEXT -> 0
                timeA == PeekTransitConstants.DUE_STATUS_TEXT -> -1
                timeB == PeekTransitConstants.DUE_STATUS_TEXT -> 1
                else -> {
                    val isMinutesA = timeA.endsWith(PeekTransitConstants.MINUTES_REMAINING_TEXT)
                    val isMinutesB = timeB.endsWith(PeekTransitConstants.MINUTES_REMAINING_TEXT)
                    
                    when {
                        isMinutesA && isMinutesB -> {
                            val minutesA = timeA.split(" ")[0].toIntOrNull() ?: 999
                            val minutesB = timeB.split(" ")[0].toIntOrNull() ?: 999
                            
                            if (minutesA == minutesB) {
                                compareByStatus(statusA, statusB)
                            } else {
                                minutesA.compareTo(minutesB)
                            }
                        }

                        isMinutesA -> -1
                        isMinutesB -> 1
                        else -> {
                            val timeCompare = compareClockTimes(timeA, timeB, currentDate)
                            if (timeCompare == 0) {
                                compareByStatus(statusA, statusB)
                            } else {
                                timeCompare
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
                usage = "long",
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
                usage = "long",
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