package com.example.screenlock

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class LockTileService : TileService() {

    @Suppress("DEPRECATION")
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, LockAccessibilityService::class.java).apply {
            action = LockService.ACTION_LOCK_NOW
        }
        startForegroundService(intent)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }
}
