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

    public val timeDecorators: Handlers<TimeDecorator> = Handlers()

    /** Telegram's time text, passed through every installed decorator in turn. */
    @JvmStatic
    public fun decorateTime(account: Int, message: Any, time: String): String =
        timeDecorators.all.fold(time) { current, decorator ->
            Faults.guard("message time decorator", fallback = current) {
                decorator.decorate(account, message, current)
            }
        }
}
