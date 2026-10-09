package com.eltavine.sereingram.support

import android.os.Handler
import android.os.Looper
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
        // Debug builds of Telegram check an observer's thread against its UI handler, which the app
        // creates only after SereinGram starts; no account can log out before the next turn of the UI thread.
        Handler(Looper.getMainLooper()).post {
            for (account in 0 until UserConfig.MAX_ACCOUNT_COUNT) {
                NotificationCenter.getInstance(account).addObserver(observer, NotificationCenter.appDidLogout)
            }
        }
    }
}
