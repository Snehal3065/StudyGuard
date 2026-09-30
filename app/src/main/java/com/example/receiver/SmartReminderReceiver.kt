package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.StudyGuardApp
import com.example.util.StudyReminderManager

class SmartReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(StudyReminderManager.EXTRA_REMINDER_TITLE)
            ?: "StudyGuard Focus Reminder"
        val message = intent.getStringExtra(StudyReminderManager.EXTRA_REMINDER_MESSAGE)
            ?: "It's time for your focused study session! Beat distractions and stay locked in."
        val reminderId = intent.getStringExtra(StudyReminderManager.EXTRA_REMINDER_ID) ?: "general"

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "reminders")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, StudyGuardApp.CHANNEL_ID_SMART_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🔔 $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = StudyReminderManager.NOTIFICATION_ID_BASE + (reminderId.hashCode() % 1000)
        notificationManager.notify(notifId, notification)

        // Reschedule next recurrence for daily alarms
        val manager = StudyReminderManager(context)
        manager.scheduleAllAlarms()
    }
}
