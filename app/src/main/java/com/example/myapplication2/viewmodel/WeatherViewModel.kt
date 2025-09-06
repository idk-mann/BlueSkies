package com.example.myapplication2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.myapplication2.data.model.WeatherResponse
import com.example.myapplication2.data.model.HourlyData
import com.example.myapplication2.data.remote.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class WeatherViewModel : ViewModel() {

    private val _weatherData = MutableLiveData<List<HourlyData>>()
    val weatherData: LiveData<List<HourlyData>> get() = _weatherData

    private val _weatherState = MutableLiveData<WeatherState>(WeatherState.Idle)
    val weatherState: LiveData<WeatherState> get() = _weatherState

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    // Sealed class for state management
    sealed class WeatherState {
        object Idle : WeatherState()        // Initial state, no data fetched yet
        object Loading : WeatherState()     // Data is being fetched
        object Success : WeatherState()     // Data fetched successfully
        object Error : WeatherState()       // Error occurred
    }

    fun fetchWeather(location: String, apiKey: String) {
        _weatherState.value = WeatherState.Loading

        viewModelScope.launch {
            try {
                val response: WeatherResponse = RetrofitClient.weatherApi.getForecast(
                    location = location,
                    apiKey = apiKey
                )

                _weatherData.value = response.timelines.hourly
                _weatherState.value = WeatherState.Success
                _error.value = null

            } catch (e: HttpException) {
                _error.value = when (e.code()) {
                    401 -> "Unauthorized: Check API key"
                    404 -> "Location not found"
                    else -> "Server error ${e.code()}"
                }
                _weatherState.value = WeatherState.Error
            } catch (e: IOException) {
                _error.value = "Network error: Check your internet connection"
                _weatherState.value = WeatherState.Error
            } catch (e: Exception) {
                _error.value = e.message ?: "Unexpected error"
                _weatherState.value = WeatherState.Error
            }
        }
    }

    // Optional: Clear error state
    fun clearError() {
        _error.value = null
        _weatherState.value = WeatherState.Idle
    }
}