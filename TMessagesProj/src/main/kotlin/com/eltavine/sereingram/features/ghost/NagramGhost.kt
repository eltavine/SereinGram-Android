package com.eltavine.sereingram.features.ghost

import org.telegram.messenger.UserConfig
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.tl.TL_account
import org.telegram.ui.ActionBar.BaseFragment
import tw.nekomimi.nekogram.NekoConfig
import tw.nekomimi.nekogram.settings.NekoGhostModeActivity

/** Nagram's ghost mode, whose settings and request filter SereinGram builds on. */
internal object NagramGhost {
    val isActive: Boolean get() = NekoConfig.isGhostModeActive()

    val readsHidden: Boolean get() = !NekoConfig.sendReadMessagePackets

    val typingHidden: Boolean get() = !NekoConfig.sendUploadProgress

    /** Turning it on also tells Telegram right away that every account went offline. */
    fun setActive(active: Boolean) {
        NekoConfig.setGhostMode(active)
        GhostIndicator.refresh()
        if (active) {
            for (account in 0 until UserConfig.MAX_ACCOUNT_COUNT) {
                if (UserConfig.getInstance(account).isClientActivated) {
                    val offline = TL_account.updateStatus().apply { offline = true }
                    ConnectionsManager.getInstance(account).sendRequest(offline) { _, _ -> }
                }
            }
        }
    }

    fun settings(): BaseFragment = NekoGhostModeActivity()
}
