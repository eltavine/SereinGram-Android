package com.eltavine.sereingram.hooks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RequestHooksTest {
    private class Request(var counted: Boolean)

    @Test
    fun requestsGoOutUnlessAnInterceptorDropsThem() {
        val request = Request(counted = true)
        val installs = listOf(
            RequestHooks.interceptors.install { _, _ -> throw IllegalStateException() },
            RequestHooks.interceptors.install { _, r ->
                (r as Request).counted = false
                true
            },
        )
        try {
            assertTrue(RequestHooks.intercept(0, request))
            assertEquals(false, request.counted)
            val drop = RequestHooks.interceptors.install { _, _ -> false }
            try {
                assertFalse(RequestHooks.intercept(0, request))
            } finally {
                drop.close()
            }
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }

    @Test
    fun anyExemptionLetsARequestPastGhostMode() {
        assertFalse(RequestHooks.exemptsFromGhostMode(0, "read"))
        val installs = listOf(
            RequestHooks.ghostExemptions.install { _, _ -> throw IllegalStateException() },
            RequestHooks.ghostExemptions.install { account, request -> account == 1 && request == "read" },
        )
        try {
            assertTrue(RequestHooks.exemptsFromGhostMode(1, "read"))
            assertFalse(RequestHooks.exemptsFromGhostMode(0, "read"))
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }
}
