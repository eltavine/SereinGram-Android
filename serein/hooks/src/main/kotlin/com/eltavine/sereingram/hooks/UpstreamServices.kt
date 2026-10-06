package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/** Services that Nagram runs for its own releases, which upstream code reaches on its own. */
public enum class UpstreamService {
    /** Nagram's Sentry project, which receives crash and ANR reports. */
    CRASH_REPORTS,

    /** Nagram's release channel, which offers Nagram's APKs as updates. */
    UPDATE_CHECK,
}

/**
 * Whether upstream code may contact a Nagram service. Every service is
 * allowed, as in Nagram, until an installed policy refuses it.
 */
public object UpstreamServices {
    public fun interface Policy {
        public fun allow(service: UpstreamService): Boolean
    }

    public val policies: Handlers<Policy> = Handlers()

    @JvmStatic
    public fun allowCrashReports(): Boolean = allow(UpstreamService.CRASH_REPORTS)

    @JvmStatic
    public fun allowUpdateCheck(): Boolean = allow(UpstreamService.UPDATE_CHECK)

    private fun allow(service: UpstreamService): Boolean = policies.all.all { policy ->
        Faults.guard("upstream service policy", fallback = true) { policy.allow(service) }
    }
}
