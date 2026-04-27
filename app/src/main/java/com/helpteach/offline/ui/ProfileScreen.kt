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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Person
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: AppViewModel) {
    val profile by viewModel.profile.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    var isEditingProfile by remember { mutableStateOf(false) }
    var isEditingSettings by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Copy image to internal storage
            val inputStream = context.contentResolver.openInputStream(it)
            val file = File(context.filesDir, "profile_image.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            // Update profile with new image path
            profile?.let { p ->
                viewModel.saveProfile(p.copy(profileImageUri = file.absolutePath))
            }
        }
    }

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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Profile Image
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .border(2.dp, MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape)
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (profile?.profileImageUri != null) {
                                    AsyncImage(
                                        model = profile?.profileImageUri,
                                        contentDescription = "Profil rasmi",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Shaxsiy ma'lumotlar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { isEditingProfile = true }) { Text("Tahrirlash ✏️") }
                    }
                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    if (profile == null) {
                        Text("Profil to'ldirilmagan. Iltimos, ma'lumotlarni kiriting! 🚀", color = MaterialTheme.colorScheme.error)
                    } else {
                        val p = profile!!
                        ProfileRow("Ism familiya", p.fullName)
                        ProfileRow("Rol", when(p.role) {
                            "teacher" -> "O'qituvchi 👨‍🏫"
                            "student" -> "Talaba 🎓"
                            else -> "Umumiy foydalanuvchi 👤"
                        })
                        ProfileRow("Muassasa", p.organization)
                        if (p.role == "teacher" && !p.position.isNullOrBlank()) {
                            ProfileRow("Lavozim", p.position)
                        }
                        if (p.role == "student") {
                            if (!p.course.isNullOrBlank()) ProfileRow("Kurs", p.course)
                            if (!p.groupName.isNullOrBlank()) ProfileRow("Guruh", p.groupName)
                        }
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
                        Text("Sozlamalar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
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
                        ProfileRow("⚡️ 20 daqiqa oldin", if (s.notifyBefore20) "Yoqilgan ✅" else "O'chirilgan ❌")
                        ProfileRow("🔴 Dars vaqtida", if (s.notifyOnTime) "Yoqilgan ✅" else "O'chirilgan ❌")
                        ProfileRow("🏁 Dars tugaganda", if (s.notifyOnEnd) "Yoqilgan ✅" else "O'chirilgan ❌")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(currentProfile: Profile?, onDismiss: () -> Unit, onSave: (Profile) -> Unit) {
    var fullName by remember { mutableStateOf(currentProfile?.fullName ?: "") }
    var role by remember { mutableStateOf(currentProfile?.role ?: "teacher") }
    var org by remember { mutableStateOf(currentProfile?.organization ?: "") }
    var position by remember { mutableStateOf(currentProfile?.position ?: "") }
    var course by remember { mutableStateOf(currentProfile?.course ?: "") }
    var groupName by remember { mutableStateOf(currentProfile?.groupName ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profilni tahrirlash") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Ism familiya") }, modifier = Modifier.fillMaxWidth())
                
                Text("Rolni tanlang:", fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val roles = listOf("teacher" to "O'qituvchi", "student" to "Talaba", "general" to "Umumiy")
                    roles.forEach { (id, label) ->
                        Surface(
                            onClick = { role = id },
                            shape = RoundedCornerShape(12.dp),
                            color = if (role == id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(label, style = MaterialTheme.typography.labelLarge, color = if (role == id) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                OutlinedTextField(value = org, onValueChange = { org = it }, label = { Text("Muassasa (Maktab/OTM)") }, modifier = Modifier.fillMaxWidth())

                if (role == "teacher") {
                    Text("Lavozimni tanlang:", fontWeight = FontWeight.Bold)
                    val positions = listOf("Stajyor", "Assistent", "Katta o'qituvchi", "Dotsent", "PhD", "DSc", "Professor")
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val rows = positions.chunked(3)
                        rows.forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                row.forEach { pos ->
                                    FilterChip(
                                        selected = position == pos,
                                        onClick = { position = pos },
                                        label = { Text(pos, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (row.size < 3) Spacer(Modifier.weight(3f - row.size))
                            }
                        }
                    }
                }

                if (role == "student") {
                    Text("Kursni tanlang:", fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { c ->
                            FilterChip(
                                selected = course == "$c-kurs",
                                onClick = { course = "$c-kurs" },
                                label = { Text("$c") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = groupName, 
                        onValueChange = { groupName = it }, 
                        label = { Text("Guruh nomi va raqami") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                onSave(Profile(
                    fullName = fullName, 
                    role = role, 
                    organization = org,
                    position = if (role == "teacher") position else null,
                    course = if (role == "student") course else null,
                    groupName = if (role == "student") groupName else null,
                    profileImageUri = currentProfile?.profileImageUri // Preserve existing image
                )) 
            }) { Text("Saqlash") }
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
    var notify20 by remember { mutableStateOf(currentSettings.notifyBefore20) }
    var notify0 by remember { mutableStateOf(currentSettings.notifyOnTime) }
    var notifyEnd by remember { mutableStateOf(currentSettings.notifyOnEnd) }
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
                    Checkbox(checked = notify20, onCheckedChange = { notify20 = it })
                    Text("20 daqiqa oldin eslatish")
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = notify0, onCheckedChange = { notify0 = it })
                    Text("Dars boshlanganda eslatish")
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = notifyEnd, onCheckedChange = { notifyEnd = it })
                    Text("Dars tugaganda eslatish")
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
                        notifyBefore20 = notify20,
                        notifyOnTime = notify0,
                        notifyOnEnd = notifyEnd,
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
