package com.angel.simpletouchlock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class LockScreenService : Service() {
    private var windowManager: WindowManager? = null
    private var lockOverlay: View? = null
    private val floatingButton: FloatingLockButton?
        get() = MainActivity.floatingButton

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "LockScreenServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "LOCK" -> showLockOverlay()
            "UNLOCK" -> hideLockOverlay()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Lock Screen Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Lock Active")
            .setContentText("Tap to open settings")
            .setSmallIcon(R.drawable.ic_lock)
            .build()
    }

    private fun showLockOverlay() {
        if (lockOverlay == null) {
            lockOverlay = View(this).apply {
                setBackgroundColor(Color.TRANSPARENT)
                isClickable = true
                isFocusable = true

                setOnClickListener {
                    floatingButton?.resetFadeFromExternal()
                }

                setOnTouchListener(object : View.OnTouchListener {
                    private var startX = 0f
                    private var startY = 0f
                    private val touchSlop = android.view.ViewConfiguration.get(context).scaledTouchSlop

                    override fun onTouch(v: View, event: MotionEvent): Boolean {
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                startX = event.x
                                startY = event.y
                                floatingButton?.resetFadeFromExternal()
                            }
                            MotionEvent.ACTION_UP -> {
                                val endX = event.x
                                val endY = event.y
                                if (abs(endX - startX) < touchSlop && abs(endY - startY) < touchSlop) {
                                    v.performClick()
                                }
                            }
                        }

                        if (isTouchEventInFabArea(event)) {
                            floatingButton?.dispatchTouchEvent(event)
                        }

                        return true
                    }
                })
            }

            val params = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                format = PixelFormat.TRANSLUCENT
            }
            windowManager?.addView(lockOverlay, params)
        }
    }

    private fun isTouchEventInFabArea(event: MotionEvent): Boolean {
        val fabLocation = IntArray(2)
        floatingButton?.getLocationOnScreen(fabLocation)
        val fabX = fabLocation[0]
        val fabY = fabLocation[1]
        val fabWidth = floatingButton?.width ?: 0
        val fabHeight = floatingButton?.height ?: 0

        return event.rawX >= fabX && event.rawX <= (fabX + fabWidth) &&
                event.rawY >= fabY && event.rawY <= (fabY + fabHeight)
    }

    private fun hideLockOverlay() {
        lockOverlay?.let {
            windowManager?.removeView(it)
            lockOverlay = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideLockOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}