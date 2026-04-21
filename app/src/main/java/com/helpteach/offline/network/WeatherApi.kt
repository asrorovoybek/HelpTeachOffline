package com.helpteach.offline.network

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// Data models
data class WeatherResponse(
    val location: LocationData?,
    val current: CurrentData?,
    val forecast: ForecastData?
)

data class LocationData(val name: String, val country: String, val localtime: String)
data class CurrentData(val temp_c: Double, val condition: ConditionData?, val wind_kph: Double, val humidity: Int, val feelslike_c: Double)
data class ConditionData(val text: String, val icon: String)
data class ForecastData(val forecastday: List<ForecastDayData>)
data class ForecastDayData(val date: String, val day: DayData)
data class DayData(val maxtemp_c: Double, val mintemp_c: Double, val condition: ConditionData?, val daily_chance_of_rain: Int, val daily_chance_of_snow: Int)

object NativeWeatherFetcher {
    private const val API_KEY = "44e650bf6a744cc3a3312205260804"
    private const val BASE_URL = "https://api.weatherapi.com/v1/forecast.json"

    fun fetchWeatherSync(city: String): WeatherResponse {
        val encodedCity = URLEncoder.encode(city, "UTF-8")
        val urlStr = "$BASE_URL?key=$API_KEY&q=$encodedCity&days=3&aqi=no&alerts=no"

        val url = URL(urlStr)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("Accept", "application/json")

        try {
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                // Read error stream for debugging
                val errorStream = connection.errorStream
                val errorBody = if (errorStream != null) {
                    BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                } else {
                    "Javob kodi: $responseCode"
                }
                throw Exception("Server xatosi ($responseCode): $errorBody")
            }

            val inputStream = connection.inputStream
                ?: throw Exception("Javob bo'sh qaytdi")

            val responseBody = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }

            if (responseBody.isBlank()) {
                throw Exception("Server bo'sh javob qaytardi")
            }

            return parseWeatherJson(responseBody)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseWeatherJson(jsonStr: String): WeatherResponse {
        val json = org.json.JSONObject(jsonStr)

        // Check for API error
        if (json.has("error")) {
            val errObj = json.optJSONObject("error")
            val errMsg = errObj?.optString("message", "Noma'lum API xatosi") ?: "Noma'lum API xatosi"
            throw Exception("API xatosi: $errMsg")
        }

        val locObj = json.optJSONObject("location")
        val location = if (locObj != null) {
            LocationData(
                name = locObj.optString("name", "Noma'lum"),
                country = locObj.optString("country", ""),
                localtime = locObj.optString("localtime", "")
            )
        } else null

        val curObj = json.optJSONObject("current")
        val current = if (curObj != null) {
            val condObj = curObj.optJSONObject("condition")
            CurrentData(
                temp_c = curObj.optDouble("temp_c", 0.0),
                condition = if (condObj != null) ConditionData(
                    text = condObj.optString("text", ""),
                    icon = condObj.optString("icon", "")
                ) else null,
                wind_kph = curObj.optDouble("wind_kph", 0.0),
                humidity = curObj.optInt("humidity", 0),
                feelslike_c = curObj.optDouble("feelslike_c", 0.0)
            )
        } else null

        val forecastObj = json.optJSONObject("forecast")
        val forecast = if (forecastObj != null) {
            val daysArray = forecastObj.optJSONArray("forecastday")
            val daysList = mutableListOf<ForecastDayData>()
            if (daysArray != null) {
                for (i in 0 until daysArray.length()) {
                    val dayItem = daysArray.optJSONObject(i) ?: continue
                    val dayDataObj = dayItem.optJSONObject("day") ?: continue
                    val condObj = dayDataObj.optJSONObject("condition")
                    daysList.add(
                        ForecastDayData(
                            date = dayItem.optString("date", ""),
                            day = DayData(
                                maxtemp_c = dayDataObj.optDouble("maxtemp_c", 0.0),
                                mintemp_c = dayDataObj.optDouble("mintemp_c", 0.0),
                                condition = if (condObj != null) ConditionData(
                                    text = condObj.optString("text", ""),
                                    icon = condObj.optString("icon", "")
                                ) else null,
                                daily_chance_of_rain = dayDataObj.optInt("daily_chance_of_rain", 0),
                                daily_chance_of_snow = dayDataObj.optInt("daily_chance_of_snow", 0)
                            )
                        )
                    )
                }
            }
            ForecastData(daysList)
        } else null

        return WeatherResponse(location, current, forecast)
    }
}
