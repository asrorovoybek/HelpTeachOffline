package com.helpteach.offline.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Telefon yonganda bildirishnomalarni qayta rejalashtirish
            NotificationHelper.createNotificationChannel(context)
            NotificationHelper.scheduleDailySummary(context, "morning")
            NotificationHelper.scheduleDailySummary(context, "evening")
            NotificationHelper.scheduleLessonAlarmsForToday(context)
        }
    }
}
