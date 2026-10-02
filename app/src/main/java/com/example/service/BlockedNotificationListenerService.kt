package com.example.service

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.local.StudyDatabase
import com.example.util.AppLimitCheckResult
import com.example.util.AppLimitManager
import com.example.util.EmergencyLockManager
import com.example.util.NotificationBlockReportManager
import com.example.util.StudyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BlockedNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val TAG = "FocusNotificationShield"

    companion object {
        val DEFAULT_DISTRACTION_PACKAGES = setOf(
            "com.google.android.youtube",
            "com.instagram.android",
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.twitter.android",
            "com.snapchat.android",
            "com.facebook.katana",
            "com.facebook.orca",
            "com.reddit.frontpage",
            "com.netflix.mediaclient",
            "tv.twitch.android.app",
            "com.discord",
            "com.pinterest",
            "com.badoo.mobile",
            "com.tinder"
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val pkg = sbn.packageName ?: return

        // NEVER block our own app's study reminders or timer notifications!
        if (pkg == applicationContext.packageName) return

        // Always allow incoming phone calls and SMS emergencies
        if (isSystemEmergency(pkg, sbn.notification)) return

        val context = applicationContext
        val emergencyLock = EmergencyLockManager.getInstance(context)
        val isNuclearBlockActive = emergencyLock.isNuclearBlockAppsActive()
        val isStudyActive = StudyPreferences.isStudyActive(context)

        // Only block if a focus session, study mode, or nuclear focus is running
        if (!isStudyActive && !isNuclearBlockActive) {
            return
        }

        serviceScope.launch {
            try {
                val shouldBlock = isAppDistraction(pkg)
                if (shouldBlock) {
                    // 1. Cancel notification so it doesn't alert the user
                    cancelNotification(sbn.key)

                    // 2. Extract app display name and content snippet
                    val appName = getAppLabel(pkg)
                    val extras = sbn.notification.extras
                    val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                    val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()

                    // 3. Record in NotificationBlockReportManager
                    NotificationBlockReportManager.getInstance(context).recordBlockedNotification(
                        packageName = pkg,
                        appName = appName,
                        title = title,
                        text = text
                    )

                    Log.d(TAG, "Shielded notification from $appName ($pkg): $title")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking notification: ${e.message}")
            }
        }
    }

    private suspend fun isAppDistraction(packageName: String): Boolean {
        // Check App Limit Manager first (cooldown or exceeded)
        val limitManager = AppLimitManager.getInstance(applicationContext)
        val limitResult = limitManager.checkAppLimit(applicationContext, packageName)
        if (limitResult is AppLimitCheckResult.LimitExceeded || limitResult is AppLimitCheckResult.CooldownActive) {
            return true
        }

        // Check Room database
        try {
            val db = StudyDatabase.getInstance(applicationContext)
            val distractionPackages = db.studyDao().getDistractionPackageNames()
            if (distractionPackages.contains(packageName)) {
                return true
            }
        } catch (e: Exception) {
            // fallback
        }

        // Check default known distraction packages
        return DEFAULT_DISTRACTION_PACKAGES.contains(packageName)
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val pm = applicationContext.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
        }
    }

    private fun isSystemEmergency(packageName: String, notification: Notification?): Boolean {
        // Allow dialer/phone call notifications
        if (packageName.contains("dialer") || packageName.contains("telecom") || packageName.contains("phone")) {
            return true
        }
        val category = notification?.category
        if (category == Notification.CATEGORY_CALL || category == Notification.CATEGORY_ALARM) {
            return true
        }
        return false
    }
}
