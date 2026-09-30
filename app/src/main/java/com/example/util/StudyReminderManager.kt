package com.example.util

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.StudyGuardApp
import com.example.receiver.SmartReminderReceiver
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

data class SmartReminderItem(
    val id: String,
    val title: String,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean,
    val customMessage: String,
    val iconTag: String = "study" // "morning", "afternoon", "evening", "hydrate", "study", "target"
) {
    val formattedTime: String
        get() {
            val period = if (hour >= 12) "PM" else "AM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            return String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, period)
        }
}

class StudyReminderManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("study_smart_reminders_prefs", Context.MODE_PRIVATE)

    companion object {
        const val ACTION_REMINDER_FIRED = "com.example.action.SMART_REMINDER_FIRED"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_REMINDER_TITLE = "extra_reminder_title"
        const val EXTRA_REMINDER_MESSAGE = "extra_reminder_message"
        const val NOTIFICATION_ID_BASE = 8000
    }

    private val defaultReminders = listOf(
        SmartReminderItem(
            id = "rem_morning",
            title = "Morning Deep Work Kickoff ☀️",
            hour = 9,
            minute = 0,
            isEnabled = true,
            customMessage = "Start your day with high-focus studying before distractions arrive!",
            iconTag = "morning"
        ),
        SmartReminderItem(
            id = "rem_afternoon",
            title = "Afternoon Focus Sprint ⚡",
            hour = 14,
            minute = 30,
            isEnabled = true,
            customMessage = "Power through the afternoon with a 45-minute distraction-free block.",
            iconTag = "afternoon"
        ),
        SmartReminderItem(
            id = "rem_evening",
            title = "Evening Goal Revision 📚",
            hour = 19,
            minute = 30,
            isEnabled = true,
            customMessage = "Secure today's study streak! Review key notes and wrap up your topics.",
            iconTag = "evening"
        ),
        SmartReminderItem(
            id = "rem_hydrate",
            title = "Hydration & Posture Check 💧",
            hour = 11,
            minute = 30,
            isEnabled = true,
            customMessage = "Take a sip of water, stretch your neck, and rest your eyes for 20 seconds.",
            iconTag = "hydrate"
        )
    )

    fun getReminders(): List<SmartReminderItem> {
        val json = prefs.getString("reminders_json", null) ?: return defaultReminders
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<SmartReminderItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SmartReminderItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        hour = obj.getInt("hour"),
                        minute = obj.getInt("minute"),
                        isEnabled = obj.getBoolean("isEnabled"),
                        customMessage = obj.getString("customMessage"),
                        iconTag = obj.optString("iconTag", "study")
                    )
                )
            }
            if (list.isEmpty()) defaultReminders else list
        } catch (e: Exception) {
            defaultReminders
        }
    }

    fun saveReminders(reminders: List<SmartReminderItem>) {
        val arr = JSONArray()
        reminders.forEach { r ->
            val obj = JSONObject().apply {
                put("id", r.id)
                put("title", r.title)
                put("hour", r.hour)
                put("minute", r.minute)
                put("isEnabled", r.isEnabled)
                put("customMessage", r.customMessage)
                put("iconTag", r.iconTag)
            }
            arr.put(obj)
        }
        prefs.edit().putString("reminders_json", arr.toString()).apply()
        scheduleAllAlarms(reminders)
    }

    fun addReminder(
        title: String,
        hour: Int,
        minute: Int,
        message: String,
        iconTag: String
    ): SmartReminderItem {
        val item = SmartReminderItem(
            id = "rem_${System.currentTimeMillis()}",
            title = title.ifBlank { "Study Session Nudge" },
            hour = hour,
            minute = minute,
            isEnabled = true,
            customMessage = message.ifBlank { "Time to put away distractions and focus on your goals!" },
            iconTag = iconTag
        )
        val current = getReminders().toMutableList().apply { add(item) }
        saveReminders(current)
        return item
    }

    fun deleteReminder(id: String) {
        val current = getReminders().filterNot { it.id == id }
        saveReminders(current)
    }

    fun toggleReminder(id: String, isEnabled: Boolean) {
        val current = getReminders().map {
            if (it.id == id) it.copy(isEnabled = isEnabled) else it
        }
        saveReminders(current)
    }

    fun updateReminderDetails(
        id: String,
        title: String,
        hour: Int,
        minute: Int,
        message: String,
        iconTag: String
    ) {
        val current = getReminders().map {
            if (it.id == id) {
                it.copy(
                    title = title.ifBlank { it.title },
                    hour = hour,
                    minute = minute,
                    customMessage = message.ifBlank { it.customMessage },
                    iconTag = iconTag
                )
            } else it
        }
        saveReminders(current)
    }

    fun updateReminderTime(id: String, hour: Int, minute: Int) {
        val current = getReminders().map {
            if (it.id == id) it.copy(hour = hour, minute = minute) else it
        }
        saveReminders(current)
    }

    /**
     * Schedules exact daily recurring alarms for each enabled reminder using AlarmManager.
     */
    fun scheduleAllAlarms(reminders: List<SmartReminderItem> = getReminders()) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        reminders.forEach { reminder ->
            val intent = Intent(context, SmartReminderReceiver::class.java).apply {
                action = ACTION_REMINDER_FIRED
                putExtra(EXTRA_REMINDER_ID, reminder.id)
                putExtra(EXTRA_REMINDER_TITLE, reminder.title)
                putExtra(EXTRA_REMINDER_MESSAGE, reminder.customMessage)
            }

            val requestCode = (reminder.id.hashCode() and 0x7FFFFFFF) % 100000
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (reminder.isEnabled) {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, reminder.hour)
                    set(Calendar.MINUTE, reminder.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    // If time is already past for today, schedule for tomorrow
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                } catch (e: SecurityException) {
                    // Fallback to inexact alarm if exact alarm permission is restricted
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.cancel(pendingIntent)
            }
        }
    }

    fun sendTestReminder(reminder: SmartReminderItem) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "reminders")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (reminder.id.hashCode() and 0x7FFFFFFF),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, StudyGuardApp.CHANNEL_ID_SMART_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🔔 ${reminder.title}")
            .setContentText(reminder.customMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.customMessage))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = NOTIFICATION_ID_BASE + ((reminder.id.hashCode() and 0x7FFFFFFF) % 1000)
        manager.notify(notifId, notification)
    }
}
