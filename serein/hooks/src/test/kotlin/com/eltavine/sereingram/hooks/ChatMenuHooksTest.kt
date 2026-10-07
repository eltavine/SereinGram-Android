package com.eltavine.sereingram.hooks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatMenuHooksTest {
    private class Entry(
        override val id: Int,
        var shownIn: Long,
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
    fun entriesAreAddedOnceAndShownWhereTheyApplyWheneverTheMenuOpens() {
        val ghost = Entry(ChatMenuHooks.FIRST_ID, shownIn = 6)
        var title = "Lock chat"
        val installs = listOf(
            ChatMenuHooks.entries.install(ghost),
            ChatMenuHooks.entries.install(Entry(id = 12, shownIn = 5)),
            ChatMenuHooks.entries.install(Entry(ChatMenuHooks.FIRST_ID + 1, shownIn = 5) { title }),
        )
        try {
            val added = mutableListOf<String>()
            ChatMenuHooks.fill(0, 5) { id, icon, label -> added += "$id/$icon/$label" }
            assertEquals(listOf("${ChatMenuHooks.FIRST_ID}/7/Ghost mode", "${ChatMenuHooks.FIRST_ID + 1}/7/Lock chat"), added)

            val opened = mutableListOf<String>()
            ChatMenuHooks.refresh(0, 5) { id, shown, label -> opened += "$id/$shown/$label" }
            ghost.shownIn = 5
            title = "Unlock chat"
            ChatMenuHooks.refresh(0, 5) { id, shown, label -> opened += "$id/$shown/$label" }
            assertEquals(
                listOf(
                    "${ChatMenuHooks.FIRST_ID}/false/",
                    "${ChatMenuHooks.FIRST_ID + 1}/true/Lock chat",
                    "${ChatMenuHooks.FIRST_ID}/true/Ghost mode",
                    "${ChatMenuHooks.FIRST_ID + 1}/true/Unlock chat",
                ),
                opened,
            )
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }

    @Test
    fun anEntryThatFailsIsLeftOutAndHidden() {
        val installs = listOf(
            ChatMenuHooks.entries.install(Entry(ChatMenuHooks.FIRST_ID, shownIn = 5) { throw IllegalStateException() }),
            ChatMenuHooks.entries.install(Entry(ChatMenuHooks.FIRST_ID + 1, shownIn = 5)),
        )
        try {
            val added = mutableListOf<Int>()
            ChatMenuHooks.fill(0, 5) { id, _, _ -> added += id }
            assertEquals(listOf(ChatMenuHooks.FIRST_ID + 1), added)
            val shown = mutableMapOf<Int, Boolean>()
            ChatMenuHooks.refresh(0, 5) { id, isShown, _ -> shown[id] = isShown }
            assertEquals(mapOf(ChatMenuHooks.FIRST_ID to false, ChatMenuHooks.FIRST_ID + 1 to true), shown)
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }

    @Test
    fun selectedItemsReachTheEntryThatShowedThem() {
        val ghost = Entry(ChatMenuHooks.FIRST_ID, shownIn = 5)
        val installs = listOf(
            ChatMenuHooks.entries.install(ghost),
            ChatMenuHooks.entries.install(Entry(id = 12, shownIn = 5)),
        )
        try {
            assertTrue(ChatMenuHooks.select(ChatMenuHooks.FIRST_ID, 0, 5, "chat"))
            assertFalse(ChatMenuHooks.select(12, 0, 5, "chat"))
            assertFalse(ChatMenuHooks.select(ChatMenuHooks.FIRST_ID + 9, 0, 5, "chat"))
            assertEquals(listOf(5L), ghost.selected)
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }
}
