package com.eltavine.sereingram.hooks

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecretChatHooksTest {
    @Test
    fun anyPolicyCanKeepAReadBackAndABrokenOneCannot() {
        assertTrue(SecretChatHooks.sendsReadReceipt(0, 5))
        val installs = listOf(
            SecretChatHooks.readPolicies.install { _, _ -> throw IllegalStateException() },
            SecretChatHooks.readPolicies.install { _, dialogId -> dialogId != 5L },
        )
        try {
            assertFalse(SecretChatHooks.sendsReadReceipt(0, 5))
            assertTrue(SecretChatHooks.sendsReadReceipt(0, 6))
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }
}
