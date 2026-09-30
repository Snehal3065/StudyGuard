package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class NuclearLockStatus(
    val isActive: Boolean = false,
    val remainingSeconds: Long = 0L,
    val totalMinutes: Int = 0,
    val formattedRemaining: String = "00:00"
)

class EmergencyLockManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("emergency_nuclear_lock_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NUCLEAR_END_TIME = "nuclear_end_time"
        private const val KEY_NUCLEAR_TOTAL_MINUTES = "nuclear_total_minutes"

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

    fun isNuclearLockActive(): Boolean {
        val endTime = prefs.getLong(KEY_NUCLEAR_END_TIME, 0L)
        return endTime > System.currentTimeMillis()
    }

    fun activateNuclearLock(minutes: Int) {
        val now = System.currentTimeMillis()
        val endTime = now + (minutes * 60 * 1000L)
        prefs.edit()
            .putLong(KEY_NUCLEAR_END_TIME, endTime)
            .putInt(KEY_NUCLEAR_TOTAL_MINUTES, minutes)
            .apply()
        refreshStatus()
    }

    fun checkStatus(): NuclearLockStatus {
        val endTime = prefs.getLong(KEY_NUCLEAR_END_TIME, 0L)
        val now = System.currentTimeMillis()
        if (endTime <= now) {
            return NuclearLockStatus(isActive = false, remainingSeconds = 0L, totalMinutes = 0, formattedRemaining = "00:00")
        }
        val remainingSec = (endTime - now) / 1000L
        val totalMins = prefs.getInt(KEY_NUCLEAR_TOTAL_MINUTES, 30)
        val m = remainingSec / 60
        val s = remainingSec % 60
        val formatted = String.format(Locale.getDefault(), "%02d:%02d", m, s)
        return NuclearLockStatus(
            isActive = true,
            remainingSeconds = remainingSec,
            totalMinutes = totalMins,
            formattedRemaining = formatted
        )
    }

    fun refreshStatus() {
        _statusFlow.value = checkStatus()
    }
}
