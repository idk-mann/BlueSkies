package com.example.myapplication2

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.*

class SunriseSunsetManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sun_prefs", Context.MODE_PRIVATE)

    private val LAST_CALC_KEY = "last_sun_calc_time"
    private val SUNRISE_KEY = "sunrise_time"
    private val SUNSET_KEY = "sunset_time"
    private val LAST_LAT_KEY = "last_lat"
    private val LAST_LON_KEY = "last_lon"

    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    private fun shouldRecalculate(lat: Double, lon: Double): Boolean {
        val lastCalc = prefs.getLong(LAST_CALC_KEY, 0L)
        val now = System.currentTimeMillis()
        val threeDaysMillis = TimeUnit.DAYS.toMillis(3)

        val lastLat = prefs.getString(LAST_LAT_KEY, null)?.toDoubleOrNull()
        val lastLon = prefs.getString(LAST_LON_KEY, null)?.toDoubleOrNull()

        val locationChanged = lastLat != lat || lastLon != lon

        return (now - lastCalc) > threeDaysMillis || locationChanged
    }

    fun getSunriseSunset(lat: Double, lon: Double, date: LocalDate): Pair<String, String> {
        Log.d("SunEvents", "Getting sunrise/sunset for lat=$lat, lon=$lon, date=$date")

        if (date == LocalDate.now() && shouldRecalculate(lat, lon)) {
            val sunrise = calculateSunTime(lat, lon, date, true, 90.833)
            val sunset = calculateSunTime(lat, lon, date, false, 90.833)

            Log.d("SunEvents", "Calculated (today): sunrise=$sunrise, sunset=$sunset")

            saveSunriseSunset(sunrise.format(formatter), sunset.format(formatter))

            prefs.edit()
                .putString(LAST_LAT_KEY, lat.toString())
                .putString(LAST_LON_KEY, lon.toString())
                .apply()

            return Pair(sunrise.format(formatter), sunset.format(formatter))
        }

        if (date == LocalDate.now()) {
            val sunrise = prefs.getString(SUNRISE_KEY, "06:30:00") ?: "06:30:00"
            val sunset = prefs.getString(SUNSET_KEY, "19:00:00") ?: "19:00:00"

            Log.d("SunEvents", "Loaded from cache (today): sunrise=$sunrise, sunset=$sunset")

            return Pair(sunrise, sunset)
        }

        val sunrise = calculateSunTime(lat, lon, date, true, 90.833)
        val sunset = calculateSunTime(lat, lon, date, false, 90.833)

        Log.d("SunEvents", "Calculated (non-today): sunrise=$sunrise, sunset=$sunset")

        return Pair(sunrise.format(formatter), sunset.format(formatter))
    }

    fun getDawnDusk(lat: Double, lon: Double, date: LocalDate): Pair<String, String> {
        val dawn = calculateSunTime(lat, lon, date, true, 96.0)
        val dusk = calculateSunTime(lat, lon, date, false, 96.0)

        return Pair(dawn.format(formatter), dusk.format(formatter))
    }

    private fun saveSunriseSunset(sunrise: String, sunset: String) {
        prefs.edit()
            .putString(SUNRISE_KEY, sunrise)
            .putString(SUNSET_KEY, sunset)
            .putLong(LAST_CALC_KEY, System.currentTimeMillis())
            .apply()
    }

    private fun calculateSunTime(
        lat: Double,
        lon: Double,
        date: LocalDate,
        isSunrise: Boolean,
        zenith: Double
    ): LocalTime {
        val dayOfYear = date.dayOfYear.toDouble()
        val lngHour = lon / 15.0

        val t = if (isSunrise)
            dayOfYear + ((6 - lngHour) / 24)
        else
            dayOfYear + ((18 - lngHour) / 24)

        val M = (0.9856 * t) - 3.289
        var L = M + (1.916 * sin(Math.toRadians(M))) + (0.020 * sin(Math.toRadians(2 * M))) + 282.634
        L = (L + 360) % 360

        val RA = atan(0.91764 * tan(Math.toRadians(L)))
        var RAdeg = Math.toDegrees(RA)
        RAdeg = (RAdeg + 360) % 360

        val Lquadrant = floor(L / 90.0) * 90.0
        val RAquadrant = floor(RAdeg / 90.0) * 90.0
        RAdeg += (Lquadrant - RAquadrant)
        val RAhours = RAdeg / 15.0

        val sinDec = 0.39782 * sin(Math.toRadians(L))
        val cosDec = cos(asin(sinDec))
        val cosH = (cos(Math.toRadians(zenith)) - (sinDec * sin(Math.toRadians(lat)))) / (cosDec * cos(Math.toRadians(lat)))

        if (cosH > 1 || cosH < -1) return LocalTime.NOON // Polar fallback

        val H = if (isSunrise)
            360 - Math.toDegrees(acos(cosH))
        else
            Math.toDegrees(acos(cosH))

        val Hhours = H / 15.0
        val T = Hhours + RAhours - (0.06571 * t) - 6.622
        val UT = (T - lngHour + 24) % 24

        val seconds = (UT * 3600).toLong()
        return LocalTime.ofSecondOfDay(seconds)
    }
}
