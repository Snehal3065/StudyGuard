package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.local.StudyDatabase
import com.example.data.repository.StudyRepository

class StudyGuardApp : Application() {

    lateinit var repository: StudyRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        val database = StudyDatabase.getInstance(this)
        repository = StudyRepository(database.studyDao(), this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_STUDY_TIMER,
                "Study Focus Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active study time countdown and protection status"
                setShowBadge(false)
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ID_DISTRACTION_ALERT,
                "Distraction Interceptions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when a distraction app or YouTube Short is blocked"
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_ID_SMART_REMINDERS,
                "Smart Study Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Sends scheduled study nudges, hydration breaks, and daily targets"
                enableVibration(true)
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            manager.createNotificationChannel(alertChannel)
            manager.createNotificationChannel(reminderChannel)
        }
    }

    companion object {
        const val CHANNEL_ID_STUDY_TIMER = "study_guard_timer_channel"
        const val CHANNEL_ID_DISTRACTION_ALERT = "study_guard_distraction_alert"
        const val CHANNEL_ID_SMART_REMINDERS = "study_guard_smart_reminders"

        lateinit var instance: StudyGuardApp
            private set
    }
}
