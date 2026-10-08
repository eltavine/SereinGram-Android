package com.eltavine.sereingram.features.ghost

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.hooks.DialogsHooks
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.AndroidUtilities.dp
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.AnimatedEmojiDrawable

/** A ghost where the emoji status goes beside the chat list title, while ghost mode is on. */
internal class GhostIndicator(private val options: Options) : DialogsHooks.TitleStatus {
    private var icon: Drawable? = null

    override fun draw(account: Int, slot: Any, animated: Boolean): Boolean {
        watchNagram()
        if (!options.get(GhostOptions.statusIndicator) || !NagramGhost.isActive) {
            return false
        }
        val ghost = icon ?: ContextCompat.getDrawable(ApplicationLoader.applicationContext, R.drawable.icon_ghost)!!.mutate()
            .let { AnimatedEmojiDrawable.WrapSizeDrawable(it, dp(20f), dp(20f)) }
            .also { icon = it }
        ghost.colorFilter = PorterDuffColorFilter(Theme.getColor(Theme.key_profile_verifiedBackground), PorterDuff.Mode.SRC_IN)
        val status = slot as AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable
        status.set(ghost, animated)
        status.setParticles(false, animated)
        return true
    }

    companion object {
        private var watching = false

        /**
         * Nagram's own ghost mode screen, which its drawer opens too, only says that the user's info changed
         * when it turns ghost mode on or off. Starts listening for that from the chat list, on the UI thread.
         */
        private fun watchNagram() {
            if (watching) {
                return
            }
            watching = true
            var shown = NagramGhost.isActive
            val observer = NotificationCenter.NotificationCenterDelegate { _, _, _ ->
                val active = NagramGhost.isActive
                if (active != shown) {
                    shown = active
                    refresh()
                }
            }
            for (account in 0 until UserConfig.MAX_ACCOUNT_COUNT) {
                NotificationCenter.getInstance(account).addObserver(observer, NotificationCenter.mainUserInfoChanged)
            }
        }

        /** Has every chat list redraw its title status, the way Telegram does when the emoji status changes. */
        fun refresh() {
            AndroidUtilities.runOnUIThread {
                for (account in 0 until UserConfig.MAX_ACCOUNT_COUNT) {
                    val user = UserConfig.getInstance(account).currentUser ?: continue
                    NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.userEmojiStatusUpdated, user)
                }
            }
        }
    }
}
