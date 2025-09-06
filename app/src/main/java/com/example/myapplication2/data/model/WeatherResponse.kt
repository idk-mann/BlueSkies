    package com.example.myapplication2.data.model

data class WeatherResponse(
    val timelines: Timelines
)

data class Timelines(
    val hourly: List<HourlyData>
)

data class HourlyData(
    val time: String,
    val values: WeatherValues
)

data class WeatherValues(
    val temperature: Double?,
    val precipitationProbability: Double?,
    val windSpeed: Double?,
    val humidity: Double?,
    val uvIndex: Double?,
    val weatherCode: Int?,
    val cloudCover: Double?,
    val cloudCeiling: Double?,
    val precipitationIntensity: Double?,
    val rainIntensity: Double?,
    val cloudBase: Double?
)

// Optional simplified model for UI use
data class SimplifiedWeather(
    val time: String,
    val temperature: Double?,
    val precipitationProbability: Double?
)
