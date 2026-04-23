package com.helpteach.offline.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    var showHelpDialog by remember { mutableStateOf(false) }

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
                            text = { Text("Yordam", fontWeight = FontWeight.Medium) },
                            onClick = {
                                showMenu = false
                                showHelpDialog = true
                            },
                            leadingIcon = { Icon(Icons.Filled.Help, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Dastur haqida", fontWeight = FontWeight.Medium) },
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
        AboutAppDialog(viewModel = viewModel, onDismiss = { showAboutDialog = false })
    }

    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }
}

@Composable
fun AboutAppDialog(viewModel: com.helpteach.offline.viewmodel.AppViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var clickCount by remember { mutableStateOf(0) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    if (showPasswordDialog) {
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false; clickCount = 0 },
            title = { Text("Maxfiy bo'lim", color = MaterialTheme.colorScheme.primary) },
            text = {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Parolni kiriting") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (password == "250795") {
                        showPasswordDialog = false
                        showApiKeyDialog = true
                    } else {
                        showPasswordDialog = false
                        clickCount = 0
                    }
                }) { Text("Kirish") }
            }
        )
    }

    if (showApiKeyDialog) {
        val settings by viewModel.settings.collectAsState()
        var apiKey by remember { mutableStateOf(settings?.apiKey ?: "") }
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false; clickCount = 0 },
            title = { Text("API Kalit (Google AI Studio)") },
            text = {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Gemini API Key") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val s = settings ?: com.helpteach.offline.data.Settings()
                    viewModel.saveSettings(s.copy(apiKey = apiKey.trim()))
                    showApiKeyDialog = false
                    clickCount = 0
                }) { Text("Saqlash") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Dastur haqida")
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
                    Text(
                        text = "Versiya: 1.0.0", 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable {
                            clickCount++
                            if (clickCount >= 3) {
                                showPasswordDialog = true
                            }
                        }
                    )
                }
                
                Divider()
                
                Text("Muallif: O.A.Asrorov \uD83D\uDC68\u200D\uD83D\uDCBB", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ContactRowDialog(icon = Icons.Filled.Phone, title = "Telefon", value = "+998 91 810 95 96") {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+998918109596")))
                    }
                    ContactRowDialog(icon = Icons.Filled.Email, title = "E-mail", value = "asrorovoybek@gmail.com") {
                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:asrorovoybek@gmail.com")))
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

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Foydalanish yo'riqnomasi")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HelpSection(
                    title = "1. Sozlamalar (birinchi qadam)",
                    content = "Profil \u2192 Sozlamalar \u2192 O'zgartirish tugmasini bosing. " +
                            "Ertalabki va kechki eslatma vaqtini kiriting. " +
                            "Kerakli eslatma turlarini yoqing (30 daqiqa oldin, 10 daqiqa oldin, dars boshlanishi). " +
                            "\"Saqlash\" tugmasini bosing. Sozlamalar saqlanmaguncha eslatmalar ishlamaydi."
                )

                HelpSection(
                    title = "2. Profil",
                    content = "Profil \u2192 Shaxsiy ma'lumotlar \u2192 Tahrirlash tugmasini bosing. " +
                            "Ism, familiya va ta'lim muassasangiz nomini kiriting va saqlang."
                )

                HelpSection(
                    title = "3. Dars jadvali",
                    content = "Jadval menyusiga o'ting. Hafta kunini tanlang. " +
                            "\"+\" tugmasini bosib yangi dars qo'shing: fan nomi, xona, guruh, " +
                            "boshlanish va tugash vaqtini kiriting. " +
                            "Dars turini (ma'ruza, amaliyot, lab) va hafta turini (har hafta, toq, juft) tanlang. " +
                            "Darsni o'chirish uchun dars kartochkasini bosing."
                )

                HelpSection(
                    title = "4. Vazifalar",
                    content = "Vazifalar menyusiga o'ting. \"+\" tugmasini bosib yangi vazifa qo'shing. " +
                            "Vazifa nomi, izoh (ixtiyoriy) va muddatini kiriting. " +
                            "Eslatma vaqtini kiritsangiz, belgilangan soatda signal chaladi. " +
                            "Vazifani bajarilgan deb belgilash uchun checkbox'ni bosing."
                )

                HelpSection(
                    title = "5. Ob-havo",
                    content = "Ob-havo menyusiga o'ting. " +
                            "\"Internetda ko'rish\" tugmasi orqali Google'da ob-havoni ko'rishingiz " +
                            "yoki \"Telegram bot orqali\" tugmasi bilan botdan foydalanishingiz mumkin. " +
                            "Shaharni Profil \u2192 Sozlamalar'dan o'zgartirishingiz mumkin."
                )

                HelpSection(
                    title = "6. Eslatmalar va ovozli xabarlar",
                    content = "Sozlamalar saqlanganidan keyin ilova avtomatik ishlaydi: " +
                            "darsdan 30 va 10 daqiqa oldin bildirishnoma keladi, " +
                            "dars boshlanishi bilan ovozli xabar eshitiladi. " +
                            "Ertalab kundalik xulosa, kechqurun bajarilgan vazifalar haqida xabar olasiz. " +
                            "Telefon o'chib yonsa ham eslatmalar qayta tiklanadi."
                )

                HelpSection(
                    title = "7. Bugungi reja",
                    content = "Bugun menyusida bugungi kun uchun rejalashtirilgan darslar " +
                            "va bajarilmagan vazifalar ro'yxati ko'rsatiladi. " +
                            "Toq yoki juft hafta ekanligi ham ko'rsatiladi."
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("Tushundim")
            }
        }
    )
}

@Composable
private fun HelpSection(title: String, content: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
