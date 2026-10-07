package com.eltavine.sereingram.hooks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DialogsHooksTest {
    @Test
    fun theFirstModuleThatDrawsKeepsTheSlot() {
        val drawn = mutableListOf<String>()
        assertFalse(DialogsHooks.drawTitleStatus(0, "slot", animated = false))
        val installs = listOf(
            DialogsHooks.titleStatuses.install { _, _, _ -> throw IllegalStateException() },
            DialogsHooks.titleStatuses.install { _, _, _ -> false },
            DialogsHooks.titleStatuses.install { _, slot, _ ->
                drawn += "ghost in $slot"
                true
            },
            DialogsHooks.titleStatuses.install { _, _, _ ->
                drawn += "never"
                true
            },
        )
        try {
            assertTrue(DialogsHooks.drawTitleStatus(0, "slot", animated = true))
            assertEquals(listOf("ghost in slot"), drawn)
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }
}
