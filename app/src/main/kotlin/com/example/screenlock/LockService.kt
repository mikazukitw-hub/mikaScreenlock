package com.example.screenlock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat

class LockService : Service() {

    private lateinit var overlayManager: LockOverlayManager
    private lateinit var audioManager: AudioManager
    private var volumeObserver: ContentObserver? = null

    private var lastVolume = -1
    private var lastVolumeDownTime = 0L
    private var lastVolumeUpTime = 0L
    private var volumeDownPressCount = 0
    private var volumeUpPressCount = 0

    override fun onCreate() {
        super.onCreate()
        overlayManager = LockOverlayManager(this)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        registerVolumeObserver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UNLOCK -> overlayManager.unlock()
            ACTION_LOCK_NOW -> overlayManager.lock()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        unregisterVolumeObserver()
        overlayManager.unlock()
    }

    private fun registerVolumeObserver() {
        lastVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (currentVolume == lastVolume) return

                val now = System.currentTimeMillis()
                if (currentVolume < lastVolume) {
                    if (now - lastVolumeDownTime > TRIPLE_PRESS_TIMEOUT) {
                        volumeDownPressCount = 1
                    } else {
                        volumeDownPressCount++
                    }
                    lastVolumeDownTime = now
                    if (volumeDownPressCount >= 3) {
                        overlayManager.lock()
                        volumeDownPressCount = 0
                    }
                } else if (currentVolume > lastVolume) {
                    if (now - lastVolumeUpTime > TRIPLE_PRESS_TIMEOUT) {
                        volumeUpPressCount = 1
                    } else {
                        volumeUpPressCount++
                    }
                    lastVolumeUpTime = now
                    if (volumeUpPressCount >= 3) {
                        overlayManager.unlock()
                        volumeUpPressCount = 0
                    }
                }
                lastVolume = currentVolume
            }
        }
        volumeObserver = observer
        contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
    }

    private fun unregisterVolumeObserver() {
        volumeObserver?.let { contentResolver.unregisterContentObserver(it) }
        volumeObserver = null
    }

    private fun buildNotification(): Notification {
        val lockIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, LockService::class.java).apply {
                action = ACTION_LOCK_NOW
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile_lock)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(R.drawable.ic_tile_lock, getString(R.string.action_lock_now), lockIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_LOCK_NOW = "com.example.screenlock.ACTION_LOCK_NOW"
        const val ACTION_UNLOCK = "com.example.screenlock.ACTION_UNLOCK"
        private const val CHANNEL_ID = "screenlock_channel"
        private const val NOTIFICATION_ID = 1
        private const val TRIPLE_PRESS_TIMEOUT = 1000L
    }
}
