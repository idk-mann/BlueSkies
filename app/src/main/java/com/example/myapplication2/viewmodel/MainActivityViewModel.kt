package com.example.myapplication2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData

class MainActivityViewModel : ViewModel() {

    // Track if initial location load has been completed
    val hasInitialLocationLoad = MutableLiveData(false)

    // Track the current theme state
    val isLightTheme = MutableLiveData(true)

    // Store location data to avoid refetching
    val locationData = MutableLiveData<Pair<Double, Double>?>(null)

    // Store sun times to avoid recalculating
    val sunTimes = MutableLiveData<SunTimesData?>(null)

    // Store gradient information
    val gradientData = MutableLiveData<GradientData?>(null)
}

// Data class for sun times
data class SunTimesData(
    val sunriseT: String,
    val sunsetT: String,
    val dawnT: String,
    val duskT: String,
    val zoneId: String
)

// Data class for gradient information
data class GradientData(
    val colors: List<Int>,
    val period: String
)