package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val category: String, // "FOCUS", "SHIELD", "MUSIC", "DISCIPLINE"
    val targetValue: Int,
    val currentValue: Int = 0,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long = 0L,
    val iconKey: String = "trophy" // "trophy", "shield", "music", "fire", "crown", "star"
)
