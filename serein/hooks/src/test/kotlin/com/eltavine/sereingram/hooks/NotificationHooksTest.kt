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

    @Test
    fun textIsHiddenWhileThePasscodeLocksTheAppUnlessShownAndAlwaysForHiddenChats() {
        assertTrue(NotificationHooks.hidesText(0, 7, passcodeLocked = true))
        assertFalse(NotificationHooks.hidesText(0, 7, passcodeLocked = false))
        val installs = listOf(
            NotificationHooks.lockedContentPolicies.install { true },
            NotificationHooks.contentPolicies.install { _, dialogId -> dialogId == -100L },
        )
        try {
            assertFalse(NotificationHooks.hidesText(0, 7, passcodeLocked = true))
            assertTrue(NotificationHooks.hidesText(0, -100, passcodeLocked = false))
            assertTrue(NotificationHooks.hidesText(0, -100, passcodeLocked = true))
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }

    @Test
    fun aContentPolicyThatThrowsHidesTheChat() {
        NotificationHooks.contentPolicies.install { _, _ -> throw IllegalStateException() }.use {
            assertTrue(NotificationHooks.hidesContent(0, 7))
        }
        assertFalse(NotificationHooks.hidesContent(0, 7))
    }

    @Test
    fun answersReplyUntilAPolicyMakesThemPlain() {
        assertFalse(NotificationHooks.sendsPlainAnswers())
        val installs = listOf(
            NotificationHooks.replyPolicies.install { throw IllegalStateException() },
            NotificationHooks.replyPolicies.install { true },
        )
        try {
            assertTrue(NotificationHooks.sendsPlainAnswers())
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }
}
