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

    Scaffold { padding ->
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
                    val today = state.data.forecast?.forecastday?.firstOrNull()?.day
                    
                    if (current == null || location == null) {
                        Text("Ma'lumot topilmadi. API kalit noto'g'ri bo'lishi mumkin.")
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = location.name ?: "Noma'lum",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = location.localtime ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "${current.temp_c}°C",
                                        style = MaterialTheme.typography.displayMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = current.condition?.text ?: "", 
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                    Text("Tafsilotlar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    Text("His qilinadi: ${current.feelslike_c}°C", style = MaterialTheme.typography.bodyLarge)
                                    Text("Shamol: ${current.wind_kph} km/soat", style = MaterialTheme.typography.bodyLarge)
                                    Text("Namlik: ${current.humidity}%", style = MaterialTheme.typography.bodyLarge)
                                    if (today != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Bugun: ${today.mintemp_c}°C dan ${today.maxtemp_c}°C gacha", style = MaterialTheme.typography.bodyLarge)
                                        Text("Yomg'ir ehtimoli: ${today.daily_chance_of_rain}%", style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
