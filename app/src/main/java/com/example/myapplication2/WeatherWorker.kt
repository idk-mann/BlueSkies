package com.example.myapplication2

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.myapplication2.utils.WeatherCodeUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.runBlocking
import android.app.PendingIntent
import com.example.myapplication2.utils.defaultLocations

// Holds numeric weather data + a human-readable summary
data class WeatherInfo(
    val temperature: Double,
    val precipitationProbability: Double,
    val summary: String
)

class WeatherWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    private val prefs by lazy {
        PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }

    override fun doWork(): Result {
        val type = inputData.getString("notificationType") ?: return Result.failure()
        val time = inputData.getString("time") // only present for recurring notifications

        val weatherInfo = fetchWeatherData()

        if (weatherInfo != null) {
            when (type) {
                "DailyNotification" -> {
                    showNotification("Daily Weather", weatherInfo.summary)
                }
                "RecurringNotification" -> {
                    val title = if (!time.isNullOrEmpty()) {
                        "Weather Update at $time"
                    } else {
                        "Weather Update"
                    }
                    showNotification(title, weatherInfo.summary)
                }
                "RainWarning" -> {
                    if (weatherInfo.precipitationProbability > 50) {
                        showNotification(
                            "Rain Alert",
                            "Chance of rain: ${weatherInfo.precipitationProbability}%"
                        )
                    }
                }
                "TemperatureWarning" -> {
                    if (weatherInfo.temperature > 35) {
                        showNotification(
                            "Heat Alert",
                            "Temperature is ${weatherInfo.temperature}°C"
                        )
                    }
                }
            }
            return Result.success()
        } else {
            return Result.failure()
        }
    }


    private var locations: MutableMap<String, String> = mutableMapOf()

    private fun loadLocations() {
        val json = prefs.getString("user_locations_json", null)
        val saved: MutableMap<String, String> = if (json != null) {
            try {
                val type = object : TypeToken<MutableMap<String, String>>() {}.type
                Gson().fromJson(json, type)
            } catch (e: Exception) {
                mutableMapOf()
            }
        } else {
            mutableMapOf()
        }

        // Always add defaults back in (cannot be deleted)
        locations = defaultLocations().toMutableMap().apply {
            putAll(saved)
        }
    }



    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }

    private fun fetchWeatherData(): WeatherInfo? {
        loadLocations()

        val savedLocation = prefs.getString("selected_location_name", "Kuala Lumpur")
        val coordinatesString = locations[savedLocation]

        Log.d("WeatherWorker", "coordinatesString: $coordinatesString")

        // Determine final lat/lon
        val (lat, lon) = when (coordinatesString) {
            "gps" -> {

                if (!hasLocationPermissions()) {
                    Log.w("WeatherWorker", "Location permissions not granted")
                }

                if (!hasLocationPermissions() || !isLocationEnabled(context)) {
                    showLocationPermissionNotification()
                    return null // Don’t try API fetch, fallback
                }

                val loc = getLastKnownLocation(context)
                if (loc != null) {
                    Pair(loc.latitude, loc.longitude)
                } else {
                    Log.w("WeatherWorker", "No GPS location, fallback to KL")
                    Pair(3.1390, 101.6869)
                }
            }
            "none" -> {
                Log.w("WeatherWorker", "No location selected, fallback to KL")
                Pair(3.1390, 101.6869)
            }
            null -> {
                Pair(3.1390, 101.6869)
            }
            else -> {
                try {
                    val parts = coordinatesString.split(",").map { it.trim() }
                    Pair(parts[0].toDouble(), parts[1].toDouble())
                } catch (e: Exception) {
                    Log.e("WeatherWorker", "Invalid coordinates: $coordinatesString", e)
                    Pair(3.1390, 101.6869)
                }
            }
        }

        Log.d("WeatherWorkerLog", "Using coordinates: lat=$lat, lon=$lon")

        val apiKey = BuildConfig.TOMORROW_API_KEY
        val locationParam = "$lat,$lon"

        return try {
            val response = runBlocking {
                RetrofitClient.weatherApi.getForecast(
                    location = locationParam,
                    apiKey = apiKey
                )
            }

            val hourly = response.timelines.hourly.take(12)
            if (hourly.isEmpty()) return null

            var highTemp = Double.MIN_VALUE
            var lowTemp = Double.MAX_VALUE
            val weatherCodeCounts = mutableMapOf<Int, Int>()
            val rainPeriods = mutableListOf<Pair<Int, Int>>()

            var currentRainStart: Int? = null
            var currentRainDuration = 0

            for ((i, entry) in hourly.withIndex()) {
                val temp = entry.values.temperature ?: continue
                val precipProb = entry.values.precipitationProbability ?: 0.0
                val code = entry.values.weatherCode ?: 0

                if (temp > highTemp) highTemp = temp
                if (temp < lowTemp) lowTemp = temp

                weatherCodeCounts[code] = weatherCodeCounts.getOrDefault(code, 0) + 1

                if (precipProb > 50) {
                    if (currentRainStart == null) {
                        currentRainStart = i
                        currentRainDuration = 1
                    } else {
                        currentRainDuration++
                    }
                } else {
                    if (currentRainStart != null) {
                        rainPeriods.add(currentRainStart!! to currentRainDuration)
                        currentRainStart = null
                        currentRainDuration = 0
                    }
                }
            }

            if (currentRainStart != null) {
                rainPeriods.add(currentRainStart!! to currentRainDuration)
            }

            val dominantCode = weatherCodeCounts.maxByOrNull { it.value }?.key ?: 0
            val description = WeatherCodeUtils.getDescriptionForWeatherCode(dominantCode)

            val tempPart = "High: ${"%.1f".format(highTemp)}°C, Low: ${"%.1f".format(lowTemp)}°C"
            val rainPart = if (rainPeriods.isNotEmpty()) {
                rainPeriods.joinToString("; ") { (start, dur) ->
                    "Rain in ${start}h lasting ${dur}h"
                }
            } else {
                "No significant rain expected"
            }

            WeatherInfo(
                temperature = highTemp,
                precipitationProbability = 0.0,
                summary = "Today in $savedLocation: $tempPart\n$description\n$rainPart"
            )
        } catch (e: Exception) {
            Log.e("WeatherWorker", "Failed to fetch weather data", e)
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(context: Context): Location? {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasBackground = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (!(hasFine || hasCoarse) || !hasBackground) {
            Log.w("WeatherWorker", "No background location permission")
            return null
        }

        return try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            Tasks.await(fusedClient.lastLocation)
        } catch (e: Exception) {
            Log.e("WeatherWorker", "Error fetching last location", e)
            null
        }
    }

    private fun hasLocationPermissions(): Boolean {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasBackground = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Background permission not required before Android 10
        }

        return (hasFine || hasCoarse) && hasBackground
    }

    private fun showLocationPermissionNotification() {
        val channelId = "weather_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Weather Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // Intent to app location settings
        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )


        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle("Enable Location Access")
            .setContentText("Allow 'All the time' location access and keep Location ON for accurate weather updates.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "To get weather for your current location, please:\n" +
                        "1. Turn on Location (GPS).\n" +
                        "2. Grant 'Allow all the time' permission in Settings."
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(2001, builder.build())
    }



    private fun showNotification(title: String, message: String) {
        val channelId = "weather_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Weather Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1001, builder.build())
    }
}
