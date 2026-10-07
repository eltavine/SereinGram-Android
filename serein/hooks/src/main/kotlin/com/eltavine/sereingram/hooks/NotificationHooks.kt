package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/** What Telegram's message notifications show and send. */
public object NotificationHooks {
    public fun interface LockedContentPolicy {
        public fun showsContentWhenLocked(): Boolean
    }

    public fun interface ReplyPolicy {
        /** Whether an answer typed into a notification goes out as a plain message rather than a reply. */
        public fun sendsPlainAnswers(): Boolean
    }

    public val lockedContentPolicies: Handlers<LockedContentPolicy> = Handlers()
    public val replyPolicies: Handlers<ReplyPolicy> = Handlers()

    /** Whether notifications show senders and text while a passcode locks the app. */
    @JvmStatic
    public fun showsContentWhenLocked(): Boolean =
        lockedContentPolicies.all.any { policy ->
            Faults.guard("locked notification policy", fallback = false) { policy.showsContentWhenLocked() }
        }

    /** Telegram's answers from notifications reply to the latest message unless a policy says otherwise. */
    @JvmStatic
    public fun sendsPlainAnswers(): Boolean =
        replyPolicies.all.any { policy -> Faults.guard("notification reply policy", fallback = false) { policy.sendsPlainAnswers() } }
}
