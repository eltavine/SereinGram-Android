package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/** How Telegram draws a message bubble. */
public object MessageHooks {
    public fun interface TimeDecorator {
        /**
         * Returns the text drawn where Telegram shows a message's time, given what
         * Telegram would show; [message] is Telegram's object for the message.
         */
        public fun decorate(account: Int, message: Any, time: String): String
    }

    public fun interface ReadReceiptPolicy {
        /** Whether the second check of the read outgoing [message] is left out of its bubble and its chat. */
        public fun hidesReadReceipt(account: Int, message: Any): Boolean
    }

    public val timeDecorators: Handlers<TimeDecorator> = Handlers()
    public val readReceiptPolicies: Handlers<ReadReceiptPolicy> = Handlers()

    @JvmStatic
    public fun hidesReadReceipt(account: Int, message: Any): Boolean =
        readReceiptPolicies.all.any { policy ->
            Faults.guard("read receipt policy", fallback = false) { policy.hidesReadReceipt(account, message) }
        }

    /** Telegram's time text, passed through every installed decorator in turn. */
    @JvmStatic
    public fun decorateTime(account: Int, message: Any, time: String): String =
        timeDecorators.all.fold(time) { current, decorator ->
            Faults.guard("message time decorator", fallback = current) {
                decorator.decorate(account, message, current)
            }
        }
}
