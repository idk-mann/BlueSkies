package com.example.myapplication2.utils

import android.util.Log
import java.time.*
import java.time.format.DateTimeFormatter

object TimeUtils {

    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun getTimeZoneFromLocation(lat: Double, lon: Double): ZoneId {
        return try {
            val timeZoneId = TimezoneMapper.latLngToTimezoneString(lat, lon)
            ZoneId.of(timeZoneId)
        } catch (e: Exception) {
            Log.e("Timezone", "Fallback to UTC for [$lat, $lon]", e)
            ZoneId.of("UTC")
        }
    }

    fun parseToLocal(localDate: LocalDate, timeStr: String, zoneId: ZoneId, formatter: DateTimeFormatter): LocalTime {
        return try {
            val utcTime = LocalTime.parse(timeStr, formatter)
            val utcDateTime = ZonedDateTime.of(localDate, utcTime, ZoneOffset.UTC)
            val localDateTime = utcDateTime.withZoneSameInstant(zoneId)
            localDateTime.toLocalTime()
        } catch (e: Exception) {
            Log.e("parseToLocal", "Failed to parse: $timeStr for date $localDate", e)
            LocalTime.MIDNIGHT
        }
    }

    fun formatTime(hour: Int, minute: Int, use24HourFormat: Boolean): String {
        return if (use24HourFormat) {
            String.format("%02d:%02d", hour, minute)
        } else {
            val amPm = if (hour >= 12) "PM" else "AM"
            val hour12 = if (hour % 12 == 0) 12 else hour % 12
            String.format("%02d:%02d %s", hour12, minute, amPm)
        }
    }


}
