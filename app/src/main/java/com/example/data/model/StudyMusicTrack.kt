package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_music_tracks")
data class StudyMusicTrack(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artistOrCategory: String = "Study Focus",
    val uriOrPath: String, // internal file path, content URI, or preset key
    val durationMs: Long = 0L,
    val fileSizeFormatted: String = "",
    val dateAdded: Long = System.currentTimeMillis(),
    val isPreset: Boolean = false,
    val isFavorite: Boolean = false
)
