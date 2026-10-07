package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/** How Telegram opens the links people tap. */
public object LinkHooks {
    public fun interface ConfirmPolicy {
        public fun confirms(url: String): Boolean
    }

    public val confirmPolicies: Handlers<ConfirmPolicy> = Handlers()

    /**
     * Whether Telegram shows [url] and asks before opening it, on top of the
     * links it asks about itself. Links to Telegram itself still open directly.
     */
    @JvmStatic
    public fun confirmsBeforeOpening(url: String?): Boolean =
        url != null && confirmPolicies.all.any { policy ->
            Faults.guard("link confirm policy", fallback = false) { policy.confirms(url) }
        }
}
