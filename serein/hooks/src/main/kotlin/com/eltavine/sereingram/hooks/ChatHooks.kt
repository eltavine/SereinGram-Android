package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Handlers

/** Decisions Telegram's chat screen makes per message. */
public object ChatHooks {
    public fun interface ShareButtonPolicy {
        /** [saved] is true for messages in Saved Messages, whose button opens the original chat. */
        public fun allow(account: Int, dialogId: Long, saved: Boolean): Boolean
    }

    public val shareButtonPolicies: Handlers<ShareButtonPolicy> = Handlers()

    /** Whether the round share button may be drawn beside a message; true unless a policy refuses. */
    @JvmStatic
    public fun allowShareButton(account: Int, dialogId: Long, saved: Boolean): Boolean =
        shareButtonPolicies.all.all { it.allow(account, dialogId, saved) }
}
