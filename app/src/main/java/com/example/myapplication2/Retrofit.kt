package com.example.myapplication2

import retrofit2.http.GET
import retrofit2.http.Query

// 1. Retrofit API Interface
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

data class SimplifiedWeather(
    val time: String,
    val temperature: Double?,
    val precipitationProbability: Double?
)

object RetrofitClient {
    private const val BASE_URL = "https://api.tomorrow.io/"

    val weatherApi: WeatherApiService by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }
}
