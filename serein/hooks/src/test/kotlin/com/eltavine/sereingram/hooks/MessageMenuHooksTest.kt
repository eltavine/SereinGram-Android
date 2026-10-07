package com.eltavine.sereingram.hooks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessageMenuHooksTest {
    private class Entry(
        override val option: Int,
        private val shownFor: Int,
        private val title: () -> CharSequence = { "Edit history" },
    ) : MessageMenuHooks.Entry {
        val selected = mutableListOf<Int>()
        override val icon: Int = 42

        override fun title(): CharSequence = title.invoke()

        override fun isShown(account: Int, dialogId: Long, messageId: Int): Boolean = messageId == shownFor

        override fun onSelected(account: Int, dialogId: Long, messageId: Int, host: Any) {
            selected += messageId
        }
    }

    @Test
    fun shownEntriesAreAppendedAndSelectedByOption() {
        val history = Entry(MessageMenuHooks.FIRST_OPTION + 1, shownFor = 7)
        val install = MessageMenuHooks.entries.install(history)
        try {
            val items = mutableListOf<CharSequence>("Reply")
            val options = mutableListOf(1)
            val icons = mutableListOf(10)
            MessageMenuHooks.fill(0, 1, 7, items, options, icons)
            assertEquals(listOf<CharSequence>("Reply", "Edit history"), items)
            assertEquals(listOf(1, MessageMenuHooks.FIRST_OPTION + 1), options)
            assertEquals(listOf(10, 42), icons)
            MessageMenuHooks.fill(0, 1, 8, items, options, icons)
            assertEquals(2, items.size)
            assertTrue(MessageMenuHooks.select(MessageMenuHooks.FIRST_OPTION + 1, 0, 1, 7, "chat"))
            assertFalse(MessageMenuHooks.select(1, 0, 1, 7, "chat"))
            assertEquals(listOf(7), history.selected)
        } finally {
            install.close()
        }
    }

    @Test
    fun entriesThatClashOrThrowAreLeftOutWithoutBreakingTheLists() {
        val installs = listOf(
            MessageMenuHooks.entries.install(Entry(option = 5, shownFor = 7)),
            MessageMenuHooks.entries.install(Entry(MessageMenuHooks.FIRST_OPTION + 2, shownFor = 7) { throw IllegalStateException() }),
        )
        try {
            val items = mutableListOf<CharSequence>()
            val options = mutableListOf<Int>()
            val icons = mutableListOf<Int>()
            MessageMenuHooks.fill(0, 1, 7, items, options, icons)
            assertEquals(0, items.size)
            assertEquals(0, options.size)
            assertEquals(0, icons.size)
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }
}
