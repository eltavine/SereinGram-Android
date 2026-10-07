package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/**
 * Telegram's API requests on their way out, on its stage queue right before
 * they are serialized. Requests are Telegram's TL objects. Handlers run for
 * every request, so they must not block.
 */
public object RequestHooks {
    public fun interface Interceptor {
        /** Returns false to drop [request]; it may also change [request] before it goes out. */
        public fun intercept(account: Int, request: Any): Boolean
    }

    public fun interface GhostExemption {
        /** Whether [request] goes out although Nagram's ghost mode would hold it back. */
        public fun exempts(account: Int, request: Any): Boolean
    }

    public val interceptors: Handlers<Interceptor> = Handlers()
    public val ghostExemptions: Handlers<GhostExemption> = Handlers()

    /** False drops the request. An interceptor that throws lets it through. */
    @JvmStatic
    public fun intercept(account: Int, request: Any): Boolean =
        interceptors.all.all { interceptor ->
            Faults.guard("request interceptor", fallback = true) { interceptor.intercept(account, request) }
        }

    @JvmStatic
    public fun exemptsFromGhostMode(account: Int, request: Any): Boolean =
        ghostExemptions.all.any { exemption ->
            Faults.guard("ghost exemption", fallback = false) { exemption.exempts(account, request) }
        }
}
