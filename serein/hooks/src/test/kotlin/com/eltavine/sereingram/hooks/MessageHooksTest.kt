package com.eltavine.sereingram.hooks

import kotlin.test.Test
import kotlin.test.assertEquals

class MessageHooksTest {
    @Test
    fun decoratorsApplyInOrderAndABrokenOneIsSkipped() {
        val installs = listOf(
            MessageHooks.timeDecorators.install { _, message, time -> if (message == "kept") "deleted $time" else time },
            MessageHooks.timeDecorators.install { _, _, _ -> throw IllegalStateException() },
            MessageHooks.timeDecorators.install { _, _, time -> "$time!" },
        )
        try {
            assertEquals("deleted 12:00!", MessageHooks.decorateTime(account = 0, message = "kept", time = "12:00"))
            assertEquals("12:00!", MessageHooks.decorateTime(account = 0, message = "other", time = "12:00"))
        } finally {
            installs.forEach(AutoCloseable::close)
        }
        assertEquals("12:00", MessageHooks.decorateTime(account = 0, message = "kept", time = "12:00"))
    }

    @Test
    fun readReceiptsShowUnlessAPolicyHidesThem() {
        assertEquals(false, MessageHooks.hidesReadReceipt(0, "message"))
        val installs = listOf(
            MessageHooks.readReceiptPolicies.install { _, _ -> throw IllegalStateException() },
            MessageHooks.readReceiptPolicies.install { account, _ -> account == 1 },
        )
        try {
            assertEquals(false, MessageHooks.hidesReadReceipt(0, "message"))
            assertEquals(true, MessageHooks.hidesReadReceipt(1, "message"))
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }

    @Test
    fun loadListenersMayAddToTheBatch() {
        val install = HistoryHooks.loadListeners.install { _, _, _, _, messages, _, _ -> messages += "restored" }
        try {
            val messages = mutableListOf<Any?>("loaded")
            HistoryHooks.afterHistoryLoaded(0, 1, 0, 0, messages, mutableListOf<Any?>(), mutableListOf<Any?>())
            assertEquals(listOf<Any?>("loaded", "restored"), messages)
        } finally {
            install.close()
        }
    }
}
