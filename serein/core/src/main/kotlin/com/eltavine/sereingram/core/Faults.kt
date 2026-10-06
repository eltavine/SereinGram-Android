package com.eltavine.sereingram.core

/**
 * Failures of SereinGram code that run inside upstream code paths. A handler
 * that throws, for example after an upstream change it was not written for,
 * is reported and the hook falls back to upstream behavior.
 */
public object Faults {
    public fun interface Reporter {
        public fun report(message: String, error: Throwable)
    }

    public val reporters: Handlers<Reporter> = Handlers()

    public fun report(message: String, error: Throwable) {
        reporters.all.forEach { reporter ->
            try {
                reporter.report(message, error)
            } catch (_: Exception) {
            }
        }
    }

    /** Runs [block]; when it throws anything but a VM error, reports it and returns [fallback]. */
    public inline fun <T> guard(what: String, fallback: T, block: () -> T): T = try {
        block()
    } catch (error: Throwable) {
        if (error is VirtualMachineError) {
            throw error
        }
        report(what, error)
        fallback
    }
}
