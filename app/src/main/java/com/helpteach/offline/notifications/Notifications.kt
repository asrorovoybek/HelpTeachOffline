package com.helpteach.offline.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.helpteach.offline.MainActivity
import com.helpteach.offline.R
import com.helpteach.offline.data.AppDatabase
import com.helpteach.offline.data.Lesson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

import android.media.MediaPlayer

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val action = intent.action
        val id = intent.getIntExtra("id", 0)
        val title = intent.getStringExtra("title") ?: "Eslatma"
        val message = intent.getStringExtra("message") ?: ""

        // Standart ovozli fayl tanlash (fallback)
        val fallbackAudioResId = when (action) {
            "ACTION_LESSON_REMINDER" -> com.helpteach.offline.R.raw.lesson_reminder
            "ACTION_LESSON_STARTED" -> com.helpteach.offline.R.raw.lesson_started
            "ACTION_LESSON_ENDED" -> com.helpteach.offline.R.raw.lesson_ended
            "ACTION_CUSTOM_REMINDER" -> com.helpteach.offline.R.raw.task_reminder
            "ACTION_MORNING_SUMMARY" -> com.helpteach.offline.R.raw.morning_summary
            "ACTION_EVENING_SUMMARY" -> com.helpteach.offline.R.raw.evening_summary
            else -> null
        }

        // Bildirishnoma ko'rsatish
        when (action) {
            "ACTION_LESSON_REMINDER" -> NotificationHelper.showNotification(context, id, title, message)
            "ACTION_LESSON_STARTED" -> NotificationHelper.showNotification(context, id, title, message)
            "ACTION_LESSON_ENDED" -> NotificationHelper.showNotification(context, id, title, message)
            "ACTION_MORNING_SUMMARY" -> handleMorningSummary(context)
            "ACTION_EVENING_SUMMARY" -> handleEveningSummary(context)
            "ACTION_CUSTOM_REMINDER" -> NotificationHelper.showNotification(context, id, title, message)
        }

        // Ovozli xabarni chalish
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Avval bazadan dars mavjudligini va hafta turiga mosligini tekshiramiz
                if (action == "ACTION_LESSON_REMINDER" || action == "ACTION_LESSON_STARTED" || action == "ACTION_LESSON_ENDED") {
                    val lessonId = id / 1000
                    val lesson = kotlinx.coroutines.withContext(Dispatchers.IO) {
                        AppDatabase.getDatabase(appContext).lessonDao().getLessonById(lessonId)
                    }
                    if (lesson == null) {
                        pendingResult.finish()
                        return@launch
                    }

                    // Hafta turi mosligini tekshirish
                    val settings = kotlinx.coroutines.withContext(Dispatchers.IO) {
                        AppDatabase.getDatabase(appContext).settingsDao().getSettings().firstOrNull()
                    }
                    if (settings != null) {
                        val cal = Calendar.getInstance()
                        val currentWeekOfYear = cal.get(Calendar.WEEK_OF_YEAR)
                        val isOddWeek = settings.isOddWeek(currentWeekOfYear)
                        
                        if (lesson.weekType == "odd" && !isOddWeek) {
                            pendingResult.finish()
                            return@launch
                        }
                        if (lesson.weekType == "even" && isOddWeek) {
                            pendingResult.finish()
                            return@launch
                        }
                    }
                } else if (action == "ACTION_CUSTOM_REMINDER") {
                    val taskTitle = intent.getStringExtra("message") ?: ""
                    if (taskTitle.isNotBlank()) {
                        val exists = kotlinx.coroutines.withContext(Dispatchers.IO) {
                            AppDatabase.getDatabase(appContext).taskDao().getTaskByTitle(taskTitle) != null
                        }
                        if (!exists) {
                            pendingResult.finish()
                            return@launch
                        }
                    }
                }

                // Bildirishnoma ovozi tugashini kutamiz
                delay(1500)

                // Avval shaxsiy audio faylni tekshirish
                val customAudioUriString = intent.getStringExtra("custom_audio_uri")
                val customAudioFile = if (customAudioUriString != null) {
                    java.io.File(customAudioUriString)
                } else null

                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .build()

                // Audio focus so'rash (Boshqa dasturlar musiqasini pasaytirish yoki to'xtatish uchun)
                val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val focusRequest = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                        .setAudioAttributes(audioAttributes)
                        .build()
                    audioManager.requestAudioFocus(focusRequest)
                } else {
                    audioManager.requestAudioFocus(null, android.media.AudioManager.STREAM_ALARM, android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                }

                var playedCustom = false
                if (customAudioFile != null && customAudioFile.exists() && customAudioFile.length() > 0) {
                    try {
                        val mediaPlayer = MediaPlayer()
                        mediaPlayer.setAudioAttributes(audioAttributes)
                        mediaPlayer.setDataSource(customAudioFile.absolutePath)
                        mediaPlayer.prepare()

                        // Maksimal 30 soniya chalish (uzun musiqalar to'xtatiladi)
                        val timeoutJob = launch {
                            delay(30000)
                            try {
                                if (mediaPlayer.isPlaying) {
                                    mediaPlayer.stop()
                                    mediaPlayer.release()
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                                        audioManager.abandonAudioFocus(null)
                                    }
                                    pendingResult.finish()
                                }
                            } catch (e: Exception) {}
                        }

                        mediaPlayer.setOnCompletionListener { mp ->
                            timeoutJob.cancel()
                            mp.release()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                // Focus handles itself
                            } else {
                                audioManager.abandonAudioFocus(null)
                            }
                            pendingResult.finish()
                        }
                        mediaPlayer.start()
                        playedCustom = true
                    } catch (e: Exception) {
                        playedCustom = false
                    }
                }

                if (!playedCustom && fallbackAudioResId != null) {
                    try {
                        val mediaPlayer = MediaPlayer()
                        mediaPlayer.setAudioAttributes(audioAttributes)
                        val afd = appContext.resources.openRawResourceFd(fallbackAudioResId)
                        if (afd != null) {
                            mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                            afd.close()
                            mediaPlayer.prepare()

                            // Maksimal 30 soniya (standart ovozlar uchun ham)
                            val timeoutJob = launch {
                                delay(30000)
                                try {
                                    if (mediaPlayer.isPlaying) {
                                        mediaPlayer.stop()
                                        mediaPlayer.release()
                                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                                            audioManager.abandonAudioFocus(null)
                                        }
                                        pendingResult.finish()
                                    }
                                } catch (e: Exception) {}
                            }

                            mediaPlayer.setOnCompletionListener { mp ->
                                timeoutJob.cancel()
                                mp.release()
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    // Focus handles itself
                                } else {
                                    audioManager.abandonAudioFocus(null)
                                }
                                pendingResult.finish()
                            }
                            mediaPlayer.start()
                        } else {
                            pendingResult.finish()
                        }
                    } catch (e: Exception) {
                        pendingResult.finish()
                    }
                } else if (!playedCustom) {
                    pendingResult.finish()
                }
            } catch (e: Exception) {
                pendingResult.finish()
            }
        }
    }

    private fun handleMorningSummary(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val cal = Calendar.getInstance()
            // In Java Calendar, Sunday=1, Monday=2. Our DB: Monday=0, Sunday=6
            var dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 2
            if (dayOfWeek < 0) dayOfWeek = 6
            
            val weekNumber = cal.get(Calendar.WEEK_OF_YEAR)
            val isOddWeek = weekNumber % 2 != 0

            val lessons = db.lessonDao().getLessonsByDaySync(dayOfWeek)
            val validLessons = lessons.filter { l ->
                l.weekType == "every" || (l.weekType == "odd" && isOddWeek) || (l.weekType == "even" && !isOddWeek)
            }
            val pendingTasks = db.taskDao().getPendingTasksSync()

            val msg = buildString {
                if (validLessons.isNotEmpty()) {
                    append("📚 Bugungi darslar: ${validLessons.size} ta\n")
                } else {
                    append("📚 Bugun dars yo'q!\n")
                }
                if (pendingTasks.isNotEmpty()) {
                    append("✅ Bajarilmagan vazifalar: ${pendingTasks.size} ta")
                }
            }
            
            NotificationHelper.showNotification(context, 9991, "🌅 Xayrli tong!", msg.trim())
            // Ertangi kun uchun qayta rejalashtirish
            NotificationHelper.scheduleDailySummary(context, "morning")
            // Bugungi darslar uchun alarmlarni avtomatik o'rnatish
            NotificationHelper.scheduleLessonAlarmsForToday(context)
        }
    }

    private fun handleEveningSummary(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val pendingTasks = db.taskDao().getPendingTasksSync()
            val completedTasks = db.taskDao().getCompletedTasksSync()

            val msg = buildString {
                append("✅ Bajarilgan vazifalar: ${completedTasks.size} ta\n")
                append("⏳ Kutayotgan vazifalar: ${pendingTasks.size} ta")
            }
            
            NotificationHelper.showNotification(context, 9992, "🌙 Kechki xulosa", msg.trim())
            // Reschedule for tomorrow
            NotificationHelper.scheduleDailySummary(context, "evening")
        }
    }
}

