package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.StudyGuardApp

enum class MarathonPhase {
    IDLE,
    STUDY,
    BREAK,
    COMPLETED
}

data class MarathonConfig(
    val title: String = "4-Hour Focus Marathon",
    val studyDurationMinutes: Int = 45,
    val breakDurationMinutes: Int = 15,
    val totalCycles: Int = 4,
    val autoStartNextCycle: Boolean = true
) {
    val totalMinutes: Int
        get() = totalCycles * (studyDurationMinutes + breakDurationMinutes)

    val totalHoursFormatted: String
        get() {
            val h = totalMinutes / 60
            val m = totalMinutes % 60
            return if (m == 0) "${h}h" else "${h}h ${m}m"
        }

    val totalStudyMinutes: Int
        get() = totalCycles * studyDurationMinutes
}

data class MarathonState(
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val isNuclear: Boolean = false,
    val config: MarathonConfig = MarathonConfig(),
    val currentCycle: Int = 1,
    val currentPhase: MarathonPhase = MarathonPhase.IDLE,
    val phaseRemainingSeconds: Long = 0L,
    val totalRemainingSeconds: Long = 0L,
    val completedStudyMinutes: Int = 0
) {
    val phaseTimeString: String
        get() {
            val mins = (phaseRemainingSeconds / 60).coerceAtLeast(0)
            val secs = (phaseRemainingSeconds % 60).coerceAtLeast(0)
            return String.format(java.util.Locale.getDefault(), "%02d:%02d", mins, secs)
        }

    val progressFraction: Float
        get() {
            val phaseTotal = when (currentPhase) {
                MarathonPhase.STUDY -> config.studyDurationMinutes * 60f
                MarathonPhase.BREAK -> config.breakDurationMinutes * 60f
                else -> 1f
            }
            if (phaseTotal <= 0) return 0f
            return ((phaseTotal - phaseRemainingSeconds) / phaseTotal).coerceIn(0f, 1f)
        }
}
