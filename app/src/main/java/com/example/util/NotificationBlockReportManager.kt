package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.example.service.BlockedNotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppNotificationBlockCount(
    val packageName: String,
    val appName: String,
    val count: Int,
    val latestTimestamp: Long,
    val sampleSnippets: List<String> = emptyList()
)

data class NotificationShieldReport(
    val id: String = System.currentTimeMillis().toString(),
    val sessionTitle: String = "Focus Session",
    val startTimeMs: Long = System.currentTimeMillis(),
    val endTimeMs: Long = System.currentTimeMillis(),
    val totalBlockedCount: Int = 0,
    val appBreakdown: List<AppNotificationBlockCount> = emptyList()
) {
    val formattedDuration: String
        get() {
            val durationSec = ((endTimeMs - startTimeMs) / 1000).coerceAtLeast(0)
            val mins = durationSec / 60
            val secs = durationSec % 60
            return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            return sdf.format(Date(endTimeMs))
        }
}

class NotificationBlockReportManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("notification_shield_report_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_REPORT_JSON = "last_report_json"
        private const val KEY_ALL_TIME_BLOCKED = "all_time_blocked_count"
        private const val KEY_ACTIVE_SESSION_TITLE = "active_session_title"
        private const val KEY_ACTIVE_SESSION_START = "active_session_start"

        @Volatile
        private var instance: NotificationBlockReportManager? = null

        fun getInstance(context: Context): NotificationBlockReportManager {
            return instance ?: synchronized(this) {
                instance ?: NotificationBlockReportManager(context.applicationContext).also { instance = it }
            }
        }

        fun isNotificationAccessGranted(context: Context): Boolean {
            val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
            return enabledListeners.contains(context.packageName)
        }

        fun openNotificationAccessSettings(context: Context) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    // In-memory active session tracking
    private val _activeSessionBlockedMap = mutableMapOf<String, MutableAppBlockEntry>()
    private var isSessionActive: Boolean = false
    private var sessionTitle: String = "Focus Session"
    private var sessionStartTime: Long = 0L

    private val _currentSessionBlockedCount = MutableStateFlow(0)
    val currentSessionBlockedCount: StateFlow<Int> = _currentSessionBlockedCount.asStateFlow()

    private val _lastReportFlow = MutableStateFlow(loadLastReport())
    val lastReportFlow: StateFlow<NotificationShieldReport?> = _lastReportFlow.asStateFlow()

    private val _allTimeBlockedFlow = MutableStateFlow(prefs.getInt(KEY_ALL_TIME_BLOCKED, 0))
    val allTimeBlockedFlow: StateFlow<Int> = _allTimeBlockedFlow.asStateFlow()

    private class MutableAppBlockEntry(
        val packageName: String,
        var appName: String,
        var count: Int = 0,
        var latestTimestamp: Long = System.currentTimeMillis(),
        val sampleSnippets: MutableList<String> = mutableListOf()
    )

    @Synchronized
    fun startSession(title: String) {
        isSessionActive = true
        sessionTitle = title
        sessionStartTime = System.currentTimeMillis()
        _activeSessionBlockedMap.clear()
        _currentSessionBlockedCount.value = 0

        prefs.edit()
            .putString(KEY_ACTIVE_SESSION_TITLE, title)
            .putLong(KEY_ACTIVE_SESSION_START, sessionStartTime)
            .apply()
    }

    @Synchronized
    fun recordBlockedNotification(
        packageName: String,
        appName: String,
        title: String?,
        text: String?
    ) {
        val entry = _activeSessionBlockedMap.getOrPut(packageName) {
            MutableAppBlockEntry(packageName = packageName, appName = appName)
        }
        entry.count++
        entry.latestTimestamp = System.currentTimeMillis()
        if (entry.appName.isBlank() && appName.isNotBlank()) {
            entry.appName = appName
        }

        val snippet = when {
            !title.isNullOrBlank() && !text.isNullOrBlank() -> "$title: $text"
            !title.isNullOrBlank() -> title
            !text.isNullOrBlank() -> text
            else -> "Notification from $appName"
        }
        if (entry.sampleSnippets.size < 5) {
            entry.sampleSnippets.add(snippet)
        }

        val newTotal = _activeSessionBlockedMap.values.sumOf { it.count }
        _currentSessionBlockedCount.value = newTotal

        // Increment all time
        val allTime = prefs.getInt(KEY_ALL_TIME_BLOCKED, 0) + 1
        prefs.edit().putInt(KEY_ALL_TIME_BLOCKED, allTime).apply()
        _allTimeBlockedFlow.value = allTime
    }

    @Synchronized
    fun finishSession(): NotificationShieldReport? {
        val endTime = System.currentTimeMillis()
        val totalBlocked = _activeSessionBlockedMap.values.sumOf { it.count }

        val appBreakdown = _activeSessionBlockedMap.values.map { entry ->
            AppNotificationBlockCount(
                packageName = entry.packageName,
                appName = entry.appName.ifBlank { entry.packageName.substringAfterLast('.') },
                count = entry.count,
                latestTimestamp = entry.latestTimestamp,
                sampleSnippets = entry.sampleSnippets.toList()
            )
        }.sortedByDescending { it.count }

        val report = NotificationShieldReport(
            id = System.currentTimeMillis().toString(),
            sessionTitle = sessionTitle,
            startTimeMs = if (sessionStartTime > 0) sessionStartTime else (endTime - 1800_000L),
            endTimeMs = endTime,
            totalBlockedCount = totalBlocked,
            appBreakdown = appBreakdown
        )

        saveLastReport(report)
        _lastReportFlow.value = report

        isSessionActive = false
        _activeSessionBlockedMap.clear()
        _currentSessionBlockedCount.value = 0

        return report
    }

    fun getActiveAppBreakdown(): List<AppNotificationBlockCount> {
        return synchronized(this) {
            _activeSessionBlockedMap.values.map { entry ->
                AppNotificationBlockCount(
                    packageName = entry.packageName,
                    appName = entry.appName.ifBlank { entry.packageName.substringAfterLast('.') },
                    count = entry.count,
                    latestTimestamp = entry.latestTimestamp,
                    sampleSnippets = entry.sampleSnippets.toList()
                )
            }.sortedByDescending { it.count }
        }
    }

    private fun saveLastReport(report: NotificationShieldReport) {
        try {
            val json = JSONObject().apply {
                put("id", report.id)
                put("sessionTitle", report.sessionTitle)
                put("startTimeMs", report.startTimeMs)
                put("endTimeMs", report.endTimeMs)
                put("totalBlockedCount", report.totalBlockedCount)

                val arr = JSONArray()
                report.appBreakdown.forEach { item ->
                    val itemObj = JSONObject().apply {
                        put("packageName", item.packageName)
                        put("appName", item.appName)
                        put("count", item.count)
                        put("latestTimestamp", item.latestTimestamp)
                        val snipArr = JSONArray()
                        item.sampleSnippets.forEach { snipArr.put(it) }
                        put("snippets", snipArr)
                    }
                    arr.put(itemObj)
                }
                put("appBreakdown", arr)
            }
            prefs.edit().putString(KEY_LAST_REPORT_JSON, json.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadLastReport(): NotificationShieldReport? {
        val jsonStr = prefs.getString(KEY_LAST_REPORT_JSON, null) ?: return null
        return try {
            val json = JSONObject(jsonStr)
            val arr = json.optJSONArray("appBreakdown") ?: JSONArray()
            val breakdown = mutableListOf<AppNotificationBlockCount>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val snipArr = obj.optJSONArray("snippets") ?: JSONArray()
                val snips = mutableListOf<String>()
                for (j in 0 until snipArr.length()) {
                    snips.add(snipArr.getString(j))
                }
                breakdown.add(
                    AppNotificationBlockCount(
                        packageName = obj.optString("packageName"),
                        appName = obj.optString("appName"),
                        count = obj.optInt("count"),
                        latestTimestamp = obj.optLong("latestTimestamp"),
                        sampleSnippets = snips
                    )
                )
            }
            NotificationShieldReport(
                id = json.optString("id"),
                sessionTitle = json.optString("sessionTitle", "Focus Session"),
                startTimeMs = json.optLong("startTimeMs"),
                endTimeMs = json.optLong("endTimeMs"),
                totalBlockedCount = json.optInt("totalBlockedCount"),
                appBreakdown = breakdown
            )
        } catch (e: Exception) {
            null
        }
    }
}
