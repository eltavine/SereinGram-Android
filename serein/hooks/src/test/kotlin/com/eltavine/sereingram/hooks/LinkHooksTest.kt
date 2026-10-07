package com.eltavine.sereingram.hooks

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LinkHooksTest {
    @Test
    fun linksOpenAsTelegramDecidesUntilAPolicyConfirmsThem() {
        assertFalse(LinkHooks.confirmsBeforeOpening("https://example.com"))
        val installs = listOf(
            LinkHooks.confirmPolicies.install { throw IllegalStateException() },
            LinkHooks.confirmPolicies.install { url -> url.startsWith("https://") },
        )
        try {
            assertTrue(LinkHooks.confirmsBeforeOpening("https://example.com"))
            assertFalse(LinkHooks.confirmsBeforeOpening("ftp://example.com"))
            assertFalse(LinkHooks.confirmsBeforeOpening(null))
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }
}
