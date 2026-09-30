package com.example.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class ClickService : AccessibilityService() {

    private lateinit var wm: WindowManager
    private var dot: View? = null
    private var panel: LinearLayout? = null
    private lateinit var dotParams: WindowManager.LayoutParams
    private lateinit var panelParams: WindowManager.LayoutParams
    private lateinit var startBtn: Button
    private lateinit var speedBtn: Button
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private val speeds = longArrayOf(100, 200, 500, 1000)
    private var speedIdx = 1
    private val intervalMs get() = speeds[speedIdx]

    private val loop = object : Runnable {
        override fun run() {
            if (!running) return
            val d = dot ?: return
            val loc = IntArray(2)
            d.getLocationOnScreen(loc)
            tap(loc[0] + d.width / 2f, loc[1] + d.height / 2f)
            handler.postDelayed(this, intervalMs)
        }
    }

    override fun onServiceConnected() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        if (dot == null) { addDot(); addPanel() }
    }

    private fun tap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 10))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun addDot() {
        val size = (28 * resources.displayMetrics.density).toInt()   // pehle 56 tha, ab 50% chhota
        val v = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(140, 255, 0, 0))
                setStroke(3, Color.WHITE)
            }
        }
        dotParams = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 400; y = 800 }

        var sx = 0f; var sy = 0f; var px = 0; var py = 0
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; px = dotParams.x; py = dotParams.y }
                MotionEvent.ACTION_MOVE -> {
                    dotParams.x = px + (e.rawX - sx).toInt()
                    dotParams.y = py + (e.rawY - sy).toInt()
                    wm.updateViewLayout(v, dotParams)
                }
            }
            true
        }
        dot = v
        wm.addView(v, dotParams)
    }

    private fun addPanel() {
        val density = resources.displayMetrics.density

        // Drag handle: isko pakad ke panel ko kahin bhi le jao
        val handle = TextView(this).apply {
            text = "☰ Drag"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.argb(200, 60, 60, 60))
            gravity = Gravity.CENTER
            val pad = (8 * density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        startBtn = Button(this).apply { text = "START"; setOnClickListener { toggle() } }
        speedBtn = Button(this).apply {
            text = "${intervalMs}ms"
            setOnClickListener {
                speedIdx = (speedIdx + 1) % speeds.size
                text = "${intervalMs}ms"
            }
        }
        val closeBtn = Button(this).apply { text = "✕ Close"; setOnClickListener { shutdown() } }

        val p = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(handle); addView(startBtn); addView(speedBtn); addView(closeBtn)
        }
        panel = p

        panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 100; y = 300 }

        var sx = 0f; var sy = 0f; var px = 0; var py = 0
        handle.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; px = panelParams.x; py = panelParams.y }
                MotionEvent.ACTION_MOVE -> {
                    panelParams.x = px + (e.rawX - sx).toInt()
                    panelParams.y = py + (e.rawY - sy).toInt()
                    wm.updateViewLayout(p, panelParams)
                }
            }
            true
        }
        wm.addView(p, panelParams)
    }

    private fun toggle() {
        running = !running
        // chalte waqt dot touch-through rahe, warna click dot par hi lagega
        dotParams.flags = if (running)
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        dot?.let { wm.updateViewLayout(it, dotParams) }
        startBtn.text = if (running) "STOP" else "START"
        if (running) handler.post(loop) else handler.removeCallbacks(loop)
    }

    private fun removeViews() {
        running = false
        handler.removeCallbacks(loop)
        runCatching { dot?.let { wm.removeView(it) } }
        runCatching { panel?.let { wm.removeView(it) } }
        dot = null; panel = null
    }

    private fun shutdown() {
        removeViews()
        disableSelf()   // service OFF, verification pe koi asar nahi
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() {
        removeViews()
        super.onDestroy()
    }
}
