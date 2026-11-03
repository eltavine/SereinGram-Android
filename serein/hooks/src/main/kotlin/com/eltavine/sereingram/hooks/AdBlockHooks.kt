package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

public object AdBlockHooks {
    public fun interface Filter {
        public fun hides(account: Int, message: Any, album: Any?): Boolean
    }
    public val filters: Handlers<Filter> = Handlers()
    public fun interface SponsoredPolicy { public fun blocks(account: Int): Boolean }
    public val sponsoredPolicies: Handlers<SponsoredPolicy> = Handlers()
    @JvmStatic
    public fun blocksSponsored(account: Int): Boolean = sponsoredPolicies.all.any {
        Faults.guard("sponsored advertising", fallback = false) { it.blocks(account) }
    }
    @JvmStatic
    public fun hides(account: Int, message: Any?, album: Any?): Boolean = message != null && filters.all.any {
        Faults.guard("advertising filter", fallback = false) { it.hides(account, message, album) }
    }
}
