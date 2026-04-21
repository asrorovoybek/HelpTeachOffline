package com.helpteach.offline.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.helpteach.offline.viewmodel.AppViewModel
import com.helpteach.offline.viewmodel.WeatherState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(viewModel: AppViewModel) {
    val settings by viewModel.settings.collectAsState()
    val weatherState by viewModel.weatherState.collectAsState()
    
    val city = settings?.weatherCity ?: "Toshkent"
    
    LaunchedEffect(city) {
        viewModel.fetchWeather(city)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ob-havo: $city") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val state = weatherState) {
                is WeatherState.Loading -> CircularProgressIndicator()
                is WeatherState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Xatolik yuz berdi:", color = MaterialTheme.colorScheme.error)
                        Text(state.message)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.fetchWeather(city) }) {
                            Text("Qayta urinish")
                        }
                    }
                }
                is WeatherState.Success -> {
                    val current = state.data.current
                    val location = state.data.location
                    val today = state.data.forecast.forecastday.firstOrNull()?.day
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = location.name,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = location.localtime)
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "${current.temp_c}°C",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(text = current.condition.text, style = MaterialTheme.typography.titleLarge)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Tafsilotlar", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("His qilinadi: ${current.feelslike_c}°C")
                                Text("Shamol: ${current.wind_kph} km/soat")
                                Text("Namlik: ${current.humidity}%")
                                if (today != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Bugun: ${today.mintemp_c}°C dan ${today.maxtemp_c}°C gacha")
                                    Text("Yomg'ir ehtimoli: ${today.daily_chance_of_rain}%")
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Bu ma'lumotlar WeatherAPI orqali olinmoqda va faqatgina shu oynada internet sarflanadi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
