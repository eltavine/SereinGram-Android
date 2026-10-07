package com.eltavine.sereingram.features.proxy

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.MessagesController
import org.telegram.messenger.R
import org.telegram.messenger.SharedConfig

/** A quick settings tile that turns the chosen proxy on and off, after NagramXF's request for a proxy toggle. */
@RequiresApi(Build.VERSION_CODES.N)
class ProxyTileService : TileService() {
    override fun onStartListening() {
        render()
    }

    override fun onClick() {
        if (chosenProxy() == null) {
            return
        }
        SharedConfig.setProxyEnable(!isOn())
        render()
    }

    // The tile may start the process; Telegram's connections must exist before a proxy reaches them.
    private fun chosenProxy(): SharedConfig.ProxyInfo? {
        ApplicationLoader.postInitApplication()
        SharedConfig.loadProxyList()
        return SharedConfig.currentProxy
    }

    private fun isOn(): Boolean = MessagesController.getGlobalMainSettings().getBoolean("proxy_enabled", false)

    private fun render() {
        val tile = qsTile ?: return
        val proxy = chosenProxy()
        tile.state = when {
            proxy == null -> Tile.STATE_UNAVAILABLE
            isOn() -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.label = getString(R.string.Proxy)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = proxy?.address ?: getString(R.string.serein_proxy_tile_none)
        }
        tile.icon = Icon.createWithResource(this, R.drawable.outline_shield_plain_24)
        tile.updateTile()
    }
}
