package com.helpteach.offline.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.helpteach.offline.viewmodel.AppViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Bugun", Icons.Filled.Home)
    object Schedule : Screen("schedule", "Jadval", Icons.Filled.DateRange)
    object Tasks : Screen("tasks", "Vazifalar", Icons.Filled.CheckCircle)
    object Weather : Screen("weather", "Ob-havo", Icons.Filled.Cloud)
    object Profile : Screen("profile", "Profil", Icons.Filled.AccountCircle)
}

val items = listOf(
    Screen.Home,
    Screen.Schedule,
    Screen.Tasks,
    Screen.Weather,
    Screen.Profile
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(viewModel: AppViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(viewModel) }
            composable(Screen.Schedule.route) { ScheduleScreen(viewModel) }
            composable(Screen.Tasks.route) { TasksScreen(viewModel) }
            composable(Screen.Weather.route) { WeatherScreen(viewModel) }
            composable(Screen.Profile.route) { ProfileScreen(viewModel) }
        }
    }
}
