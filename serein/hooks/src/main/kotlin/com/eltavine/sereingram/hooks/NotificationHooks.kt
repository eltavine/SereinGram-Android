package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/** What Telegram's message notifications show and send. */
public object NotificationHooks {
    public fun interface LockedContentPolicy {
        public fun showsContentWhenLocked(): Boolean
    }

    public fun interface ContentPolicy {
        /** Whether notifications of [dialogId] keep its messages, senders and pictures to themselves, and open no popup or bubble. */
        public fun hidesContent(account: Int, dialogId: Long): Boolean
    }

    public fun interface ReplyPolicy {
        /** Whether an answer typed into a notification goes out as a plain message rather than a reply. */
        public fun sendsPlainAnswers(): Boolean
    }

    public val lockedContentPolicies: Handlers<LockedContentPolicy> = Handlers()
    public val contentPolicies: Handlers<ContentPolicy> = Handlers()
    public val replyPolicies: Handlers<ReplyPolicy> = Handlers()

    /** Whether notifications show senders and text while a passcode locks the app. */
    @JvmStatic
    public fun showsContentWhenLocked(): Boolean =
        lockedContentPolicies.all.any { policy ->
            Faults.guard("locked notification policy", fallback = false) { policy.showsContentWhenLocked() }
        }

    /** Whether notifications of [dialogId] show nothing of it; a policy that throws hides it. */
    @JvmStatic
    public fun hidesContent(account: Int, dialogId: Long): Boolean =
        contentPolicies.all.any { policy -> Faults.guard("notification content policy", fallback = true) { policy.hidesContent(account, dialogId) } }

    /**
     * Whether a notification of [dialogId] says only that there is a new message: while a passcode
     * locks the app, as Telegram does unless a policy shows content then, and for chats a policy hides.
     */
    @JvmStatic
    public fun hidesText(account: Int, dialogId: Long, passcodeLocked: Boolean): Boolean =
        passcodeLocked && !showsContentWhenLocked() || hidesContent(account, dialogId)

    /** Telegram's answers from notifications reply to the latest message unless a policy says otherwise. */
    @JvmStatic
    public fun sendsPlainAnswers(): Boolean =
        replyPolicies.all.any { policy -> Faults.guard("notification reply policy", fallback = false) { policy.sendsPlainAnswers() } }
}
