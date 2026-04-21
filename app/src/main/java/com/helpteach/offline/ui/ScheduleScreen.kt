package com.helpteach.offline.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.helpteach.offline.viewmodel.LessonViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(viewModel: LessonViewModel) {
    val allLessons by viewModel.allLessons.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dars Jadvali") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Dars qo'shish")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Group by dayOfWeek
            val grouped = allLessons.groupBy { it.dayOfWeek }.toSortedMap()
            val days = listOf("Dushanba", "Seshanba", "Chorshanba", "Payshanba", "Juma", "Shanba", "Yakshanba")

            grouped.forEach { (day, lessons) ->
                item {
                    Text(
                        text = days.getOrElse(day - 1) { "Noma'lum" },
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(lessons) { lesson ->
                    LessonCard(lesson = lesson, onDelete = { viewModel.deleteLesson(it) })
                }
            }
        }

        if (showAddDialog) {
            AddLessonDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { lesson ->
                    viewModel.insertLesson(lesson)
                    showAddDialog = false
                }
            )
        }
    }
}
