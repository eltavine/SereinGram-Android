package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

public object JoinHooks {
    public fun interface ErrorHandler {
        public fun handle(account: Int, request: Any, error: String, host: Any?): Boolean
    }
    public val handlers: Handlers<ErrorHandler> = Handlers()
    @JvmStatic
    public fun handlesError(account: Int, request: Any?, error: String, host: Any?): Boolean = request != null && handlers.all.any {
        Faults.guard("join error", fallback = false) { it.handle(account, request, error, host) }
    }
}
