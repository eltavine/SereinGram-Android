package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/** Messages as Telegram starts sending them. */
public object SendHooks {
    public fun interface Rewriter {
        /** May change [message], Telegram's parameters for one message, before it is built and sent. */
        public fun beforeSend(account: Int, message: Any)
    }

    /** What is about to be sent with a single tap in the sticker and GIF panel. */
    public enum class Tapped { STICKER, GIF }

    public fun interface Confirmer {
        /** Whether it asked the user, in the chat [host]; if it did, it runs [send] once they agree. */
        public fun asks(account: Int, tapped: Tapped, host: Any, send: Runnable): Boolean
    }

    public val rewriters: Handlers<Rewriter> = Handlers()
    public val confirmers: Handlers<Confirmer> = Handlers()

    @JvmStatic
    public fun beforeSend(account: Int, message: Any) {
        rewriters.all.forEach { rewriter ->
            Faults.guard("send rewriter", fallback = Unit) { rewriter.beforeSend(account, message) }
        }
    }

    /** Whether sending a sticker waits for the user; Telegram sends right away when this is false. */
    @JvmStatic
    public fun asksBeforeSendingSticker(account: Int, host: Any?, send: Runnable): Boolean = asks(account, Tapped.STICKER, host, send)

    /** Whether sending a GIF waits for the user; Telegram sends right away when this is false. */
    @JvmStatic
    public fun asksBeforeSendingGif(account: Int, host: Any?, send: Runnable): Boolean = asks(account, Tapped.GIF, host, send)

    private fun asks(account: Int, tapped: Tapped, host: Any?, send: Runnable): Boolean =
        host != null &&
            confirmers.all.any { confirmer ->
                Faults.guard("send confirmer", fallback = false) { confirmer.asks(account, tapped, host, send) }
            }
}
