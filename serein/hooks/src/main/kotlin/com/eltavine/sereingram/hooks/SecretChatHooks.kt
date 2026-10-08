package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/** Secret chats, whose service messages Telegram encrypts before any request could show what they say. */
public object SecretChatHooks {
    public fun interface ReadPolicy {
        public fun sendsReadReceipt(account: Int, dialogId: Long): Boolean
    }

    public val readPolicies: Handlers<ReadPolicy> = Handlers()

    /**
     * Whether the secret chat [dialogId] is told that messages were read,
     * which also starts the timers of its self-destructing ones there.
     * A policy that throws lets it be told.
     */
    @JvmStatic
    public fun sendsReadReceipt(account: Int, dialogId: Long): Boolean =
        readPolicies.all.all { policy ->
            Faults.guard("secret chat read policy", fallback = true) { policy.sendsReadReceipt(account, dialogId) }
        }
}
