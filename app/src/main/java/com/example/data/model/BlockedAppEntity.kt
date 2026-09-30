package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isDistraction: Boolean = true, // true = blocked during study session; false = allowed study tool
    val category: String = "Distraction", // "Social", "Entertainment", "Games", "Study Tool", "Other"
    val isProtectedSystemApp: Boolean = false
)
