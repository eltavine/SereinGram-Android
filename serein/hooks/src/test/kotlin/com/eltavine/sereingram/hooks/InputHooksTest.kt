package com.eltavine.sereingram.hooks

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InputHooksTest {
    @Test
    fun pastingKeepsFormattingUntilAPolicyAsksForPlainText() {
        assertFalse(InputHooks.pastesPlainText())
        val installs = listOf(
            InputHooks.pastePolicies.install { throw IllegalStateException() },
            InputHooks.pastePolicies.install { true },
        )
        try {
            assertTrue(InputHooks.pastesPlainText())
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }
}
