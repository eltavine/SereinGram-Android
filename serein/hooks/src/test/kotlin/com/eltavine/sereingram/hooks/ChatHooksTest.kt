package com.eltavine.sereingram.hooks

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatHooksTest {
    @Test
    fun theShareButtonStaysUntilAPolicyRefusesIt() {
        assertTrue(ChatHooks.allowShareButton(account = 0, dialogId = -100, saved = false))
        val onlySaved = ChatHooks.shareButtonPolicies.install { _, _, saved -> saved }
        try {
            assertFalse(ChatHooks.allowShareButton(account = 0, dialogId = -100, saved = false))
            assertTrue(ChatHooks.allowShareButton(account = 0, dialogId = 1, saved = true))
        } finally {
            onlySaved.close()
        }
    }

    @Test
    fun channelButtonsShowUntilAPolicyRefusesThem() {
        assertTrue(ChatHooks.allowsChannelButton(1))
        val installs = listOf(
            ChatHooks.channelButtonPolicies.install { throw IllegalStateException() },
            ChatHooks.channelButtonPolicies.install { button -> button != 1 },
        )
        try {
            assertFalse(ChatHooks.allowsChannelButton(1))
            assertTrue(ChatHooks.allowsChannelButton(2))
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }
}
