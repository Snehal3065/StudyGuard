package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String,
    val plannedMinutes: Int,
    val actualMinutes: Int = 0,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = 0,
    val isCompleted: Boolean = false,
    val distractionsIntercepted: Int = 0,
    val notes: String = ""
)
