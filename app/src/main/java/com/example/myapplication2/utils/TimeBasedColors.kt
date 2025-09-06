package com.example.myapplication2.utils

import com.example.myapplication2.R
import java.time.LocalTime

object TimeBasedColors {

    fun getCurrentBackgroundColorRes(
        now: LocalTime,
        dawnT: LocalTime?,
        sunriseT: LocalTime?,
        sunsetT: LocalTime?,
        duskT: LocalTime?
    ): Int {
        val dawn = dawnT ?: return R.color.colorSurface
        val sunrise = sunriseT ?: return R.color.colorSurface
        val sunset = sunsetT ?: return R.color.colorSurfaceDark
        val dusk = duskT ?: return R.color.colorSurfaceDark

        return when {
            now.isAfter(dawn) && now.isBefore(sunrise) -> R.color.colorSurface        // Early morning
            now.isAfter(sunrise) && now.isBefore(sunset) -> R.color.colorSurface      // Daytime
            now.isAfter(sunset) && now.isBefore(dusk) -> R.color.colorSurfaceDark     // Evening
            else -> R.color.colorSurfaceDark                                           // Night
        }
    }

    fun getCurrentTextColorRes(
        now: LocalTime,
        dawnT: LocalTime?,
        sunriseT: LocalTime?,
        sunsetT: LocalTime?,
        duskT: LocalTime?
    ): Int {
        val dawn = dawnT ?: return R.color.colorOnSurfaceSecondaryDark
        val sunrise = sunriseT ?: return R.color.colorOnSurfaceSecondaryDark
        val sunset = sunsetT ?: return R.color.colorOnSurfaceSecondaryDark
        val dusk = duskT ?: return R.color.colorOnSurfaceSecondaryDark

        return when {
            now.isAfter(dawn) && now.isBefore(sunrise) -> R.color.colorOnSurfaceSecondary       // Early morning
            now.isAfter(sunrise) && now.isBefore(dusk) -> R.color.colorOnSurface                // Daytime
            now.isAfter(dusk) && now.isBefore(sunset) -> R.color.colorOnSurfaceSecondaryDark    // Twilight (note: this condition may be logically flawed, see note below)
            else -> R.color.colorOnSurfaceDark                                                  // Night
        }
    }
}
