package com.helpteach.offline.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.helpteach.offline.data.AppDatabase
import com.helpteach.offline.data.Lesson
import com.helpteach.offline.notifications.NotificationHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class LessonViewModel(application: Application) : AndroidViewModel(application) {

    private val lessonDao = AppDatabase.getDatabase(application).lessonDao()

    val allLessons: StateFlow<List<Lesson>> = lessonDao.getAllLessons()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Get today's day of week (1=Monday ... 7=Sunday)
    private val today: Int
        get() {
            val calendar = Calendar.getInstance()
            val day = calendar.get(Calendar.DAY_OF_WEEK)
            return if (day == Calendar.SUNDAY) 7 else day - 1
        }

    val todayLessons: StateFlow<List<Lesson>> = lessonDao.getLessonsByDay(today)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun insertLesson(lesson: Lesson) {
        viewModelScope.launch {
            val id = lessonDao.insertLesson(lesson)
            val savedLesson = lesson.copy(id = id.toInt())
            // Schedule Alarm
            NotificationHelper.scheduleLessonAlarm(getApplication(), savedLesson)
        }
    }

    fun deleteLesson(lesson: Lesson) {
        viewModelScope.launch {
            lessonDao.deleteLesson(lesson)
            // Cancel Alarm
            NotificationHelper.cancelLessonAlarm(getApplication(), lesson.id)
        }
    }
}
