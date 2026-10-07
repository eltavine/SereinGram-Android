package com.eltavine.sereingram.adapters.room

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.eltavine.sereingram.ports.Bookmark
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomBookmarkStoreTest {
    private val store = RoomBookmarkStore(
        Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), BookmarkDatabase::class.java)
            .allowMainThreadQueries()
            .build(),
    )

    @After
    fun close() = store.close()

    private fun bookmark(dialogId: Long, messageId: Int, createdAt: Long, text: String = "message $messageId") =
        Bookmark(dialogId, messageId, messageDate = messageId * 10, senderId = 7, text = text, createdAt = createdAt)

    @Test
    fun bookmarksListNewestFirstAndPerChatByMessage() {
        store.add(bookmark(-100, 5, createdAt = 2))
        store.add(bookmark(-100, 9, createdAt = 1))
        store.add(bookmark(42, 3, createdAt = 3))
        assertEquals(listOf(3, 5, 9), store.all().map { it.messageId })
        assertEquals(listOf(9, 5), store.inChat(-100).map { it.messageId })
        assertEquals("message 5", store.inChat(-100).last().text)
    }

    @Test
    fun aMessageIsBookmarkedOnceAndCanBeRemoved() {
        store.add(bookmark(-100, 5, createdAt = 1, text = "first"))
        store.add(bookmark(-100, 5, createdAt = 2, text = "second"))
        assertEquals(listOf("first"), store.inChat(-100).map { it.text })
        store.remove(-100, 5)
        assertEquals(emptyList(), store.all())
    }
}
