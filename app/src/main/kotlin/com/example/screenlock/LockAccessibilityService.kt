package com.example.screenlock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class LockAccessibilityService : AccessibilityService() {

    private lateinit var overlayManager: LockOverlayManager
    private val doublePressTimeout = 1200L
    private var isTransparentMode = false
    private var lastVolumeDownTime = 0L
    private var lastVolumeUpTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        overlayManager = LockOverlayManager(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (::overlayManager.isInitialized && overlayManager.isLocked) {
            val packageName = event.packageName?.toString() ?: ""
            if (packageName.contains("systemui") ||
                event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            ) {
                extremeCollapse()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun extremeCollapse() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
        }
        collapseStatusBar()
        try {
            sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
        } catch (_: Exception) {
        }
    }

    private fun collapseStatusBar() {
        try {
            val statusBarService = getSystemService("statusbar")
            val collapsePanels = Class.forName("android.app.StatusBarManager")
                .getMethod("collapsePanels")
            collapsePanels.invoke(statusBarService)
        } catch (_: Exception) {
        }
    }

    override fun onInterrupt() {
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val now = System.currentTimeMillis()
            if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                val interval = now - lastVolumeDownTime
                lastVolumeDownTime = now
                if (interval <= doublePressTimeout) {
                    if (::overlayManager.isInitialized) {
                        overlayManager.lock(isTransparentMode)
                    }
                    return true
                }
            }
            if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                val interval = now - lastVolumeUpTime
                lastVolumeUpTime = now
                if (interval <= doublePressTimeout) {
                    if (::overlayManager.isInitialized && overlayManager.isLocked) {
                        overlayManager.unlock()
                        return true
                    }
                }
            }
            if (::overlayManager.isInitialized &&
                overlayManager.isLocked &&
                event.keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
                event.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN
            ) {
                return true
            }
        }
        return super.onKeyEvent(event)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SET_MODE -> {
                isTransparentMode = intent.getBooleanExtra(EXTRA_IS_TRANSPARENT, false)
            }
            LockService.ACTION_LOCK_NOW -> {
                if (::overlayManager.isInitialized) {
                    overlayManager.lock(isTransparentMode)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    companion object {
        const val ACTION_SET_MODE = "com.example.screenlock.ACTION_SET_MODE"
        const val EXTRA_IS_TRANSPARENT = "is_transparent"
        var instance: LockAccessibilityService? = null
    }
}
