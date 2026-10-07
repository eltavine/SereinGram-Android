package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/** What Telegram's message notifications show. */
public object NotificationHooks {
    public fun interface LockedContentPolicy {
        public fun showsContentWhenLocked(): Boolean
    }

    public val lockedContentPolicies: Handlers<LockedContentPolicy> = Handlers()

    /** Whether notifications show senders and text while a passcode locks the app. */
    @JvmStatic
    public fun showsContentWhenLocked(): Boolean =
        lockedContentPolicies.all.any { policy ->
            Faults.guard("locked notification policy", fallback = false) { policy.showsContentWhenLocked() }
        }
}
