package com.helpteach.offline.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.helpteach.offline.R
import com.helpteach.offline.viewmodel.AppViewModel

sealed class Screen(val route: String, val navTitle: String, val topBarTitle: String, val icon: ImageVector) {
    object Home : Screen("home", "Bugun", "Bugungi reja", Icons.Filled.Home)
    object Schedule : Screen("schedule", "Jadval", "Dars jadvali", Icons.Filled.DateRange)
    object Tasks : Screen("tasks", "Vazifalar", "Vazifalar", Icons.Filled.CheckCircle)
    object Weather : Screen("weather", "Ob-havo", "Ob-havo", Icons.Filled.Cloud)
    object Profile : Screen("profile", "Profil", "Mening profilim", Icons.Filled.AccountCircle)
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
    var showMenu by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentScreen = items.find { it.route == currentDestination?.route } ?: Screen.Home

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        currentScreen.topBarTitle, 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menyu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.width(200.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Dastur haqida ℹ️", fontWeight = FontWeight.Medium) },
                            onClick = {
                                showMenu = false
                                showAboutDialog = true
                            },
                            leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) }
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp
            ) {
                items.forEach { screen ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                imageVector = screen.icon, 
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            ) 
                        },
                        label = { 
                            Text(
                                screen.navTitle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            ) 
                        },
                        selected = isSelected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
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

    if (showAboutDialog) {
        AboutAppDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
fun AboutAppDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Dastur Haqida")
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(100.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.muallif),
                        contentDescription = "Muallif Rasmi",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("HelpTeach Offline", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Versiya: 1.0.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Divider()
                
                Text("Muallif: Asrorov Oybek \uD83D\uDC68\u200D\uD83D\uDCBB", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ContactRowDialog(icon = Icons.Filled.Phone, title = "Telefon", value = "+998 91 810 95 96") {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+998918109596")))
                    }
                    ContactRowDialog(icon = Icons.Filled.Email, title = "E-mail", value = "AsrorovOybek@gmail.com") {
                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:AsrorovOybek@gmail.com")))
                    }
                    ContactRowDialog(icon = Icons.Filled.Send, title = "Telegram", value = "@Asrorov_Oybek") {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Asrorov_Oybek")))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("Yopish")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactRowDialog(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
