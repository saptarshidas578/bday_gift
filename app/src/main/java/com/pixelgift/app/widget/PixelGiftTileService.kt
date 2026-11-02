package com.pixelgift.app.widget

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.pixelgift.app.R
import com.pixelgift.app.protocol.Command
import com.pixelgift.app.protocol.ProtocolConstants
import com.pixelgift.app.transport.BleTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Quick-Settings System Tile for fast status check and power / Cherry Red scene toggle.
 * Complies with SPEC 9.4 and 10.
 */
class PixelGiftTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isTileActive = true

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        isTileActive = !isTileActive
        updateTileState()

        scope.launch {
            try {
                val transport = BleTransport(applicationContext)
                val cmd = if (isTileActive) {
                    Command.SetColor(210, 4, 45) // Cherry Red
                } else {
                    Command.SetMode(ProtocolConstants.MODE_OFF)
                }
                transport.send(cmd)
            } catch (e: Exception) {
                // Handled gracefully
            }
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        tile.state = if (isTileActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            tile.subtitle = if (isTileActive) "Cherry Red" else "Off"
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_splash_logo)
        tile.updateTile()
    }
}

