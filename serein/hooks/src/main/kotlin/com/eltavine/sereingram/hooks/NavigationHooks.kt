package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/** Screens as Telegram is about to open them. */
public object NavigationHooks {
    public fun interface Guard {
        /**
         * False keeps [screen], Telegram's fragment, from opening for now. [preview] is true for a
         * peek that opens nothing for good. A guard that refuses may run [retry] later, for example
         * once the user unlocked the screen, which opens it the way it was asked to open.
         */
        public fun allows(screen: Any, preview: Boolean, retry: Runnable): Boolean
    }

    public val guards: Handlers<Guard> = Handlers()

    /** Whether [screen] opens now. A guard that throws lets it open. */
    @JvmStatic
    public fun allowsPresenting(screen: Any, preview: Boolean, retry: Runnable): Boolean =
        guards.all.all { guard -> Faults.guard("navigation guard", fallback = true) { guard.allows(screen, preview, retry) } }
}
