package com.eltavine.sereingram.features.bookmarks

import com.eltavine.sereingram.ports.Bookmark
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookmarkIndexTest {
    private fun bookmark(dialogId: Long, messageId: Int) = Bookmark(dialogId, messageId, 0, 0, "", 0)

    @Test
    fun loadedBookmarksAreFoundPerAccountAndChat() {
        val index = BookmarkIndex()
        assertFalse(index.isLoaded(0))
        index.load(0, listOf(bookmark(-100, 5), bookmark(42, 3)))
        assertTrue(index.isLoaded(0))
        assertTrue(index.contains(0, -100, 5))
        assertFalse(index.contains(0, -100, 3))
        assertFalse(index.contains(1, -100, 5))
        assertTrue(index.hasAny(0, 42))
        assertFalse(index.hasAny(0, 7))
    }

    @Test
    fun bookmarksCanBeAddedAndRemoved() {
        val index = BookmarkIndex()
        index.set(0, -100, 5, bookmarked = true)
        assertTrue(index.contains(0, -100, 5))
        index.set(0, -100, 5, bookmarked = false)
        assertFalse(index.contains(0, -100, 5))
        assertFalse(index.hasAny(0, -100))
    }

    @Test
    fun snippetsAreOneLineAndCutWithAnEllipsis() {
        assertEquals("two lines", snippet("  two\n   lines "))
        assertEquals("abcd…", snippet("abcdefgh", limit = 5))
        assertEquals("abcde", snippet("abcde", limit = 5))
    }
}
