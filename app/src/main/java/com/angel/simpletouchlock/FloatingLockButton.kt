package com.angel.simpletouchlock

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.core.content.ContextCompat
import kotlin.math.abs

class FloatingLockButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val onToggleLock: ((Boolean) -> Unit)? = null
) : View(context, attrs, defStyleAttr) {

    private var windowManager: WindowManager? = null
    private var params: WindowManager.LayoutParams
    private var isLocked = false
    private var isAttached = false

    private val buttonSize = context.resources.getDimensionPixelSize(R.dimen.floating_button_size)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.semi_transparent_background)
        style = Paint.Style.FILL
    }

    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var isDragging = false

    private val handler = Handler(Looper.getMainLooper())
    private var fadeRunnable: Runnable? = null
    private var currentAlpha = 255
    private val minAlpha = 64
    private val fadeAnimator: ValueAnimator = ValueAnimator.ofInt(255, minAlpha).apply {
        duration = 2500 // 10 seconds
        addUpdateListener { animator ->
            currentAlpha = animator.animatedValue as Int
            invalidate()
        }
    }

    init {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        params = WindowManager.LayoutParams().apply {
            width = buttonSize
            height = buttonSize
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }

        setOnTouchListener(createTouchListener())
        startFadeTimer()
    }

    private fun createTouchListener() = OnTouchListener { _, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                resetFade()
                initialX = params.x.toFloat()
                initialY = params.y.toFloat()
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                true
            }
            MotionEvent.ACTION_MOVE -> {
                resetFade()
                val deltaX = event.rawX - initialTouchX
                val deltaY = event.rawY - initialTouchY
                if (!isDragging && (abs(deltaX) > touchSlop || abs(deltaY) > touchSlop)) {
                    isDragging = true
                }
                if (isDragging) {
                    params.x = (initialX + deltaX).toInt()
                    params.y = (initialY + deltaY).toInt()
                    windowManager?.updateViewLayout(this, params)
                }
                true
            }
            MotionEvent.ACTION_UP -> {
                resetFade()
                if (!isDragging) {
                    performClick()
                }
                true
            }
            else -> false
        }
    }

    private fun startFadeTimer() {
        fadeRunnable = Runnable {
            fadeAnimator.start()
        }
        handler.postDelayed(fadeRunnable!!, 2500)
    }

    private fun resetFade() {
        fadeAnimator.cancel()
        currentAlpha = 255
        invalidate()
        handler.removeCallbacks(fadeRunnable!!)
        startFadeTimer()
    }

    fun resetFadeFromExternal() {
        post { resetFade() }
    }

    override fun performClick(): Boolean {
        if (super.performClick()) return true
        isLocked = !isLocked
        onToggleLock?.let { toggleLock -> toggleLock(isLocked) }
        invalidate()
        return true
    }

    fun attach() {
        if (!isAttached) {
            try {
                windowManager?.addView(this, params)
                isAttached = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun detach() {
        if (isAttached) {
            try {
                windowManager?.removeView(this)
                isAttached = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        backgroundPaint.alpha = currentAlpha
        super.onDraw(canvas)
        val radius = width / 2f
        canvas.drawCircle(radius, radius, radius, backgroundPaint)
        val icon = if (isLocked) R.drawable.ic_lock else R.drawable.ic_lock_open
        val drawable = ContextCompat.getDrawable(context, icon)
        drawable?.let {
            it.alpha = currentAlpha
            it.setBounds(width / 4, height / 4, width * 3 / 4, height * 3 / 4)
            it.draw(canvas)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(buttonSize, buttonSize)
    }

    fun updateLockedState(locked: Boolean) {
        if (isLocked != locked) {
            isLocked = locked
            resetFade()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(fadeRunnable!!)
        fadeAnimator.cancel()
    }
}