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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: AppViewModel) {
    val pendingTasks by viewModel.pendingTasks.collectAsState()
    val completedTasks by viewModel.completedTasks.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vazifalar") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
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
            onSave = { task ->
                viewModel.addTask(task)
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
fun AddTaskDialog(onDismiss: () -> Unit, onSave: (Task) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yangi vazifa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Vazifa nomi") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Izoh (ixtiyoriy)") })
                OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, label = { Text("Muddat (YYYY-MM-DD)") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(Task(title = title, description = description.takeIf { it.isNotBlank() }, dueDate = dueDate.takeIf { it.isNotBlank() }))
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
