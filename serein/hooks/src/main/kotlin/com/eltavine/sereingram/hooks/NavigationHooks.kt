package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/** Screens as Telegram is about to open them. */
public object NavigationHooks {
    public fun interface Guard {
        /**
         * False keeps [screen], Telegram's fragment, from opening for now; [layout]
         * and [params], Telegram's own, are what it takes to open it later.
         */
        public fun allows(layout: Any, screen: Any, params: Any): Boolean
    }

    public val guards: Handlers<Guard> = Handlers()

    /** Whether [screen] opens now. A guard that throws lets it open. */
    @JvmStatic
    public fun allowsPresenting(layout: Any, screen: Any, params: Any): Boolean =
        guards.all.all { guard -> Faults.guard("navigation guard", fallback = true) { guard.allows(layout, screen, params) } }
}
