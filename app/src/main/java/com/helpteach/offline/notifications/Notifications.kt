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

        // Ovozli fayl tanlash
        val audioResId = when (action) {
            "ACTION_LESSON_REMINDER" -> com.helpteach.offline.R.raw.lesson_reminder
            "ACTION_LESSON_STARTED" -> com.helpteach.offline.R.raw.lesson_started
            "ACTION_CUSTOM_REMINDER" -> com.helpteach.offline.R.raw.task_reminder
            "ACTION_MORNING_SUMMARY" -> com.helpteach.offline.R.raw.morning_summary
            "ACTION_EVENING_SUMMARY" -> com.helpteach.offline.R.raw.evening_summary
            else -> null
        }

        // Bildirishnoma ko'rsatish
        when (action) {
            "ACTION_LESSON_REMINDER" -> NotificationHelper.showNotification(context, id, title, message)
            "ACTION_LESSON_STARTED" -> NotificationHelper.showNotification(context, id, title, message)
            "ACTION_MORNING_SUMMARY" -> handleMorningSummary(context)
            "ACTION_EVENING_SUMMARY" -> handleEveningSummary(context)
            "ACTION_CUSTOM_REMINDER" -> NotificationHelper.showNotification(context, id, title, message)
        }

        // Ovozli xabarni chalish
        if (audioResId != null) {
            CoroutineScope(Dispatchers.IO).launch {
                // Bildirishnoma ovozi tugashini kutamiz
                delay(2500)

                try {
                    val mediaPlayer = MediaPlayer.create(context.applicationContext, audioResId)
                    if (mediaPlayer != null) {
                        mediaPlayer.setOnCompletionListener { mp ->
                            mp.release()
                            pendingResult.finish()
                        }
                        mediaPlayer.start()
                    } else {
                        pendingResult.finish()
                    }
                } catch (e: Exception) {
                    pendingResult.finish()
                }
            }
        } else {
            pendingResult.finish()
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

            val lessons = db.lessonDao().getLessonsByDay(dayOfWeek).firstOrNull() ?: emptyList()
            val validLessons = lessons.filter { l ->
                l.weekType == "every" || (l.weekType == "odd" && isOddWeek) || (l.weekType == "even" && !isOddWeek)
            }
            val pendingTasks = db.taskDao().getPendingTasks().firstOrNull() ?: emptyList()

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
            // Reschedule for tomorrow
            NotificationHelper.scheduleDailySummary(context, "morning")
        }
    }

    private fun handleEveningSummary(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val pendingTasks = db.taskDao().getPendingTasks().firstOrNull() ?: emptyList()
            val completedTasks = db.taskDao().getCompletedTasks().firstOrNull() ?: emptyList()

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
            if (settings?.doNotDisturb == true) return@launch

            val timeStr = if (type == "morning") settings?.morningTime ?: "07:00" else settings?.eveningTime ?: "21:00"
            val parts = timeStr.split(":")
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

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = if (type == "morning") "ACTION_MORNING_SUMMARY" else "ACTION_EVENING_SUMMARY"
            }
            val reqCode = if (type == "morning") 8881 else 8882
            val pendingIntent = PendingIntent.getBroadcast(
                context, reqCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            } catch (e: SecurityException) {
                // permission denied
            }
        }
    }

    fun scheduleTaskAlarm(context: Context, title: String, timeHHmm: String) {
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
            putExtra("id", title.hashCode())
            putExtra("title", "🔔 Vazifa eslatmasi!")
            putExtra("message", title)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, title.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
        } catch (e: SecurityException) { }
    }

    fun scheduleLessonAlarmsForToday(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val settings = db.settingsDao().getSettings().firstOrNull()
            if (settings?.doNotDisturb == true) return@launch

            val cal = Calendar.getInstance()
            var dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 2
            if (dayOfWeek < 0) dayOfWeek = 6
            val isOddWeek = cal.get(Calendar.WEEK_OF_YEAR) % 2 != 0

            val lessons = db.lessonDao().getLessonsByDay(dayOfWeek).firstOrNull() ?: emptyList()
            for (lesson in lessons) {
                if (lesson.weekType == "odd" && !isOddWeek) continue
                if (lesson.weekType == "even" && isOddWeek) continue
                
                val parts = lesson.startTime.split(":")
                if (parts.size != 2) continue
                val hour = parts[0].toIntOrNull() ?: continue
                val min = parts[1].toIntOrNull() ?: continue

                // 30 mins before
                if (settings?.notifyBefore30 == true) {
                    setAlarm(context, lesson, hour, min, -30, "📅 30 daqiqadan dars boshlanadi!")
                }
                // 10 mins before
                if (settings?.notifyBefore10 == true) {
                    setAlarm(context, lesson, hour, min, -10, "⚡️ 10 daqiqa qoldi!")
                }
                // On time
                if (settings?.notifyOnTime == true) {
                    setAlarm(context, lesson, hour, min, 0, "🔴 DARS BOSHLANDI!", "ACTION_LESSON_STARTED")
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

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = actionType
            putExtra("id", lesson.id * 100 + Math.abs(offsetMins))
            putExtra("title", title)
            putExtra("message", "📚 Fan: ${lesson.subject}\n🏛 Xona: ${lesson.room}\n👥 Guruh: ${lesson.groupName}")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, lesson.id * 100 + Math.abs(offsetMins), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
        } catch (e: SecurityException) {
            // Permission denied
        }
    }
}
