package com.helpteach.offline.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.helpteach.offline.data.*
import com.helpteach.offline.network.WeatherResponse
import com.helpteach.offline.notifications.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)

    // Flows for UI
    val profile = db.profileDao().getProfile().stateIn(viewModelScope, SharingStarted.Lazily, null)
    val settings = db.settingsDao().getSettings().stateIn(viewModelScope, SharingStarted.Lazily, null)
    val allLessons = db.lessonDao().getAllLessons().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val pendingTasks = db.taskDao().getPendingTasks().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val completedTasks = db.taskDao().getCompletedTasks().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _weatherState = MutableStateFlow<WeatherState>(WeatherState.Idle)
    val weatherState: StateFlow<WeatherState> = _weatherState

    init {
        // Automatically schedule daily alarms on app launch
        NotificationHelper.createNotificationChannel(application)
        NotificationHelper.scheduleDailySummary(application, "morning")
        NotificationHelper.scheduleDailySummary(application, "evening")
        NotificationHelper.scheduleLessonAlarmsForToday(application)
    }

    // --- Profile & Settings ---
    fun saveProfile(profile: Profile) = viewModelScope.launch { db.profileDao().saveProfile(profile) }
    
    fun saveSettings(s: Settings) = viewModelScope.launch {
        db.settingsDao().saveSettings(s)
        // Reschedule alarms with new settings
        NotificationHelper.scheduleDailySummary(getApplication(), "morning")
        NotificationHelper.scheduleDailySummary(getApplication(), "evening")
        NotificationHelper.scheduleLessonAlarmsForToday(getApplication())
    }

    // --- Lessons ---
    fun addLesson(lesson: Lesson) = viewModelScope.launch {
        db.lessonDao().insertLesson(lesson)
        // Bazaga yozilishini kutamiz
        kotlinx.coroutines.delay(500)
        
        val currentSettings = settings.value
        
        // Gemini TTS orqali ovozli xabar generatsiya qilish (fon rejimda)
        if (currentSettings != null && currentSettings.apiKey.isNotBlank()) {
            launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val text = com.helpteach.offline.network.GeminiTTS.buildLessonText(
                        lesson.subject, lesson.lessonType, lesson.startTime, lesson.room
                    )
                    val filename = com.helpteach.offline.network.GeminiTTS.lessonAudioFilename(
                        lesson.dayOfWeek, lesson.startTime, lesson.subject
                    )
                    com.helpteach.offline.network.GeminiTTS.generateAndSave(
                        getApplication(), text, filename, currentSettings.apiKey
                    )
                } catch (_: Exception) { }
            }
        }
        
        NotificationHelper.scheduleLessonAlarmsForToday(getApplication())
    }

    fun deleteLesson(lesson: Lesson) = viewModelScope.launch {
        db.lessonDao().deleteLesson(lesson)
        // Dars o'chirilganda, unga tegishli audio faylni ham o'chiramiz (axlat qoldirmaslik uchun)
        launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val filename = com.helpteach.offline.network.GeminiTTS.lessonAudioFilename(
                    lesson.dayOfWeek, lesson.startTime, lesson.subject
                )
                val file = com.helpteach.offline.network.GeminiTTS.getAudioFile(getApplication(), filename)
                if (file != null && file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) { }
        }
    }

    // --- Tasks ---
    fun addTask(task: Task) = viewModelScope.launch { db.taskDao().insertTask(task) }
    
    fun addTaskWithReminder(task: Task, time: String) = viewModelScope.launch {
        db.taskDao().insertTask(task)
        if (time.isNotBlank()) {
            NotificationHelper.scheduleTaskAlarm(getApplication(), task.title, time)
        }
    }
    
    fun toggleTaskComplete(task: Task) = viewModelScope.launch {
        db.taskDao().insertTask(task.copy(isDone = !task.isDone))
    }
    
    fun deleteTask(task: Task) = viewModelScope.launch { db.taskDao().deleteTask(task) }

    // --- Weather ---
    fun fetchWeather(city: String) {
        _weatherState.value = WeatherState.Loading
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val response = com.helpteach.offline.network.NativeWeatherFetcher.fetchWeatherSync(city)
                _weatherState.value = WeatherState.Success(response)
            } catch (e: Throwable) {
                _weatherState.value = WeatherState.Error(e.message ?: "Noma'lum xatolik")
            }
        }
    }
}

sealed class WeatherState {
    object Idle : WeatherState()
    object Loading : WeatherState()
    data class Success(val data: WeatherResponse) : WeatherState()
    data class Error(val message: String) : WeatherState()
}
