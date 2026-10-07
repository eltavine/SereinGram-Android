package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/** How Telegram's message and caption fields take input. */
public object InputHooks {
    public fun interface PastePolicy {
        public fun pastesPlainText(): Boolean
    }

    public val pastePolicies: Handlers<PastePolicy> = Handlers()

    /** Whether pasting drops the formatting of what was copied. */
    @JvmStatic
    public fun pastesPlainText(): Boolean =
        pastePolicies.all.any { policy -> Faults.guard("paste policy", fallback = false) { policy.pastesPlainText() } }
}
