package com.helpteach.offline.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.helpteach.offline.data.Task
import com.helpteach.offline.viewmodel.AppViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.helpteach.offline.utils.AudioHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: AppViewModel) {
    val pendingTasks by viewModel.pendingTasks.collectAsState()
    val completedTasks by viewModel.completedTasks.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, "Vazifa qo'shish")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Kutayotgan (${pendingTasks.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Bajarilgan (${completedTasks.size})") }
                )
            }

            val listToShow = if (selectedTab == 0) pendingTasks else completedTasks

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (listToShow.isEmpty()) {
                    item {
                        Text(
                            text = if (selectedTab == 0) "Bajarilmagan vazifalar yo'q." else "Bajarilgan vazifalar yo'q.",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(listToShow) { task ->
                        TaskCardWithDelete(
                            task = task,
                            onToggle = { viewModel.toggleTaskComplete(task) },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onSave = { task, remindTime ->
                viewModel.addTaskWithReminder(task, remindTime)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun TaskCardWithDelete(task: Task, onToggle: () -> Unit, onDelete: () -> Unit) {
    var showDelete by remember { mutableStateOf(false) }
    
    // Wrap in a layout with a delete button at the end
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                TaskCard(task = task, onToggle = onToggle)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "O'chirish", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onSave: (Task, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var remindTime by remember { mutableStateOf("") }

    val context = LocalContext.current
    var customAudioUri by remember { mutableStateOf<Uri?>(null) }
    val audioPicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        customAudioUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(dismissOnClickOutside = false),
        title = { Text("Yangi vazifa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Vazifa nomi") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Izoh (ixtiyoriy)") })
                OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, label = { Text("Sana (DD.MM.YYYY) ixtiyoriy") })
                OutlinedTextField(value = remindTime, onValueChange = { remindTime = it }, label = { Text("Bong urish vaqti (HH:mm) ixtiyoriy") })
                Text("Agar vaqt kiritsangiz, shu soatda signal chalib eslatadi 🔔", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                
                // Audio Selection
                if (customAudioUri == null) {
                    OutlinedButton(
                        onClick = { audioPicker.launch("audio/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🎵 Shaxsiy audio biriktirish (ixtiyoriy)")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎵 Tanlandi: Audio Fayl",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { customAudioUri = null }) {
                            Icon(Icons.Filled.Delete, contentDescription = "O'chirish", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            val context = LocalContext.current
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        var internalAudioPath: String? = null
                        if (customAudioUri != null) {
                            internalAudioPath = AudioHelper.copyAudioToInternal(context, customAudioUri!!)
                        }
                        onSave(
                            Task(
                                title = title, 
                                description = description.takeIf { it.isNotBlank() }, 
                                dueDate = dueDate.takeIf { it.isNotBlank() },
                                customAudioUri = internalAudioPath
                            ), 
                            remindTime
                        )
                    }
                }
            ) {
                Text("Saqlash")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Bekor qilish") }
        }
    )
}
