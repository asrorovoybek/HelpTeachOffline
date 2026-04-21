package com.helpteach.offline.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.helpteach.offline.R
import com.helpteach.offline.data.Lesson
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val lessonTitle = intent.getStringExtra("LESSON_TITLE") ?: "Dars boshlanadi"
        val lessonTime = intent.getStringExtra("LESSON_TIME") ?: ""

        showNotification(context, lessonTitle, lessonTime)
    }

    private fun showNotification(context: Context, title: String, time: String) {
        val channelId = "helpteach_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "HelpTeach Eslatmalar",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Darslar boshlanishidan oldin eslatmalar"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // using standard android icon for simplicity
            .setContentTitle("Eslatma: $title")
            .setContentText("Dars 5 daqiqadan so'ng, $time da boshlanadi.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

object NotificationHelper {

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleLessonAlarm(context: Context, lesson: Lesson) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("LESSON_TITLE", lesson.title)
            putExtra("LESSON_TIME", lesson.startTime)
            putExtra("LESSON_ID", lesson.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            lesson.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Parse time (HH:mm)
        val timeParts = lesson.startTime.split(":")
        if (timeParts.size != 2) return

        val hour = timeParts[0].toIntOrNull() ?: return
        val minute = timeParts[1].toIntOrNull() ?: return

        // Setup calendar for the next occurrence of this lesson
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            
            // Adjust day of week (Calendar.SUNDAY = 1, ... Calendar.SATURDAY = 7)
            // Our dayOfWeek: 1 = Monday ... 7 = Sunday
            val calendarDayOfWeek = if (lesson.dayOfWeek == 7) Calendar.SUNDAY else lesson.dayOfWeek + 1
            set(Calendar.DAY_OF_WEEK, calendarDayOfWeek)
            
            // Subtract 5 minutes
            add(Calendar.MINUTE, -5)

            // If time is in the past, move to next week
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelLessonAlarm(context: Context, lessonId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            lessonId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
