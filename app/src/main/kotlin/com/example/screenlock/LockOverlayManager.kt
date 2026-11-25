package com.example.screenlock

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast

class LockOverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null

    val isLocked: Boolean
        get() = overlayView != null

    @Suppress("DEPRECATION")
    fun lock(isTransparent: Boolean = false) {
        if (overlayView != null) return

        mainHandler.post {
            try {
                val params = WindowManager.LayoutParams().apply {
                    width = WindowManager.LayoutParams.MATCH_PARENT
                    height = WindowManager.LayoutParams.MATCH_PARENT
                    type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                    // Preserves exact original overlay flags
                    flags = -2130704504
                    format = PixelFormat.TRANSLUCENT
                    gravity = Gravity.TOP
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                    systemUiVisibility = 4871
                }

                val view = object : View(context) {
                    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
                        super.onWindowFocusChanged(hasWindowFocus)
                        if (!hasWindowFocus && isLocked) {
                            collapseStatusBar()
                        }
                    }
                }

                if (!isTransparent) {
                    view.setBackgroundColor(Color.argb(249, 0, 0, 0))
                } else {
                    view.setBackgroundColor(Color.TRANSPARENT)
                }

                windowManager.addView(view, params)
                overlayView = view
                showStatusToast("螢幕鎖定已開啟")
            } catch (e: Exception) {
                Toast.makeText(context, "鎖定失敗: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun collapseStatusBar() {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val collapsePanels = Class.forName("android.app.StatusBarManager")
                .getMethod("collapsePanels")
            collapsePanels.invoke(statusBarService)
        } catch (_: Exception) {
        }
    }

    fun unlock() {
        if (overlayView == null) return

        mainHandler.post {
            try {
                overlayView?.let { windowManager.removeView(it) }
                overlayView = null
                showStatusToast("螢幕鎖定已關閉")
            } catch (_: Exception) {
            }
        }
    }

    private fun showStatusToast(message: String) {
        val density = context.resources.displayMetrics.density
        val params = WindowManager.LayoutParams().apply {
            height = WindowManager.LayoutParams.WRAP_CONTENT
            width = WindowManager.LayoutParams.WRAP_CONTENT
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            flags = 152
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.CENTER
        }

        val pillBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24f * density
            setColor(Color.argb(215, 43, 53, 48))
        }

        val textView = TextView(context).apply {
            text = message
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(Color.parseColor("#F6F8F5"))
            background = pillBackground
            val hPad = (24 * density).toInt()
            val vPad = (12 * density).toInt()
            setPadding(hPad, vPad, hPad, vPad)
        }

        try {
            windowManager.addView(textView, params)
            mainHandler.postDelayed({
                try {
                    windowManager.removeView(textView)
                } catch (_: Exception) {
                }
            }, 2000L)
        } catch (_: Exception) {
        }
    }
}