object NotificationHelper {
    private const val CHANNEL_ID = "helpteach_reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Dars va vazifalar eslatmalari",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Dars va vazifa eslatmalari"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                // Oddiy bildirishnoma ovozi (budilnik emas)
                setSound(
                    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION),
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, id: Int, title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, builder.build())
    }

    fun scheduleDailySummary(context: Context, type: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val settings = db.settingsDao().getSettings().firstOrNull()
            if (settings == null) return@launch // Sozlamalar hali saqlanmagan
            if (settings.doNotDisturb) return@launch

            val timeStr = if (type == "morning") settings.morningTime else settings.eveningTime
            val parts = timeStr.trim().split(":")
            if (parts.size != 2) return@launch
            
            val hour = parts[0].toIntOrNull() ?: return@launch
            val min = parts[1].toIntOrNull() ?: return@launch

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, min)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val reqCode = if (type == "morning") 8881 else 8882
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = if (type == "morning") "ACTION_MORNING_SUMMARY" else "ACTION_EVENING_SUMMARY"
                putExtra("id", reqCode)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, reqCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            setAlarmClockHelper(context, alarmManager, cal.timeInMillis, pendingIntent)
        }
    }

    private fun setAlarmClockHelper(context: Context, alarmManager: AlarmManager, timeInMillis: Long, pendingIntent: PendingIntent) {
        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context, 0, showIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val info = AlarmManager.AlarmClockInfo(timeInMillis, showPendingIntent)
        try {
            alarmManager.setAlarmClock(info, pendingIntent)
        } catch (e: SecurityException) {
            // Exact alarm permission might be missing on some devices or OS versions
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            } catch (e2: SecurityException) {}
        }
    }

    fun scheduleTaskAlarm(context: Context, task: com.helpteach.offline.data.Task, timeHHmm: String) {
        val parts = timeHHmm.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val min = parts[1].toIntOrNull() ?: return

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, min)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1) // default to tomorrow if time passed
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "ACTION_CUSTOM_REMINDER"
            putExtra("id", task.title.hashCode())
            putExtra("title", "🔔 Vazifa eslatmasi!")
            putExtra("message", task.title)
            if (!task.customAudioUri.isNullOrBlank()) {
                putExtra("custom_audio_uri", task.customAudioUri)
            }
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, task.title.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        setAlarmClockHelper(context, alarmManager, cal.timeInMillis, pendingIntent)
    }

    fun scheduleLessonAlarmsForToday(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val settings = db.settingsDao().getSettings().firstOrNull()
            if (settings == null) return@launch // Sozlamalar hali saqlanmagan
            if (settings.doNotDisturb) return@launch

            val cal = Calendar.getInstance()
            var dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 2
            if (dayOfWeek < 0) dayOfWeek = 6
            val currentWeekOfYear = cal.get(Calendar.WEEK_OF_YEAR)
            val isOddWeek = settings.isOddWeek(currentWeekOfYear)

            val lessons = db.lessonDao().getLessonsByDay(dayOfWeek).firstOrNull() ?: emptyList()
            for (lesson in lessons) {
                if (lesson.weekType == "odd" && !isOddWeek) continue
                if (lesson.weekType == "even" && isOddWeek) continue
                
                val parts = lesson.startTime.split(":")
                if (parts.size != 2) continue
                val hour = parts[0].toIntOrNull() ?: continue
                val min = parts[1].toIntOrNull() ?: continue

                // 30 mins before
                if (settings.notifyBefore30) {
                    setAlarm(context, lesson, hour, min, -30, "📅 30 daqiqadan dars boshlanadi!")
                }
                // 20 mins before
                if (settings.notifyBefore20) {
                    setAlarm(context, lesson, hour, min, -20, "⚡️ 20 daqiqa qoldi!")
                }
                // On time
                if (settings.notifyOnTime) {
                    setAlarm(context, lesson, hour, min, 0, "🔴 DARS BOSHLANDI!", "ACTION_LESSON_STARTED")
                }
                // On end
                if (settings.notifyOnEnd) {
                    val endParts = lesson.endTime.split(":")
                    if (endParts.size == 2) {
                        val endHour = endParts[0].toIntOrNull() ?: 0
                        val endMin = endParts[1].toIntOrNull() ?: 0
                        setAlarm(context, lesson, endHour, endMin, 0, "🏁 Dars tugadi!", "ACTION_LESSON_ENDED")
                    }
                }
            }
        }
    }

    private fun setAlarm(context: Context, lesson: Lesson, hour: Int, min: Int, offsetMins: Int, title: String, actionType: String = "ACTION_LESSON_REMINDER") {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, min)
            set(Calendar.SECOND, 0)
            add(Calendar.MINUTE, offsetMins)
        }
        
        if (cal.before(Calendar.getInstance())) return // already passed today

        val alarmId = when(actionType) {
            "ACTION_LESSON_ENDED" -> lesson.id * 1000 + 999
            else -> lesson.id * 1000 + Math.abs(offsetMins)
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = actionType
            putExtra("id", alarmId)
            putExtra("title", title)
            putExtra("message", "📚 Fan: ${lesson.subject}\n🏛 Xona: ${lesson.room}\n👥 Guruh: ${lesson.groupName}")
            if (actionType == "ACTION_LESSON_REMINDER" && !lesson.customAudioUri.isNullOrBlank()) {
                putExtra("custom_audio_uri", lesson.customAudioUri)
            }
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, alarmId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        setAlarmClockHelper(context, alarmManager, cal.timeInMillis, pendingIntent)
    }

    fun cancelLessonAlarms(context: Context, lesson: Lesson) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val offsets = listOf(-30, -10, 0)
        for (offset in offsets) {
            val actionType = if (offset == 0) "ACTION_LESSON_STARTED" else "ACTION_LESSON_REMINDER"
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = actionType
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, lesson.id * 100 + Math.abs(offset), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    fun cancelTaskAlarm(context: Context, task: com.helpteach.offline.data.Task) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "ACTION_CUSTOM_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, task.title.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
