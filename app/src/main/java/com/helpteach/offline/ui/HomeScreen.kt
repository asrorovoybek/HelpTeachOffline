package com.helpteach.offline.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.helpteach.offline.data.Lesson
import com.helpteach.offline.data.Task
import com.helpteach.offline.viewmodel.AppViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: AppViewModel) {
    val settings by viewModel.settings.collectAsState()
    val allLessons by viewModel.allLessons.collectAsState()
    val pendingTasks by viewModel.pendingTasks.collectAsState()

    val cal = Calendar.getInstance()
    var todayIndex = cal.get(Calendar.DAY_OF_WEEK) - 2
    if (todayIndex < 0) todayIndex = 6
    val currentWeekOfYear = cal.get(Calendar.WEEK_OF_YEAR)
    val isOddWeek = settings?.isOddWeek(currentWeekOfYear) ?: (currentWeekOfYear % 2 != 0)
    val weekTypeStr = if (isOddWeek) "Toq hafta" else "Juft hafta"

    val todayLessons = allLessons.filter { l ->
        l.dayOfWeek == todayIndex && 
        (l.weekType == "every" || (l.weekType == "odd" && isOddWeek) || (l.weekType == "even" && !isOddWeek))
    }.sortedBy { it.startTime }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bugungi darslar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    OutlinedButton(
                        onClick = { viewModel.setWeekType(!isOddWeek) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(text = weekTypeStr, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (todayLessons.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        PaddingValues(16.dp)
                        Text(
                            text = "Bugun darsingiz yo'q! Dam oling \uD83C\uDF89",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(todayLessons) { lesson ->
                    LessonCard(lesson = lesson, onLongClick = {})
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Vazifalar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (pendingTasks.isEmpty()) {
                item {
                    Text("Bajarilmagan vazifalar yo'q. Qoyil! \uD83D\uDE0E")
                }
            } else {
                items(pendingTasks) { task ->
                    TaskCard(
                        task = task,
                        onToggle = { viewModel.toggleTaskComplete(task) }
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonCard(lesson: Lesson, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = onLongClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${lesson.startTime} - ${lesson.endTime}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = lessonTypeToUzbek(lesson.lessonType),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = lesson.subject, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Xona: ${lesson.room} | Guruh: ${lesson.groupName}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(task: Task, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (task.isDone) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                contentDescription = "Toggle task",
                tint = if (task.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (!task.dueDate.isNullOrBlank()) {
                    Text(
                        text = "Muddat: ${task.dueDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

fun lessonTypeToUzbek(type: String): String {
    return when (type.lowercase()) {
        "lecture" -> "Ma'ruza"
        "practical" -> "Amaliyot"
        "lab" -> "Laboratoriya"
        "seminar" -> "Seminar"
        "course" -> "Kurs ishi"
        "other" -> "Boshqa"
        else -> type
    }
}
