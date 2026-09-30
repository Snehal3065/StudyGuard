package com.example.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.util.StudyPreferences

class StudyDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "StudyGuard Uninstall Protection Enabled 🛡️", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        if (StudyPreferences.isStudyActive(context) && StudyPreferences.isStrictUninstallLockEnabled(context)) {
            val subject = StudyPreferences.getSessionSubject(context)
            val endTime = StudyPreferences.getSessionEndTime(context)
            val minutesLeft = ((endTime - System.currentTimeMillis()) / 60000L).coerceAtLeast(1)
            return "WARNING: Study session '$subject' is currently active ($minutesLeft min left)! Uninstall protection prevents deactivation during study hours."
        }
        return "Disabling device administrator will remove uninstall protection for StudyGuard."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "StudyGuard Uninstall Protection Deactivated", Toast.LENGTH_SHORT).show()
    }
}
