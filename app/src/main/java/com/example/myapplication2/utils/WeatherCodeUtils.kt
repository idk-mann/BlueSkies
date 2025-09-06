package com.example.myapplication2.utils



import android.util.Log
import com.example.myapplication2.R
import java.time.*



object WeatherCodeUtils {

    fun getIconForWeatherCode(code: Int): Int {
        return when (code) {
            1000 -> R.drawable.clear_day
            1010 -> R.drawable.clear_night
            1100 -> R.drawable.mostly_clear_day
            1101 -> R.drawable.partly_cloudy_day
            1102 -> R.drawable.mostly_cloudy
            1110 -> R.drawable.mostly_clear_night
            1111 -> R.drawable.partly_cloudy_night
            1001 -> R.drawable.cloudy
            2000 -> R.drawable.fog
            2100 -> R.drawable.fog_light
            4000 -> R.drawable.drizzle
            4001 -> R.drawable.rain
            4200 -> R.drawable.rain_light
            4201 -> R.drawable.rain_heavy
            5000 -> R.drawable.snow
            5001 -> R.drawable.flurries
            5100 -> R.drawable.snow_light
            5101 -> R.drawable.snow_heavy
            6000 -> R.drawable.freezing_drizzle
            6001 -> R.drawable.freezing_rain
            6200 -> R.drawable.freezing_rain_light
            6201 -> R.drawable.freezing_rain_heavy
            7000 -> R.drawable.ice_pellets
            7101 -> R.drawable.ice_pellets_heavy
            7102 -> R.drawable.ice_pellets_light
            8000 -> R.drawable.tstorm
            else -> R.drawable.unknown
        }
    }

    val weatherCodeMap = mapOf(
        1000 to "Clear",
        1010 to "Clear Night",
        1100 to "Mostly Clear",
        1101 to "Partly Cloudy",
        1102 to "Mostly Cloudy",
        1110 to "Mostly Clear Night",
        1111 to "Partly Cloudy Night",
        1001 to "Cloudy",
        2000 to "Fog",
        2100 to "Light Fog",
        4000 to "Drizzle",
        4001 to "Rain",
        4200 to "Light Rain",
        4201 to "Heavy Rain",
        5000 to "Snow",
        5001 to "Flurries",
        5100 to "Light Snow",
        5101 to "Heavy Snow",
        6000 to "Freezing Drizzle",
        6001 to "Freezing Rain",
        6200 to "Light Freezing Rain",
        6201 to "Heavy Freezing Rain",
        7000 to "Ice Pellets",
        7101 to "Heavy Ice Pellets",
        7102 to "Light Ice Pellets",
        8000 to "Thunderstorm"
    )

    fun getDescriptionForWeatherCode(code: Int): String {
        return weatherCodeMap[code] ?: "Unknown"
    }
}