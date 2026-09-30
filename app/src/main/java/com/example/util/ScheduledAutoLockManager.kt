package com.example.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

data class AutoLockScheduleRule(
    val id: String,
    val name: String,
    val isEnabled: Boolean = true,
    val daysOfWeek: List<Int>, // Calendar.SUNDAY=1, MONDAY=2, ... SATURDAY=7
    val startHour: Int, // 0..23
    val startMinute: Int, // 0..59
    val endHour: Int, // 0..23
    val endMinute: Int, // 0..59
    val requiredStudyMinutes: Int // e.g. 120 (2 hours)
) {
    val formattedStartTime: String
        get() = formatTime(startHour, startMinute)

    val formattedEndTime: String
        get() = formatTime(endHour, endMinute)

    val formattedRequiredStudy: String
        get() {
            val hours = requiredStudyMinutes / 60
            val mins = requiredStudyMinutes % 60
            return when {
                hours > 0 && mins > 0 -> "${hours}h ${mins}m"
                hours > 0 -> "${hours}h"
                else -> "${mins}m"
            }
        }

    val daysFormatted: String
        get() {
            if (daysOfWeek.size == 7) return "Every day"
            if (daysOfWeek.containsAll(listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)) && daysOfWeek.size == 5) {
                return "Weekdays"
            }
            if (daysOfWeek.containsAll(listOf(Calendar.SATURDAY, Calendar.SUNDAY)) && daysOfWeek.size == 2) {
                return "Weekends"
            }
            val map = mapOf(
                Calendar.MONDAY to "Mon",
                Calendar.TUESDAY to "Tue",
                Calendar.WEDNESDAY to "Wed",
                Calendar.THURSDAY to "Thu",
                Calendar.FRIDAY to "Fri",
                Calendar.SATURDAY to "Sat",
                Calendar.SUNDAY to "Sun"
            )
            return daysOfWeek.sorted().mapNotNull { map[it] }.joinToString(", ")
        }

    private fun formatTime(hour: Int, minute: Int): String {
        val period = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, period)
    }
}

data class ActiveAutoLockStatus(
    val rule: AutoLockScheduleRule,
    val remainingStudyMinutes: Int,
    val completedStudyMinutes: Int
)

class ScheduledAutoLockManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("scheduled_auto_lock_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SCHEDULES_JSON = "schedules_json"
        private const val KEY_INITIALIZED = "schedules_initialized_v2"
    }

    private val defaultRules = listOf(
        AutoLockScheduleRule(
            id = "rule_morning_focus",
            name = "Morning Study Gate",
            isEnabled = true,
            daysOfWeek = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY),
            startHour = 8,
            startMinute = 30,
            endHour = 13,
            endMinute = 0,
            requiredStudyMinutes = 120 // 2 hours required
        ),
        AutoLockScheduleRule(
            id = "rule_evening_revision",
            name = "Evening Revision Gate",
            isEnabled = true,
            daysOfWeek = listOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY),
            startHour = 18,
            startMinute = 0,
            endHour = 22,
            endMinute = 0,
            requiredStudyMinutes = 90 // 1.5 hours required
        )
    )

    fun getRules(): List<AutoLockScheduleRule> {
        val isInitialized = prefs.getBoolean(KEY_INITIALIZED, false)
        if (!isInitialized) {
            saveRules(defaultRules)
            prefs.edit().putBoolean(KEY_INITIALIZED, true).apply()
            return defaultRules
        }

        val json = prefs.getString(KEY_SCHEDULES_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<AutoLockScheduleRule>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val daysArr = obj.getJSONArray("daysOfWeek")
                val days = mutableListOf<Int>()
                for (j in 0 until daysArr.length()) {
                    days.add(daysArr.getInt(j))
                }
                list.add(
                    AutoLockScheduleRule(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        daysOfWeek = days,
                        startHour = obj.getInt("startHour"),
                        startMinute = obj.getInt("startMinute"),
                        endHour = obj.getInt("endHour"),
                        endMinute = obj.getInt("endMinute"),
                        requiredStudyMinutes = obj.getInt("requiredStudyMinutes")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveRules(rules: List<AutoLockScheduleRule>) {
        val arr = JSONArray()
        rules.forEach { r ->
            val obj = JSONObject().apply {
                put("id", r.id)
                put("name", r.name)
                put("isEnabled", r.isEnabled)
                val daysArr = JSONArray()
                r.daysOfWeek.forEach { daysArr.put(it) }
                put("daysOfWeek", daysArr)
                put("startHour", r.startHour)
                put("startMinute", r.startMinute)
                put("endHour", r.endHour)
                put("endMinute", r.endMinute)
                put("requiredStudyMinutes", r.requiredStudyMinutes)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_SCHEDULES_JSON, arr.toString()).apply()
    }

    fun toggleRule(id: String, isEnabled: Boolean) {
        val current = getRules().map {
            if (it.id == id) it.copy(isEnabled = isEnabled) else it
        }
        saveRules(current)
    }

    fun addRule(
        name: String,
        daysOfWeek: List<Int>,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        requiredStudyMinutes: Int
    ) {
        val newRule = AutoLockScheduleRule(
            id = "rule_${System.currentTimeMillis()}",
            name = name.ifBlank { "Study Gate" },
            isEnabled = true,
            daysOfWeek = if (daysOfWeek.isEmpty()) listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY) else daysOfWeek,
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            requiredStudyMinutes = requiredStudyMinutes.coerceAtLeast(15)
        )
        val list = getRules().toMutableList().apply { add(newRule) }
        saveRules(list)
    }

    fun updateRule(updated: AutoLockScheduleRule) {
        val current = getRules().map {
            if (it.id == updated.id) updated else it
        }
        saveRules(current)
    }

    fun deleteRule(id: String) {
        val current = getRules().filterNot { it.id == id }
        saveRules(current)
    }

    /**
     * Checks if any rule is active right now and if the study requirement has not yet been satisfied today.
     */
    fun getActiveRule(todayStudiedMinutes: Int): ActiveAutoLockStatus? {
        val rules = getRules().filter { it.isEnabled }
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_WEEK)
        val currentMinutesOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        for (rule in rules) {
            if (!rule.daysOfWeek.contains(currentDay)) continue

            val ruleStartMinutes = rule.startHour * 60 + rule.startMinute
            val ruleEndMinutes = rule.endHour * 60 + rule.endMinute

            val inWindow = if (ruleEndMinutes >= ruleStartMinutes) {
                currentMinutesOfDay in ruleStartMinutes..ruleEndMinutes
            } else {
                // Crosses midnight
                currentMinutesOfDay >= ruleStartMinutes || currentMinutesOfDay <= ruleEndMinutes
            }

            if (inWindow) {
                if (todayStudiedMinutes < rule.requiredStudyMinutes) {
                    val remaining = rule.requiredStudyMinutes - todayStudiedMinutes
                    return ActiveAutoLockStatus(
                        rule = rule,
                        remainingStudyMinutes = remaining.coerceAtLeast(1),
                        completedStudyMinutes = todayStudiedMinutes
                    )
                }
            }
        }
        return null
    }
}
