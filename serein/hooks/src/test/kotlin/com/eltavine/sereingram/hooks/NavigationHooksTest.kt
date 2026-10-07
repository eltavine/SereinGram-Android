package com.eltavine.sereingram.hooks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigationHooksTest {
    @Test
    fun screensOpenUnlessAGuardHoldsThemBack() {
        assertTrue(NavigationHooks.allowsPresenting("chat", preview = false) {})
        val installs = listOf(
            NavigationHooks.guards.install { _, _, _ -> throw IllegalStateException() },
            NavigationHooks.guards.install { screen, _, _ -> screen != "locked chat" },
        )
        try {
            assertTrue(NavigationHooks.allowsPresenting("chat", preview = false) {})
            assertFalse(NavigationHooks.allowsPresenting("locked chat", preview = false) {})
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }

    @Test
    fun aGuardThatRefusesCanOpenTheScreenLater() {
        var pending: Runnable? = null
        val opened = mutableListOf<String>()
        val install = NavigationHooks.guards.install { _, preview, retry ->
            if (!preview) pending = retry
            false
        }
        try {
            assertFalse(NavigationHooks.allowsPresenting("locked chat", preview = false) { opened += "locked chat" })
            assertFalse(NavigationHooks.allowsPresenting("peek", preview = true) { opened += "peek" })
            pending?.run()
            assertEquals(listOf("locked chat"), opened)
        } finally {
            install.close()
        }
    }
}
