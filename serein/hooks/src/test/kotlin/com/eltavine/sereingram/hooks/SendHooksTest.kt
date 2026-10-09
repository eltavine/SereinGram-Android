package com.eltavine.sereingram.hooks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun aConfirmerHoldsBackAGifUntilTheUserAgrees() {
        val sent = mutableListOf<String>()
        val asked = mutableListOf<Runnable>()
        val installs = listOf(
            SendHooks.confirmers.install { _, _, _ -> throw IllegalStateException() },
            SendHooks.confirmers.install { account, _, send -> (account == 0).also { if (it) asked += send } },
        )
        try {
            assertFalse(SendHooks.asksBeforeSendingGif(1, "chat", { sent += "unasked" }))
            assertTrue(SendHooks.asksBeforeSendingGif(0, "chat", { sent += "gif" }))
            assertEquals(emptyList(), sent)
            asked.single().run()
            assertEquals(listOf("gif"), sent)
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }

    @Test
    fun nothingIsAskedOutsideAChat() {
        SendHooks.confirmers.install { _, _, _ -> true }.use {
            assertFalse(SendHooks.asksBeforeSendingGif(0, null, {}))
        }
    }

    @Test
    fun schedulersMoveForwardsAndABrokenOneLeavesThemAlone() {
        assertEquals(0, SendHooks.forwardScheduleDate(0, 42, 0))
        val installs = listOf(
            SendHooks.forwardSchedulers.install { _, _, _ -> throw IllegalStateException() },
            SendHooks.forwardSchedulers.install { _, peer, date -> if (date == 0 && peer == 42L) 1_012 else date },
        )
        try {
            assertEquals(1_012, SendHooks.forwardScheduleDate(0, 42, 0))
            assertEquals(5_000, SendHooks.forwardScheduleDate(0, 42, 5_000))
            assertEquals(0, SendHooks.forwardScheduleDate(0, 7, 0))
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }
}
