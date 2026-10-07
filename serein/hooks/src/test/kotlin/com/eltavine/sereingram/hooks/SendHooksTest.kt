package com.eltavine.sereingram.hooks

import kotlin.test.Test
import kotlin.test.assertEquals

class SendHooksTest {
    private class Message(var scheduleDate: Int = 0)

    @Test
    fun rewritersChangeAMessageBeforeItGoesOut() {
        val message = Message()
        val installs = listOf(
            SendHooks.rewriters.install { _, _ -> throw IllegalStateException() },
            SendHooks.rewriters.install { account, m -> (m as Message).scheduleDate = 1_000 + account },
        )
        try {
            SendHooks.beforeSend(2, message)
            assertEquals(1_002, message.scheduleDate)
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }
}
