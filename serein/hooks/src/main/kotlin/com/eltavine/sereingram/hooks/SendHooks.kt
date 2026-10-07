package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/** Messages as Telegram starts sending them. */
public object SendHooks {
    public fun interface Rewriter {
        /** May change [message], Telegram's parameters for one message, before it is built and sent. */
        public fun beforeSend(account: Int, message: Any)
    }

    public val rewriters: Handlers<Rewriter> = Handlers()

    @JvmStatic
    public fun beforeSend(account: Int, message: Any) {
        rewriters.all.forEach { rewriter ->
            Faults.guard("send rewriter", fallback = Unit) { rewriter.beforeSend(account, message) }
        }
    }
}
