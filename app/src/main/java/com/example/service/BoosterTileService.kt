package com.example.service

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

/**
 * Quick Settings Tile to easily toggle the Game Booster from the status shade.
 */
@RequiresApi(Build.VERSION_CODES.N)
class BoosterTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val context = applicationContext
        val isServiceRunning = isServiceRunning(context, BoosterService::class.java)

        val serviceIntent = Intent(context, BoosterService::class.java)
        if (isServiceRunning) {
            serviceIntent.action = "STOP_SERVICE"
            context.startService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // Delay slightly to allow service to start/stop before updating tile
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            updateTileState()
        }, 300)
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isRunning = isServiceRunning(applicationContext, BoosterService::class.java)

        if (isRunning) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Booster: Active"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Monitoring active apps"
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Game Booster"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Tap to optimize"
            }
        }
        tile.updateTile()
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
