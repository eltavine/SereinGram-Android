package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/** How Telegram draws the chat list. */
public object DialogsHooks {
    public fun interface TitleStatus {
        /**
         * May draw into [slot], Telegram's status drawable beside the chat list
         * title where the emoji status goes; true when it did and keeps the slot.
         */
        public fun draw(account: Int, slot: Any, animated: Boolean): Boolean
    }

    public val titleStatuses: Handlers<TitleStatus> = Handlers()

    /** True when a module drew the status beside the title instead of Telegram. */
    @JvmStatic
    public fun drawTitleStatus(account: Int, slot: Any, animated: Boolean): Boolean =
        titleStatuses.all.any { status ->
            Faults.guard("title status", fallback = false) { status.draw(account, slot, animated) }
        }
}
