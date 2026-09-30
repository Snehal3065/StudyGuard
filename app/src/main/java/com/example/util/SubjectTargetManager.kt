package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.StudySession
import org.json.JSONArray
import org.json.JSONObject

data class SubjectTarget(
    val subject: String,
    val targetHours: Int,
    val emoji: String,
    val colorHex: String
) {
    fun getProgressPercent(completedMinutes: Int): Int {
        val targetMinutes = targetHours * 60
        if (targetMinutes <= 0) return 0
        return ((completedMinutes.toFloat() / targetMinutes) * 100).toInt().coerceIn(0, 100)
    }

    fun getFormattedCompleted(completedMinutes: Int): String {
        val h = completedMinutes / 60
        val m = completedMinutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}

class SubjectTargetManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("study_subject_targets_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TARGETS_JSON = "subject_targets_json"
        private const val KEY_INIT = "subject_targets_init_v1"

        @Volatile
        private var instance: SubjectTargetManager? = null

        fun getInstance(context: Context): SubjectTargetManager {
            return instance ?: synchronized(this) {
                instance ?: SubjectTargetManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val defaultTargets = listOf(
        SubjectTarget("Mathematics", 25, "📐", "#6366F1"),
        SubjectTarget("Physics", 20, "⚡", "#F59E0B"),
        SubjectTarget("Computer Science", 25, "💻", "#06B6D4"),
        SubjectTarget("Biology", 20, "🧬", "#10B981"),
        SubjectTarget("Chemistry", 15, "🧪", "#EC4899"),
        SubjectTarget("General Study", 30, "📚", "#8B5CF6")
    )

    fun getTargets(): List<SubjectTarget> {
        val isInit = prefs.getBoolean(KEY_INIT, false)
        if (!isInit) {
            saveTargets(defaultTargets)
            prefs.edit().putBoolean(KEY_INIT, true).apply()
            return defaultTargets
        }

        val json = prefs.getString(KEY_TARGETS_JSON, null) ?: return defaultTargets
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<SubjectTarget>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SubjectTarget(
                        subject = obj.getString("subject"),
                        targetHours = obj.getInt("targetHours"),
                        emoji = obj.optString("emoji", "📚"),
                        colorHex = obj.optString("colorHex", "#6366F1")
                    )
                )
            }
            if (list.isEmpty()) defaultTargets else list
        } catch (e: Exception) {
            defaultTargets
        }
    }

    fun saveTargets(targets: List<SubjectTarget>) {
        val arr = JSONArray()
        targets.forEach { t ->
            val obj = JSONObject().apply {
                put("subject", t.subject)
                put("targetHours", t.targetHours)
                put("emoji", t.emoji)
                put("colorHex", t.colorHex)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_TARGETS_JSON, arr.toString()).apply()
    }

    fun updateTarget(subject: String, newHours: Int) {
        val current = getTargets().map {
            if (it.subject.equals(subject, ignoreCase = true)) {
                it.copy(targetHours = newHours.coerceAtLeast(1))
            } else it
        }
        saveTargets(current)
    }

    fun addTarget(subject: String, targetHours: Int, emoji: String = "📚", colorHex: String = "#6366F1") {
        val current = getTargets().filterNot { it.subject.equals(subject, ignoreCase = true) }
        val updated = current + SubjectTarget(subject.trim(), targetHours.coerceAtLeast(1), emoji, colorHex)
        saveTargets(updated)
    }
}
