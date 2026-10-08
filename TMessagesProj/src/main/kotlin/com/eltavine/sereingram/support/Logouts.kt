package com.eltavine.sereingram.support

import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.UserConfig

/**
 * Accounts as they log out, once Telegram has dropped its own data for them
 * and before another account can log in in their place.
 */
internal object Logouts {
    /** Calls [onLogout] on the UI thread with every account that logs out; call it on the UI thread. */
    fun observe(onLogout: (account: Int) -> Unit) {
        val observer = NotificationCenter.NotificationCenterDelegate { id, account, _ ->
            if (id == NotificationCenter.appDidLogout) {
                onLogout(account)
            }
        }
        for (account in 0 until UserConfig.MAX_ACCOUNT_COUNT) {
            NotificationCenter.getInstance(account).addObserver(observer, NotificationCenter.appDidLogout)
        }
    }
}
