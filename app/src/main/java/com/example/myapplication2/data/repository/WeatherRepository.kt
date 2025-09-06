package com.example.myapplication2.data.repository

import com.example.myapplication2.data.remote.RetrofitClient
import com.example.myapplication2.data.model.WeatherResponse

class WeatherRepository {
    private val api = RetrofitClient.weatherApi

    suspend fun getForecast(location: String, apiKey: String): WeatherResponse {
        return api.getForecast(location, apiKey = apiKey)
    }
}
