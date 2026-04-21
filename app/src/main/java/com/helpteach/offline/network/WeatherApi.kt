package com.helpteach.offline.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// WeatherAPI Models
data class WeatherResponse(
    val location: LocationData?,
    val current: CurrentData?,
    val forecast: ForecastData?
)

data class LocationData(
    val name: String,
    val country: String,
    val localtime: String
)

data class CurrentData(
    val temp_c: Double,
    val condition: ConditionData,
    val wind_kph: Double,
    val humidity: Int,
    val feelslike_c: Double
)

data class ConditionData(
    val text: String,
    val icon: String
)

data class ForecastData(
    val forecastday: List<ForecastDayData>
)

data class ForecastDayData(
    val date: String,
    val day: DayData
)

data class DayData(
    val maxtemp_c: Double,
    val mintemp_c: Double,
    val condition: ConditionData,
    val daily_chance_of_rain: Int,
    val daily_chance_of_snow: Int
)

interface WeatherApiService {
    @GET("v1/forecast.json")
    suspend fun getWeather(
        @Query("key") apiKey: String,
        @Query("q") city: String,
        @Query("days") days: Int = 5,
        @Query("aqi") aqi: String = "no",
        @Query("alerts") alerts: String = "no"
    ): WeatherResponse
}

object RetrofitInstance {
    private const val BASE_URL = "https://api.weatherapi.com/"
    // Provide a default free API key for demonstration, user should configure this in settings later
    const val DEFAULT_API_KEY = "eb481a50b86a43878b3174205242204"

    val api: WeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }
}
