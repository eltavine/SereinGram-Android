package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/** Decisions Telegram's chat screen makes per message and about its bars. */
public object ChatHooks {
    public fun interface ShareButtonPolicy {
        /** [saved] is true for messages in Saved Messages, whose button opens the original chat. */
        public fun allow(account: Int, dialogId: Long, saved: Boolean): Boolean
    }

    public fun interface ChannelButtonPolicy {
        /** [button] is one of the button ids of Telegram's `ChatActivityChannelButtonsLayout`. */
        public fun allow(button: Int): Boolean
    }

    public val shareButtonPolicies: Handlers<ShareButtonPolicy> = Handlers()
    public val channelButtonPolicies: Handlers<ChannelButtonPolicy> = Handlers()

    /** Whether the round share button may be drawn beside a message; true unless a policy refuses. */
    @JvmStatic
    public fun allowShareButton(account: Int, dialogId: Long, saved: Boolean): Boolean =
        shareButtonPolicies.all.all { policy ->
            Faults.guard("share button policy", fallback = true) { policy.allow(account, dialogId, saved) }
        }

    /** Whether a button of the bar under a channel may be shown; true unless a policy refuses. */
    @JvmStatic
    public fun allowsChannelButton(button: Int): Boolean =
        channelButtonPolicies.all.all { policy -> Faults.guard("channel button policy", fallback = true) { policy.allow(button) } }
}
