package com.eltavine.sereingram.hooks

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigationHooksTest {
    @Test
    fun screensOpenUnlessAGuardHoldsThemBack() {
        assertTrue(NavigationHooks.allowsPresenting("layout", "chat", "params"))
        val installs = listOf(
            NavigationHooks.guards.install { _, _, _ -> throw IllegalStateException() },
            NavigationHooks.guards.install { _, screen, _ -> screen != "locked chat" },
        )
        try {
            assertTrue(NavigationHooks.allowsPresenting("layout", "chat", "params"))
            assertFalse(NavigationHooks.allowsPresenting("layout", "locked chat", "params"))
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }
}
