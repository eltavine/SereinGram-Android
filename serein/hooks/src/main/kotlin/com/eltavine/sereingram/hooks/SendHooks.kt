package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/** Messages as Telegram starts sending them. */
public object SendHooks {
    public fun interface Rewriter {
        /** May change [message], Telegram's parameters for one message, before it is built and sent. */
        public fun beforeSend(account: Int, message: Any)
    }

    public fun interface ForwardScheduler {
        /** The date to schedule a forward to [peer] for, given the one Telegram has; 0 forwards right away. */
        public fun scheduleDate(account: Int, peer: Long, scheduleDate: Int): Int
    }

    public fun interface Confirmer {
        /** Whether it asked the user, in the chat [host], before a GIF tapped in the GIF panel goes out; if it did, it runs [send] once they agree. */
        public fun asks(account: Int, host: Any, send: Runnable): Boolean
    }

    public val rewriters: Handlers<Rewriter> = Handlers()
    public val forwardSchedulers: Handlers<ForwardScheduler> = Handlers()
    public val confirmers: Handlers<Confirmer> = Handlers()

    @JvmStatic
    public fun beforeSend(account: Int, message: Any) {
        rewriters.all.forEach { rewriter ->
            Faults.guard("send rewriter", fallback = Unit) { rewriter.beforeSend(account, message) }
        }
    }

    /** When forwarded messages go out: at [scheduleDate], unless a scheduler moves them. */
    @JvmStatic
    public fun forwardScheduleDate(account: Int, peer: Long, scheduleDate: Int): Int =
        forwardSchedulers.all.fold(scheduleDate) { date, scheduler ->
            Faults.guard("forward scheduler", fallback = date) { scheduler.scheduleDate(account, peer, date) }
        }

    /** Whether sending a GIF waits for the user; Telegram sends right away when this is false. */
    @JvmStatic
    public fun asksBeforeSendingGif(account: Int, host: Any?, send: Runnable): Boolean =
        host != null &&
            confirmers.all.any { confirmer ->
                Faults.guard("send confirmer", fallback = false) { confirmer.asks(account, host, send) }
            }
}
