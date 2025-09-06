package com.example.myapplication2.data.remote

import retrofit2.http.GET


import retrofit2.http.Query
import com.example.myapplication2.data.model.WeatherResponse


interface WeatherApiService {
    @GET("v4/weather/forecast")
    suspend fun getForecast(
        @Query("location") location: String,
        @Query("fields") fields: String = "temperature,precipitationProbability,windSpeed,humidity,uvIndex,weatherCode,cloudCover,cloudCeiling,precipitationIntensity,rainIntensity,cloudBase",
        @Query("timesteps") timesteps: String = "1h",
        @Query("units") units: String = "metric",
        @Query("apikey") apiKey: String
    ): WeatherResponse
}
