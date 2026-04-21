package com.helpteach.offline.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.helpteach.offline.data.Profile
import com.helpteach.offline.data.Settings
import com.helpteach.offline.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: AppViewModel) {
    val profile by viewModel.profile.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var isEditingProfile by remember { mutableStateOf(false) }
    var isEditingSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil va Sozlamalar") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Profil", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = { isEditingProfile = true }) { Text("Tahrirlash") }
                    }
                    Divider()
                    if (profile == null) {
                        Text("Profil to'ldirilmagan. Iltimos, tahrirlang.")
                    } else {
                        val p = profile!!
                        Text("Ism: ${p.fullName}", style = MaterialTheme.typography.bodyLarge)
                        Text("Rol: ${if (p.role == "teacher") "O'qituvchi" else if (p.role == "student") "Talaba" else "Boshqa"}", style = MaterialTheme.typography.bodyLarge)
                        Text("Muassasa: ${p.organization}", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            // Settings Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sozlamalar", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = { isEditingSettings = true }) { Text("Tahrirlash") }
                    }
                    Divider()
                    if (settings == null) {
                        Text("Standart sozlamalar o'rnatilmoqda...")
                    } else {
                        val s = settings!!
                        Text("Ertalabki xabar: ${s.morningTime}")
                        Text("Kechki xulosa: ${s.eveningTime}")
                        Text("Ob-havo shahri: ${s.weatherCity}")
                        Text("Darsdan 30 daqiqa oldin eslatma: ${if (s.notifyBefore30) "Yoqilgan" else "O'chirilgan"}")
                        Text("Darsdan 10 daqiqa oldin eslatma: ${if (s.notifyBefore10) "Yoqilgan" else "O'chirilgan"}")
                        Text("Dars boshlanganda eslatma: ${if (s.notifyOnTime) "Yoqilgan" else "O'chirilgan"}")
                        Text("Bezovta qilma rejimi: ${if (s.doNotDisturb) "Yoqilgan" else "O'chirilgan"}")
                    }
                }
            }

            // About App Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Dastur haqida", style = MaterialTheme.typography.titleLarge)
                    Divider()
                    Text("Muallif: Asrorov Oybek", style = MaterialTheme.typography.bodyLarge)
                    Text("Telefon: +998918109596", style = MaterialTheme.typography.bodyLarge)
                    Text("E-mail: AsrorovOybek@gmail.com", style = MaterialTheme.typography.bodyLarge)
                    Text("Telegram: https://t.me/Asrorov_Oybek", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }

    if (isEditingProfile) {
        EditProfileDialog(
            currentProfile = profile,
            onDismiss = { isEditingProfile = false },
            onSave = { p ->
                viewModel.saveProfile(p)
                isEditingProfile = false
            }
        )
    }

    if (isEditingSettings) {
        val currentSettings = settings ?: Settings()
        EditSettingsDialog(
            currentSettings = currentSettings,
            onDismiss = { isEditingSettings = false },
            onSave = { s ->
                viewModel.saveSettings(s)
                isEditingSettings = false
            }
        )
    }
}

@Composable
fun EditProfileDialog(currentProfile: Profile?, onDismiss: () -> Unit, onSave: (Profile) -> Unit) {
    var fullName by remember { mutableStateOf(currentProfile?.fullName ?: "") }
    var role by remember { mutableStateOf(currentProfile?.role ?: "teacher") }
    var org by remember { mutableStateOf(currentProfile?.organization ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profilni Tahrirlash") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Ism Familiya") })
                Text("Rolni tanlang:")
                Row {
                    Button(onClick = { role = "teacher" }, modifier = Modifier.weight(1f).padding(2.dp)) { Text("O'qituvchi") }
                    Button(onClick = { role = "student" }, modifier = Modifier.weight(1f).padding(2.dp)) { Text("Talaba") }
                }
                OutlinedTextField(value = org, onValueChange = { org = it }, label = { Text("Muassasa") })
            }
        },
        confirmButton = {
            Button(onClick = { onSave(Profile(fullName = fullName, role = role, organization = org)) }) { Text("Saqlash") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Bekor qilish") }
        }
    )
}

@Composable
fun EditSettingsDialog(currentSettings: Settings, onDismiss: () -> Unit, onSave: (Settings) -> Unit) {
    var morningTime by remember { mutableStateOf(currentSettings.morningTime) }
    var eveningTime by remember { mutableStateOf(currentSettings.eveningTime) }
    var weatherCity by remember { mutableStateOf(currentSettings.weatherCity) }
    
    var notify30 by remember { mutableStateOf(currentSettings.notifyBefore30) }
    var notify10 by remember { mutableStateOf(currentSettings.notifyBefore10) }
    var notify0 by remember { mutableStateOf(currentSettings.notifyOnTime) }
    var dnd by remember { mutableStateOf(currentSettings.doNotDisturb) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sozlamalar") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(value = morningTime, onValueChange = { morningTime = it }, label = { Text("Ertalabki xabar (HH:mm)") })
                OutlinedTextField(value = eveningTime, onValueChange = { eveningTime = it }, label = { Text("Kechki xulosa (HH:mm)") })
                OutlinedTextField(value = weatherCity, onValueChange = { weatherCity = it }, label = { Text("Ob-havo shahri") })
                
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = notify30, onCheckedChange = { notify30 = it })
                    Text("30 daqiqa oldin eslatish")
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = notify10, onCheckedChange = { notify10 = it })
                    Text("10 daqiqa oldin eslatish")
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = notify0, onCheckedChange = { notify0 = it })
                    Text("Dars boshlanganda eslatish")
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Switch(checked = dnd, onCheckedChange = { dnd = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Bezovta qilma")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    Settings(
                        morningTime = morningTime,
                        eveningTime = eveningTime,
                        weatherCity = weatherCity,
                        notifyBefore30 = notify30,
                        notifyBefore10 = notify10,
                        notifyOnTime = notify0,
                        doNotDisturb = dnd
                    )
                )
            }) { Text("Saqlash") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Bekor qilish") }
        }
    )
}
