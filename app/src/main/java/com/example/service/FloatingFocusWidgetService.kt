package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.example.MainActivity
import com.example.R
import com.example.util.StudyPreferences

/**
 * Accessibility & Focus Floating Overlay Widget:
 * Provides an on-screen floating widget showing study countdown timer,
 * quick study timer toggle, and active YouTube Shorts protection indicator.
 */
class FloatingFocusWidgetService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isExpanded = false

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateWidgetUI()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createFloatingWidget()
        handler.post(updateRunnable)
    }

    private fun createFloatingWidget() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.widget_floating_focus, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 150
        }

        // Setup drag & drop
        setupTouchListener()
        setupClickListeners()

        try {
            windowManager?.addView(floatingView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        updateWidgetUI()
    }

    private fun setupTouchListener() {
        val root = floatingView?.findViewById<View>(R.id.floating_widget_root) ?: return

        root.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val p = params ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = p.x
                        initialY = p.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isDragging = true
                            p.x = initialX + dx
                            p.y = initialY + dy
                            try {
                                windowManager?.updateViewLayout(floatingView, p)
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            toggleExpandedView()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun setupClickListeners() {
        val btnToggle = floatingView?.findViewById<View>(R.id.btn_widget_timer_toggle)
        btnToggle?.setOnClickListener {
            val isActive = StudyPreferences.isStudyActive(this)
            if (isActive) {
                StudyFocusService.stop(this)
                StudyPreferences.stopStudySession(this)
            } else {
                StudyPreferences.startStudySession(this, System.currentTimeMillis(), "Quick Study", 25)
                StudyFocusService.start(this)
            }
            updateWidgetUI()
        }

        val btnOpenApp = floatingView?.findViewById<View>(R.id.btn_widget_open_app)
        btnOpenApp?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        }

        val btnClose = floatingView?.findViewById<View>(R.id.btn_widget_close)
        btnClose?.setOnClickListener {
            stopSelf()
        }
    }

    private fun toggleExpandedView() {
        isExpanded = !isExpanded
        val expandedLayout = floatingView?.findViewById<View>(R.id.layout_widget_expanded)
        expandedLayout?.visibility = if (isExpanded) View.VISIBLE else View.GONE
    }

    private fun updateWidgetUI() {
        val tvTimer = floatingView?.findViewById<TextView>(R.id.tv_widget_timer)
        val tvStatus = floatingView?.findViewById<TextView>(R.id.tv_widget_status)
        val ivBadge = floatingView?.findViewById<ImageView>(R.id.iv_widget_badge)
        val btnToggle = floatingView?.findViewById<TextView>(R.id.btn_widget_timer_toggle)

        val isActive = StudyPreferences.isStudyActive(this)
        if (isActive) {
            val endTime = StudyPreferences.getSessionEndTime(this)
            val left = ((endTime - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
            val mins = left / 60
            val secs = left % 60
            tvTimer?.text = String.format("%02d:%02d", mins, secs)
            tvStatus?.text = "🛡️ Study Locked"
            btnToggle?.text = "Stop Session"
            ivBadge?.setImageResource(R.drawable.luffy_study_image_1790402150695)
        } else {
            tvTimer?.text = "Ready"
            tvStatus?.text = "StudyGuard Idle"
            btnToggle?.text = "Start 25m"
            ivBadge?.setImageResource(R.drawable.luffy_study_image_1790402150695)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
        if (floatingView != null) {
            try {
                windowManager?.removeView(floatingView)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, FloatingFocusWidgetService::class.java)
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingFocusWidgetService::class.java)
            context.stopService(intent)
        }
    }
}
