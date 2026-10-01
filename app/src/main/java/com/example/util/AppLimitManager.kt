package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.Locale

sealed class AppLimitCheckResult {
    object Allowed : AppLimitCheckResult()
    data class CooldownActive(
        val limitMinutes: Int,
        val usedMinutes: Int,
        val remainingCooldownSeconds: Long,
        val totalCooldownMinutes: Int = 120
    ) : AppLimitCheckResult()
    data class LimitExceeded(
        val limitMinutes: Int,
        val usedMinutes: Int,
        val lockedUntilTimestamp: Long
    ) : AppLimitCheckResult()
}

data class AppLimitItemState(
    val packageName: String,
    val limitMinutes: Int = 0, // 0 = no limit
    val usedMinutesToday: Int = 0,
    val remainingMinutesToday: Int = 0,
    val isLimitExceeded: Boolean = false,
    val isInCooldown: Boolean = false,
    val cooldownRemainingFormatted: String = "",
    val cooldownRemainingSeconds: Long = 0L,
    val isModifyLocked: Boolean = false,
    val modifyLockRemainingFormatted: String = ""
)

class AppLimitManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_usage_limits_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_PACKAGES = "packages_with_limits"
        private const val PREFIX_LIMIT_MINS = "limit_mins_"
        private const val PREFIX_LOCK_UNTIL = "lock_until_"
        private const val PREFIX_SET_DATE = "set_date_"
        private const val PREFIX_COOLDOWN_START = "cooldown_start_"
        private const val PREFIX_COOLDOWN_DATE = "cooldown_date_"
        private const val PREFIX_INTERNAL_USAGE = "internal_usage_"

        @Volatile
        private var instance: AppLimitManager? = null

        fun getInstance(context: Context): AppLimitManager {
            return instance ?: synchronized(this) {
                instance ?: AppLimitManager(context.applicationContext).also { instance = it }
            }
        }

        fun getTodayDateKey(): String {
            val cal = Calendar.getInstance()
            return String.format(
                Locale.US,
                "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        fun getNextMorning6AmTimestamp(): Long {
            val cal = Calendar.getInstance()
            // If already past 6:00 AM today, next morning is tomorrow 6:00 AM
            if (cal.get(Calendar.HOUR_OF_DAY) >= 6) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            cal.set(Calendar.HOUR_OF_DAY, 6)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }

    private val _limitsFlow = MutableStateFlow<Map<String, AppLimitItemState>>(emptyMap())
    val limitsFlow: StateFlow<Map<String, AppLimitItemState>> = _limitsFlow.asStateFlow()

    init {
        refreshAll()
    }

    fun getAllConfiguredPackages(): Set<String> {
        return prefs.getStringSet(PREF_PACKAGES, emptySet()) ?: emptySet()
    }

    fun getAppLimitMinutes(packageName: String): Int {
        return prefs.getInt(PREFIX_LIMIT_MINS + packageName, 0)
    }

    fun getLimitLockedUntil(packageName: String): Long {
        return prefs.getLong(PREFIX_LOCK_UNTIL + packageName, 0L)
    }

    fun canModifyLimit(packageName: String): Boolean {
        val currentLimit = getAppLimitMinutes(packageName)
        if (currentLimit <= 0) return true
        val lockUntil = getLimitLockedUntil(packageName)
        val now = System.currentTimeMillis()
        return now >= lockUntil
    }

    fun getModifyLockRemainingFormatted(packageName: String): String {
        val lockUntil = getLimitLockedUntil(packageName)
        val now = System.currentTimeMillis()
        if (now >= lockUntil) return ""
        val diffSec = (lockUntil - now) / 1000L
        val hours = diffSec / 3600
        val mins = (diffSec % 3600) / 60
        return String.format(Locale.getDefault(), "%dh %02dm (until 6:00 AM)", hours, mins)
    }

    /**
     * Sets or updates daily limit for an app.
     * If locked until tomorrow 6:00 AM, returns false and refuses modification.
     */
    fun setAppLimit(packageName: String, limitMinutes: Int): Boolean {
        if (!canModifyLimit(packageName)) {
            return false // Locked until next morning!
        }

        val packages = getAllConfiguredPackages().toMutableSet()
        val nextMorning6Am = getNextMorning6AmTimestamp()
        val today = getTodayDateKey()

        if (limitMinutes > 0) {
            packages.add(packageName)
            prefs.edit()
                .putStringSet(PREF_PACKAGES, packages)
                .putInt(PREFIX_LIMIT_MINS + packageName, limitMinutes)
                .putLong(PREFIX_LOCK_UNTIL + packageName, nextMorning6Am)
                .putString(PREFIX_SET_DATE + packageName, today)
                .apply()
        } else {
            packages.remove(packageName)
            prefs.edit()
                .putStringSet(PREF_PACKAGES, packages)
                .remove(PREFIX_LIMIT_MINS + packageName)
                .remove(PREFIX_LOCK_UNTIL + packageName)
                .remove(PREFIX_SET_DATE + packageName)
                .remove(PREFIX_COOLDOWN_START + packageName)
                .remove(PREFIX_COOLDOWN_DATE + packageName)
                .apply()
        }

        refreshAll()
        return true
    }

    fun removeAppLimit(packageName: String): Boolean {
        if (!canModifyLimit(packageName)) {
            return false
        }
        return setAppLimit(packageName, 0)
    }

    fun recordForegroundUsage(packageName: String, elapsedMs: Long) {
        if (elapsedMs <= 0) return
        val today = getTodayDateKey()
        val key = PREFIX_INTERNAL_USAGE + today + "_" + packageName
        val current = prefs.getLong(key, 0L)
        prefs.edit().putLong(key, current + elapsedMs).apply()
        refreshAll()
    }

    fun getUsedMinutesToday(packageName: String): Int {
        val usageStatsMs = UsageStatsManagerHelper.getTodayPackageUsageMs(context, packageName)
        val today = getTodayDateKey()
        val internalMs = prefs.getLong(PREFIX_INTERNAL_USAGE + today + "_" + packageName, 0L)
        val maxMs = maxOf(usageStatsMs, internalMs)
        return (maxMs / 60_000L).toInt()
    }

    fun checkAppLimit(context: Context, packageName: String): AppLimitCheckResult {
        val limitMinutes = getAppLimitMinutes(packageName)
        if (limitMinutes <= 0) {
            return AppLimitCheckResult.Allowed
        }

        val usedMinutes = getUsedMinutesToday(packageName)
        val lockUntil = getLimitLockedUntil(packageName)

        // 1. Full daily limit exceeded -> Lock until tomorrow morning
        if (usedMinutes >= limitMinutes) {
            return AppLimitCheckResult.LimitExceeded(
                limitMinutes = limitMinutes,
                usedMinutes = usedMinutes,
                lockedUntilTimestamp = lockUntil
            )
        }

        // 2. Half-time usage reached -> 2-Hour (120 min) Focus Cooldown Lockout
        val halfLimit = (limitMinutes / 2).coerceAtLeast(1)
        if (usedMinutes >= halfLimit) {
            val today = getTodayDateKey()
            val cooldownDate = prefs.getString(PREFIX_COOLDOWN_DATE + packageName, "") ?: ""
            var cooldownStart = prefs.getLong(PREFIX_COOLDOWN_START + packageName, 0L)

            // If cooldown hasn't been triggered yet today, trigger it now!
            if (cooldownDate != today || cooldownStart <= 0L) {
                cooldownStart = System.currentTimeMillis()
                prefs.edit()
                    .putLong(PREFIX_COOLDOWN_START + packageName, cooldownStart)
                    .putString(PREFIX_COOLDOWN_DATE + packageName, today)
                    .apply()
            }

            val cooldownDurationMs = 120 * 60 * 1000L // 2 Hours cooldown
            val elapsedCooldownMs = System.currentTimeMillis() - cooldownStart
            if (elapsedCooldownMs < cooldownDurationMs) {
                val remainingSeconds = (cooldownDurationMs - elapsedCooldownMs) / 1000L
                return AppLimitCheckResult.CooldownActive(
                    limitMinutes = limitMinutes,
                    usedMinutes = usedMinutes,
                    remainingCooldownSeconds = remainingSeconds,
                    totalCooldownMinutes = 120
                )
            }
        }

        return AppLimitCheckResult.Allowed
    }

    fun getItemState(packageName: String): AppLimitItemState {
        val limitMinutes = getAppLimitMinutes(packageName)
        if (limitMinutes <= 0) {
            return AppLimitItemState(packageName = packageName, limitMinutes = 0)
        }

        val usedMinutes = getUsedMinutesToday(packageName)
        val remainingMinutes = (limitMinutes - usedMinutes).coerceAtLeast(0)
        val isExceeded = usedMinutes >= limitMinutes

        val isLocked = !canModifyLimit(packageName)
        val lockRemaining = if (isLocked) getModifyLockRemainingFormatted(packageName) else ""

        var inCooldown = false
        var cdRemainingSec = 0L
        var cdFormatted = ""

        if (!isExceeded && usedMinutes >= (limitMinutes / 2).coerceAtLeast(1)) {
            val today = getTodayDateKey()
            val cooldownDate = prefs.getString(PREFIX_COOLDOWN_DATE + packageName, "") ?: ""
            val cooldownStart = prefs.getLong(PREFIX_COOLDOWN_START + packageName, 0L)
            if (cooldownDate == today && cooldownStart > 0L) {
                val cooldownDurationMs = 120 * 60 * 1000L
                val elapsed = System.currentTimeMillis() - cooldownStart
                if (elapsed < cooldownDurationMs) {
                    inCooldown = true
                    cdRemainingSec = (cooldownDurationMs - elapsed) / 1000L
                    val h = cdRemainingSec / 3600
                    val m = (cdRemainingSec % 3600) / 60
                    val s = cdRemainingSec % 60
                    cdFormatted = if (h > 0) {
                        String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
                    } else {
                        String.format(Locale.getDefault(), "%02d:%02d", m, s)
                    }
                }
            }
        }

        return AppLimitItemState(
            packageName = packageName,
            limitMinutes = limitMinutes,
            usedMinutesToday = usedMinutes,
            remainingMinutesToday = remainingMinutes,
            isLimitExceeded = isExceeded,
            isInCooldown = inCooldown,
            cooldownRemainingFormatted = cdFormatted,
            cooldownRemainingSeconds = cdRemainingSec,
            isModifyLocked = isLocked,
            modifyLockRemainingFormatted = lockRemaining
        )
    }

    fun refreshAll() {
        val packages = getAllConfiguredPackages()
        val map = mutableMapOf<String, AppLimitItemState>()
        for (pkg in packages) {
            map[pkg] = getItemState(pkg)
        }
        _limitsFlow.value = map
    }
}
