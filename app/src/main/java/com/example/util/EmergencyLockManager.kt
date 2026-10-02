package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class NuclearLockStatus(
    val isActive: Boolean = false,
    val isMarathon: Boolean = false,
    val currentPhase: MarathonPhase = MarathonPhase.STUDY,
    val currentCycle: Int = 1,
    val totalCycles: Int = 1,
    val phaseRemainingSeconds: Long = 0L,
    val remainingSeconds: Long = 0L,
    val totalMinutes: Int = 0,
    val formattedRemaining: String = "00:00",
    val formattedPhaseRemaining: String = "00:00",
    val isBreakPhase: Boolean = false
)

class EmergencyLockManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("emergency_nuclear_lock_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NUCLEAR_END_TIME = "nuclear_end_time"
        private const val KEY_NUCLEAR_TOTAL_MINUTES = "nuclear_total_minutes"
        private const val KEY_IS_MARATHON = "nuclear_is_marathon"
        private const val KEY_STUDY_MINUTES = "nuclear_study_minutes"
        private const val KEY_BREAK_MINUTES = "nuclear_break_minutes"
        private const val KEY_TOTAL_CYCLES = "nuclear_total_cycles"
        private const val KEY_CURRENT_CYCLE = "nuclear_current_cycle"
        private const val KEY_IS_BREAK_PHASE = "nuclear_is_break_phase"
        private const val KEY_PHASE_END_TIME = "nuclear_phase_end_time"

        @Volatile
        private var instance: EmergencyLockManager? = null

        fun getInstance(context: Context): EmergencyLockManager {
            return instance ?: synchronized(this) {
                instance ?: EmergencyLockManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val _statusFlow = MutableStateFlow(checkStatus())
    val statusFlow: StateFlow<NuclearLockStatus> = _statusFlow.asStateFlow()

    /**
     * True whenever Nuclear Mode is active (both sprint and marathon, focus AND break).
     * During this entire time, settings and locked apps CANNOT be modified.
     */
    fun isNuclearLockActive(): Boolean {
        val endTime = prefs.getLong(KEY_NUCLEAR_END_TIME, 0L)
        return endTime > System.currentTimeMillis()
    }

    /**
     * True only during marathon break phase.
     */
    fun isNuclearBreakActive(): Boolean {
        if (!isNuclearLockActive()) return false
        val isMarathon = prefs.getBoolean(KEY_IS_MARATHON, false)
        return isMarathon && prefs.getBoolean(KEY_IS_BREAK_PHASE, false)
    }

    /**
     * True when Nuclear Lock is actively blocking distraction apps.
     * During break time, app blocking is temporarily relaxed, but settings & app lists remain frozen.
     */
    fun isNuclearBlockAppsActive(): Boolean {
        return isNuclearLockActive() && !isNuclearBreakActive()
    }

    fun isNuclearMarathon(): Boolean {
        return isNuclearLockActive() && prefs.getBoolean(KEY_IS_MARATHON, false)
    }

    /**
     * Activate a single one-way sprint lockout
     */
    fun activateNuclearLock(minutes: Int) {
        val now = System.currentTimeMillis()
        val endTime = now + (minutes * 60 * 1000L)
        prefs.edit()
            .putLong(KEY_NUCLEAR_END_TIME, endTime)
            .putInt(KEY_NUCLEAR_TOTAL_MINUTES, minutes)
            .putBoolean(KEY_IS_MARATHON, false)
            .putBoolean(KEY_IS_BREAK_PHASE, false)
            .putLong(KEY_PHASE_END_TIME, endTime)
            .apply()
        refreshStatus()
    }

    /**
     * Activate Nuclear Marathon Mode:
     * Focus cycles + break intervals for totalCycles.
     * The nuclear lock covers the FULL marathon duration so settings & locked apps are frozen even in break!
     */
    fun activateNuclearMarathon(config: MarathonConfig) {
        val now = System.currentTimeMillis()
        val totalMinutes = config.totalMinutes
        val overallEndTime = now + (totalMinutes * 60 * 1000L)
        val phaseEndTime = now + (config.studyDurationMinutes * 60 * 1000L)

        prefs.edit()
            .putLong(KEY_NUCLEAR_END_TIME, overallEndTime)
            .putInt(KEY_NUCLEAR_TOTAL_MINUTES, totalMinutes)
            .putBoolean(KEY_IS_MARATHON, true)
            .putInt(KEY_STUDY_MINUTES, config.studyDurationMinutes)
            .putInt(KEY_BREAK_MINUTES, config.breakDurationMinutes)
            .putInt(KEY_TOTAL_CYCLES, config.totalCycles)
            .putInt(KEY_CURRENT_CYCLE, 1)
            .putBoolean(KEY_IS_BREAK_PHASE, false)
            .putLong(KEY_PHASE_END_TIME, phaseEndTime)
            .apply()
        refreshStatus()
    }

    /**
     * Transition between Study and Break phases during Nuclear Marathon
     */
    fun setMarathonPhase(isBreak: Boolean, cycle: Int, phaseDurationMins: Int) {
        val now = System.currentTimeMillis()
        val phaseEndTime = now + (phaseDurationMins * 60 * 1000L)

        prefs.edit()
            .putBoolean(KEY_IS_BREAK_PHASE, isBreak)
            .putInt(KEY_CURRENT_CYCLE, cycle)
            .putLong(KEY_PHASE_END_TIME, phaseEndTime)
            .apply()
        refreshStatus()
    }

    fun completeNuclearMarathon() {
        prefs.edit()
            .remove(KEY_NUCLEAR_END_TIME)
            .remove(KEY_IS_MARATHON)
            .remove(KEY_IS_BREAK_PHASE)
            .remove(KEY_PHASE_END_TIME)
            .apply()
        refreshStatus()
    }

    fun checkStatus(): NuclearLockStatus {
        val endTime = prefs.getLong(KEY_NUCLEAR_END_TIME, 0L)
        val now = System.currentTimeMillis()
        if (endTime <= now) {
            return NuclearLockStatus(
                isActive = false,
                isMarathon = false,
                currentPhase = MarathonPhase.IDLE,
                remainingSeconds = 0L,
                phaseRemainingSeconds = 0L,
                totalMinutes = 0,
                formattedRemaining = "00:00",
                formattedPhaseRemaining = "00:00",
                isBreakPhase = false
            )
        }

        val remainingSec = (endTime - now) / 1000L
        val totalMins = prefs.getInt(KEY_NUCLEAR_TOTAL_MINUTES, 30)
        val isMarathon = prefs.getBoolean(KEY_IS_MARATHON, false)
        val isBreak = prefs.getBoolean(KEY_IS_BREAK_PHASE, false)
        val currentCycle = prefs.getInt(KEY_CURRENT_CYCLE, 1)
        val totalCycles = prefs.getInt(KEY_TOTAL_CYCLES, 1)

        val phaseEndTime = prefs.getLong(KEY_PHASE_END_TIME, endTime)
        val phaseRemainingSec = ((phaseEndTime - now) / 1000L).coerceAtLeast(0L)

        val m = remainingSec / 60
        val s = remainingSec % 60
        val formattedTotal = String.format(Locale.getDefault(), "%02d:%02d", m, s)

        val pm = phaseRemainingSec / 60
        val ps = phaseRemainingSec % 60
        val formattedPhase = String.format(Locale.getDefault(), "%02d:%02d", pm, ps)

        return NuclearLockStatus(
            isActive = true,
            isMarathon = isMarathon,
            currentPhase = if (isBreak) MarathonPhase.BREAK else MarathonPhase.STUDY,
            currentCycle = currentCycle,
            totalCycles = totalCycles,
            phaseRemainingSeconds = phaseRemainingSec,
            remainingSeconds = remainingSec,
            totalMinutes = totalMins,
            formattedRemaining = formattedTotal,
            formattedPhaseRemaining = formattedPhase,
            isBreakPhase = isBreak
        )
    }

    fun refreshStatus() {
        _statusFlow.value = checkStatus()
    }
}
