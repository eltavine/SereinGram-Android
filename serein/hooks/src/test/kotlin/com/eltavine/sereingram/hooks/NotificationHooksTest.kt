package com.eltavine.sereingram.hooks

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationHooksTest {
    @Test
    fun lockedNotificationsStayHiddenUntilAPolicyShowsThem() {
        assertFalse(NotificationHooks.showsContentWhenLocked())
        val installs = listOf(
            NotificationHooks.lockedContentPolicies.install { throw IllegalStateException() },
            NotificationHooks.lockedContentPolicies.install { true },
        )
        try {
            assertTrue(NotificationHooks.showsContentWhenLocked())
        } finally {
            installs.forEach(AutoCloseable::close)
        }
        assertFalse(NotificationHooks.showsContentWhenLocked())
    }
}
