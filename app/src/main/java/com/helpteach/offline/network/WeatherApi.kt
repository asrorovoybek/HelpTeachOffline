package com.helpteach.offline.network

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
    val condition: ConditionData?,
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
    val condition: ConditionData?,
    val daily_chance_of_rain: Int,
    val daily_chance_of_snow: Int
)

object NativeWeatherFetcher {
    private const val API_KEY = "eb481a50b86a43878b3174205242204"

    fun fetchWeatherSync(city: String): WeatherResponse {
        val urlStr = "https://api.weatherapi.com/v1/forecast.json?key=\$API_KEY&q=\$city&days=5&aqi=no&alerts=no"
        val url = java.net.URL(urlStr)
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10000
        conn.readTimeout = 10000

        val responseCode = conn.responseCode
        if (responseCode != 200) {
            throw Exception("HTTP Xatolik: \$responseCode")
        }

        val scanner = java.util.Scanner(conn.inputStream)
        val response = scanner.useDelimiter("\\A").next()
        scanner.close()

        val json = org.json.JSONObject(response)
        
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
                condition = if (condObj != null) ConditionData(condObj.optString("text", ""), condObj.optString("icon", "")) else null,
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
                                condition = if (condObj != null) ConditionData(condObj.optString("text", ""), condObj.optString("icon", "")) else null,
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
