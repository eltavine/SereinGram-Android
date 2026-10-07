package com.eltavine.sereingram.hooks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HistoryHooksTest {
    @Test
    fun chatsRemoveTheSameListWhenNothingIsKept() {
        val deleted = arrayListOf(3, 4, 5)
        val install = HistoryHooks.chatKeepers.install { _, _, _, _ -> emptyList() }
        try {
            assertSame(deleted, HistoryHooks.removedFromChat(0, 0, deleted, "chat"))
        } finally {
            install.close()
        }
    }

    @Test
    fun keptMessagesStayWithoutChangingTelegramsList() {
        val deleted = arrayListOf(3, 4, 5)
        val installs = listOf(
            HistoryHooks.chatKeepers.install { _, _, ids, _ -> ids.filter { it == 4 } + 99 },
            HistoryHooks.chatKeepers.install { _, _, _, _ -> throw IllegalStateException() },
        )
        try {
            assertEquals(listOf(3, 5), HistoryHooks.removedFromChat(0, 0, deleted, "chat"))
            assertEquals(listOf(3, 4, 5), deleted)
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }

    @Test
    fun filesAreDeletedUnlessAKeeperKeepsThem() {
        assertTrue(HistoryHooks.deletesFilesOf(0, -100, listOf(5)))
        val installs = listOf(
            HistoryHooks.fileKeepers.install { _, _, _ -> throw IllegalStateException() },
            HistoryHooks.fileKeepers.install { account, _, _ -> account == 1 },
        )
        try {
            assertTrue(HistoryHooks.deletesFilesOf(0, -100, listOf(5)))
            assertFalse(HistoryHooks.deletesFilesOf(1, -100, listOf(5)))
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }

    @Test
    fun userDeletionsReachEveryListener() {
        val seen = mutableListOf<String>()
        val installs = listOf(
            HistoryHooks.userDeletionListeners.install { _, _, _ -> throw IllegalStateException() },
            HistoryHooks.userDeletionListeners.install { account, dialogId, ids -> seen += "$account/$dialogId/$ids" },
        )
        try {
            HistoryHooks.beforeUserDeletes(1, -100, listOf(7))
            assertEquals(listOf("1/-100/[7]"), seen)
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }
}
