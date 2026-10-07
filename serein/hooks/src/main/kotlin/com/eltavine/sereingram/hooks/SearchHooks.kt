package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/** How Telegram searches from the chat list. */
public object SearchHooks {
    public fun interface GlobalSearchPolicy {
        public fun allowsGlobalSearch(): Boolean
    }

    public val globalSearchPolicies: Handlers<GlobalSearchPolicy> = Handlers()

    /** Whether the chat list search lists public chats and people the user has no part in. */
    @JvmStatic
    public fun allowsGlobalSearch(): Boolean =
        globalSearchPolicies.all.all { policy ->
            Faults.guard("global search policy", fallback = true) { policy.allowsGlobalSearch() }
        }
}
