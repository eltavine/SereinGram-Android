package com.eltavine.sereingram.features.ghost

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import org.telegram.messenger.R

/** The ghost mode tile in quick settings, after NagramXF's request for a toggle outside the app. */
@RequiresApi(Build.VERSION_CODES.N)
class GhostTileService : TileService() {
    override fun onStartListening() {
        render()
    }

    override fun onClick() {
        unlockAndRun {
            NagramGhost.setActive(!NagramGhost.isActive)
            render()
        }
    }

    private fun render() {
        val tile = qsTile ?: return
        tile.state = if (NagramGhost.isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.serein_ghost_title)
        tile.icon = Icon.createWithResource(this, R.drawable.icon_ghost)
        tile.updateTile()
    }
}

/** Flips ghost mode from a home screen shortcut, without showing the app. */
class GhostToggleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NagramGhost.setActive(!NagramGhost.isActive)
        val state = if (NagramGhost.isActive) R.string.serein_ghost_on else R.string.serein_ghost_off
        Toast.makeText(this, state, Toast.LENGTH_SHORT).show()
        finish()
    }
}

internal object GhostShortcut {
    /** Asks the launcher to pin the toggle; false when it cannot. */
    fun request(context: Context): Boolean {
        // Before Android 8 the launcher starts the shortcut itself, which the toggle, not exported, refuses.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            return false
        }
        val shortcut = ShortcutInfoCompat.Builder(context, ID)
            .setShortLabel(context.getString(R.string.serein_ghost_title))
            .setIcon(IconCompat.createWithResource(context, R.drawable.serein_shortcut_ghost))
            .setIntent(Intent(context, GhostToggleActivity::class.java).setAction(Intent.ACTION_VIEW))
            .build()
        return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }

    private const val ID = "serein_ghost_toggle"
}
