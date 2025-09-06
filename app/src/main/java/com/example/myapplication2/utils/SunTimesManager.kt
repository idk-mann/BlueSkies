package com.example.myapplication2.utils

import com.example.myapplication2.R
import java.time.LocalTime

object SunTimesManager {
    var dawn: LocalTime? = null
    var sunrise: LocalTime? = null
    var sunset: LocalTime? = null
    var dusk: LocalTime? = null
    var now: LocalTime? = null

    fun isInitialized(): Boolean {
        return dawn != null && sunrise != null && sunset != null && dusk != null
    }

    fun getCurrentTextColorRes(): Int {
        val fallbackNow = LocalTime.now()
        return TimeBasedColors.getCurrentTextColorRes(
            now ?: fallbackNow,
            dawn, sunrise, sunset, dusk
        )
    }

    fun getCurrentBackgroundColorRes(): Int {
        val fallbackNow = LocalTime.now()
        return TimeBasedColors.getCurrentBackgroundColorRes(
            now ?: fallbackNow,
            dawn, sunrise, sunset, dusk
        )
    }
}
