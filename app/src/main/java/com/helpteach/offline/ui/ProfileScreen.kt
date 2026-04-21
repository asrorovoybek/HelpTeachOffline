package com.helpteach.offline.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.helpteach.offline.data.Profile
import com.helpteach.offline.data.Settings
import com.helpteach.offline.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: AppViewModel) {
    val profile by viewModel.profile.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    var isEditingProfile by remember { mutableStateOf(false) }
    var isEditingSettings by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("👤 Shaxsiy ma'lumotlar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { isEditingProfile = true }) { Text("Tahrirlash ✏️") }
                    }
                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    if (profile == null) {
                        Text("Profil to'ldirilmagan. Iltimos, ma'lumotlarni kiriting! 🚀", color = MaterialTheme.colorScheme.error)
                    } else {
                        val p = profile!!
                        ProfileRow("Ism-sharif", p.fullName)
                        ProfileRow("Daraja", if (p.role == "teacher") "O'qituvchi 👨‍🏫" else if (p.role == "student") "Talaba 🎓" else "Boshqa 👤")
                        ProfileRow("Muassasa", p.organization)
                    }
                }
            }

            // Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("⚙️ Sozlamalar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        TextButton(onClick = { isEditingSettings = true }) { Text("O'zgartirish 🛠") }
                    }
                    Divider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
                    if (settings == null) {
                        Text("Standart sozlamalar o'rnatilmoqda... ⏳")
                    } else {
                        val s = settings!!
                        ProfileRow("🌅 Ertalabki xabar", s.morningTime)
                        ProfileRow("🌙 Kechki xulosa", s.eveningTime)
                        ProfileRow("🏙 Ob-havo shahri", s.weatherCity)
                        ProfileRow("⏰ 30 daqiqa oldin", if (s.notifyBefore30) "Yoqilgan ✅" else "O'chirilgan ❌")
                        ProfileRow("⚡️ 10 daqiqa oldin", if (s.notifyBefore10) "Yoqilgan ✅" else "O'chirilgan ❌")
                        ProfileRow("🔴 Dars vaqtida", if (s.notifyOnTime) "Yoqilgan ✅" else "O'chirilgan ❌")
                        ProfileRow("🔕 Bezovta qilma", if (s.doNotDisturb) "Faol 🔕" else "O'chirilgan 🔔")
                    }
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
fun ProfileRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ContactRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
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
