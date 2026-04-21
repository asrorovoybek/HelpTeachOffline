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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.helpteach.offline.data.Lesson
import com.helpteach.offline.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(viewModel: AppViewModel) {
    val allLessons by viewModel.allLessons.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val days = listOf("Dushanba", "Seshanba", "Chorshanba", "Payshanba", "Juma", "Shanba", "Yakshanba")
    var selectedDay by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jadval") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, "Dars qo'shish")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedDay,
                edgePadding = 8.dp
            ) {
                days.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedDay == index,
                        onClick = { selectedDay = index },
                        text = { Text(title) }
                    )
                }
            }

            val dayLessons = allLessons.filter { it.dayOfWeek == selectedDay }

            if (dayLessons.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("Bu kunga dars kiritilmagan.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(dayLessons) { lesson ->
                        LessonCardWithDelete(lesson = lesson, onDelete = { viewModel.deleteLesson(lesson) })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddLessonDialog(
            dayOfWeek = selectedDay,
            onDismiss = { showAddDialog = false },
            onSave = { lesson ->
                viewModel.addLesson(lesson)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun LessonCardWithDelete(lesson: Lesson, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LessonCard(lesson = lesson, onLongClick = { showDeleteConfirm = true })

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("O'chirish") },
            text = { Text("Ushbu darsni rostdan ham o'chirmoqchimisiz?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("O'chirish", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Bekor qilish") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLessonDialog(dayOfWeek: Int, onDismiss: () -> Unit, onSave: (Lesson) -> Unit) {
    var subject by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("09:30") }
    
    val lessonTypes = listOf("lecture", "practical", "lab", "course", "seminar", "other")
    var lessonType by remember { mutableStateOf(lessonTypes[0]) }
    
    val weekTypes = listOf("every", "odd", "even")
    var weekType by remember { mutableStateOf(weekTypes[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yangi dars") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Fan nomi") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("Xona") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = groupName, onValueChange = { groupName = it }, label = { Text("Guruh") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = startTime, onValueChange = { startTime = it }, label = { Text("Boshlanish (HH:mm)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = endTime, onValueChange = { endTime = it }, label = { Text("Tugash (HH:mm)") }, modifier = Modifier.weight(1f))
                }
                Text("Dars turi: ${when(lessonType) { "lecture" -> "Ma'ruza"; "practical" -> "Amaliyot"; "lab" -> "Laboratoriya"; "course" -> "Kurs ishi"; "seminar" -> "Seminar"; else -> "Boshqa" }}", fontWeight = FontWeight.Bold)
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val types = listOf("lecture" to "Ma'ruza", "practical" to "Amaliyot", "lab" to "Lab", "seminar" to "Seminar")
                    items(types) { (key, label) ->
                        FilterChip(
                            selected = lessonType == key,
                            onClick = { lessonType = key },
                            label = { Text(label) }
                        )
                    }
                }

                Text("Hafta turi: ${if(weekType=="every") "Har hafta" else if(weekType=="odd") "Toq" else "Juft"}", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(selected = weekType == "every", onClick = { weekType = "every" }, label = { Text("Har") })
                    FilterChip(selected = weekType == "odd", onClick = { weekType = "odd" }, label = { Text("Toq") })
                    FilterChip(selected = weekType == "even", onClick = { weekType = "even" }, label = { Text("Juft") })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subject.isNotBlank()) {
                        onSave(Lesson(
                            subject = subject, room = room, groupName = groupName,
                            startTime = startTime, endTime = endTime, dayOfWeek = dayOfWeek,
                            lessonType = lessonType, weekType = weekType
                        ))
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
