package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/** How Telegram draws a message bubble. */
public object MessageHooks {
    public fun interface TimeDecorator {
        /** Returns the text drawn where Telegram shows a message's time, given what Telegram would show. */
        public fun decorate(account: Int, dialogId: Long, messageId: Int, time: String): String
    }

    public val timeDecorators: Handlers<TimeDecorator> = Handlers()

    /** Telegram's time text, passed through every installed decorator in turn. */
    @JvmStatic
    public fun decorateTime(account: Int, dialogId: Long, messageId: Int, time: String): String =
        timeDecorators.all.fold(time) { current, decorator ->
            Faults.guard("message time decorator", fallback = current) {
                decorator.decorate(account, dialogId, messageId, current)
            }
        }
}
