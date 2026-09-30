package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.service.StudyFocusService
import com.example.util.StudyPreferences

/**
 * Android Home Screen Accessibility & Study Widget:
 * Shows live timer countdown, YouTube shorts protection badge,
 * and allows immediate one-tap study timer toggle.
 */
class StudyGuardAppWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_STUDY) {
            val isActive = StudyPreferences.isStudyActive(context)
            if (isActive) {
                StudyFocusService.stop(context)
                StudyPreferences.stopStudySession(context)
            } else {
                StudyPreferences.startStudySession(context, System.currentTimeMillis(), "Widget Quick Study", 25)
                StudyFocusService.start(context)
            }
            updateAllWidgets(context)
        }
    }

    companion object {
        const val ACTION_TOGGLE_STUDY = "com.example.widget.ACTION_TOGGLE_STUDY"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_home_study)

            val isActive = StudyPreferences.isStudyActive(context)
            val subject = StudyPreferences.getSessionSubject(context)

            if (isActive) {
                val endTime = StudyPreferences.getSessionEndTime(context)
                val left = ((endTime - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
                val mins = left / 60
                val secs = left % 60
                views.setTextViewText(R.id.widget_tv_timer, String.format("%02d:%02d Left", mins, secs))
                views.setTextViewText(R.id.widget_tv_subject, "📚 $subject")
                views.setTextViewText(R.id.widget_btn_toggle, "Stop Study Session")
            } else {
                views.setTextViewText(R.id.widget_tv_timer, "Ready to Focus")
                views.setTextViewText(R.id.widget_tv_subject, "Tap below to start 25m focus")
                views.setTextViewText(R.id.widget_btn_toggle, "Start 25m Focus")
            }

            val shortsShieldOn = StudyPreferences.isBlockYouTubeShortsEnabled(context)
            views.setTextViewText(
                R.id.widget_tv_badge,
                if (shortsShieldOn) "Shorts Shield: Active" else "Shorts Shield: Off"
            )

            // Click on widget body opens MainActivity
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, openPendingIntent)

            // Click on toggle button triggers study timer toggle
            val toggleIntent = Intent(context, StudyGuardAppWidget::class.java).apply {
                action = ACTION_TOGGLE_STUDY
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context, 1, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_toggle, togglePendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, StudyGuardAppWidget::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (id in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }
    }
}
