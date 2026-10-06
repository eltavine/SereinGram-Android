package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GuardedHooksTest {
    @Test
    fun throwingHandlersFallBackToUpstreamBehavior() {
        val reported = mutableListOf<String>()
        val installs = listOf(
            Faults.reporters.install { message, _ -> reported += message },
            UpstreamServices.policies.install { throw IllegalStateException() },
            ChatHooks.shareButtonPolicies.install { _, _, _ -> throw ClassCastException() },
            SettingsHooks.mainFilters.install { throw IndexOutOfBoundsException() },
        )
        try {
            assertTrue(UpstreamServices.allowUpdateCheck())
            assertTrue(ChatHooks.allowShareButton(account = 0, dialogId = 1, saved = false))
            val items = mutableListOf<Any?>("row")
            SettingsHooks.filterMainSettings(items)
            assertEquals(listOf<Any?>("row"), items)
        } finally {
            installs.forEach(AutoCloseable::close)
        }
        assertEquals(listOf("upstream service policy", "share button policy", "main settings filter"), reported)
    }
}
