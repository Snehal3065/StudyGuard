package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "distraction_events")
data class DistractionEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String,
    val appName: String,
    val eventType: String, // "APP_BLOCKED", "YOUTUBE_SHORTS_BLOCKED", "SETTINGS_TAMPER_PREVENTED"
    val reason: String = ""
)
