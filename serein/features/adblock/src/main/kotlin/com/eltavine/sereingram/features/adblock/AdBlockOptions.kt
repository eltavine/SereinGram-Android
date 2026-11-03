package com.eltavine.sereingram.features.adblock

import com.eltavine.sereingram.core.*

public object AdBlockOptions {
    public val enabled: Option<Boolean> = booleanOption("adblock_enabled", scope = OptionScope.ACCOUNT)
    public val bundled: Option<Boolean> = booleanOption("adblock_bundled", default = true, scope = OptionScope.ACCOUNT)
    public val subscription: Option<String> = textOption("adblock_subscription", default = AdRules.RECOMMENDED_URL, scope = OptionScope.ACCOUNT)
    public val lastUpdate: Option<Long> = longOption("adblock_last_update", scope = OptionScope.ACCOUNT, backedUp = false)
    public val all: List<Option<*>> = listOf(enabled, bundled, subscription, lastUpdate)
}
