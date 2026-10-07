package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/** How Telegram searches from the chat list. */
public object SearchHooks {
    public fun interface GlobalSearchPolicy {
        public fun allowsGlobalSearch(): Boolean
    }

    public fun interface AppsTabPolicy {
        public fun showsAppsTab(): Boolean
    }

    public val globalSearchPolicies: Handlers<GlobalSearchPolicy> = Handlers()
    public val appsTabPolicies: Handlers<AppsTabPolicy> = Handlers()

    /** Whether the chat list search lists public chats and people the user has no part in. */
    @JvmStatic
    public fun allowsGlobalSearch(): Boolean =
        globalSearchPolicies.all.all { policy ->
            Faults.guard("global search policy", fallback = true) { policy.allowsGlobalSearch() }
        }

    /** Whether the chat list search has its tab of mini apps and bots. */
    @JvmStatic
    public fun showsAppsTab(): Boolean =
        appsTabPolicies.all.all { policy -> Faults.guard("apps tab policy", fallback = true) { policy.showsAppsTab() } }
}
