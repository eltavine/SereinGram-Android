package com.eltavine.sereingram.hooks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatMenuHooksTest {
    private class Entry(
        override val id: Int,
        private val shownIn: Long,
        private val title: () -> CharSequence = { "Ghost mode" },
    ) : ChatMenuHooks.Entry {
        val selected = mutableListOf<Long>()
        override val icon: Int = 7

        override fun title(account: Int, dialogId: Long): CharSequence = title.invoke()

        override fun isShown(account: Int, dialogId: Long): Boolean = dialogId == shownIn

        override fun onSelected(account: Int, dialogId: Long, chat: Any) {
            selected += dialogId
        }
    }

    @Test
    fun shownEntriesAreAddedAndSelectedById() {
        val ghost = Entry(ChatMenuHooks.FIRST_ID, shownIn = 5)
        val installs = listOf(
            ChatMenuHooks.entries.install(ghost),
            ChatMenuHooks.entries.install(Entry(id = 12, shownIn = 5)),
            ChatMenuHooks.entries.install(Entry(ChatMenuHooks.FIRST_ID + 1, shownIn = 5) { throw IllegalStateException() }),
        )
        try {
            val added = mutableListOf<String>()
            ChatMenuHooks.fill(0, 5) { id, icon, title -> added += "$id/$icon/$title" }
            ChatMenuHooks.fill(0, 6) { id, icon, title -> added += "$id/$icon/$title" }
            assertEquals(listOf("${ChatMenuHooks.FIRST_ID}/7/Ghost mode"), added)
            assertTrue(ChatMenuHooks.select(ChatMenuHooks.FIRST_ID, 0, 5, "chat"))
            assertFalse(ChatMenuHooks.select(12, 0, 5, "chat"))
            assertFalse(ChatMenuHooks.select(ChatMenuHooks.FIRST_ID + 9, 0, 5, "chat"))
            assertEquals(listOf(5L), ghost.selected)
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }
}
