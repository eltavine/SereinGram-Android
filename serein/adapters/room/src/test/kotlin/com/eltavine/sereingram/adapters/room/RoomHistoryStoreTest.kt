package com.eltavine.sereingram.adapters.room

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.eltavine.sereingram.ports.HistoryRecord
import com.eltavine.sereingram.ports.RecordKind
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomHistoryStoreTest {
    private val store = RoomHistoryStore(
        Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), HistoryDatabase::class.java)
            .allowMainThreadQueries()
            .build(),
    )

    @After
    fun close() = store.close()

    private fun record(
        kind: RecordKind,
        messageId: Int,
        revision: Int = 0,
        dialogId: Long = -100L,
        text: String = "text $messageId",
        recordedAt: Long = 1_000L + revision,
    ) = HistoryRecord(kind, dialogId, messageId, revision, topicId = 0, date = messageId, recordedAt = recordedAt, fromId = 7, text = text, tlMessage = byteArrayOf(1, 2, messageId.toByte()), apiLayer = 214)

    @Test
    fun deletedMessagesPageNewestFirst() {
        store.add((1..5).map { record(RecordKind.DELETED, it) })
        assertEquals(listOf(5, 4), store.deleted(-100L, limit = 2).map { it.messageId })
        assertEquals(listOf(3, 2, 1), store.deleted(-100L, limit = 10, beforeMessageId = 4).map { it.messageId })
    }

    @Test
    fun aDeletionKeptTwiceIsStoredOnce() {
        store.add(listOf(record(RecordKind.DELETED, 9, text = "first")))
        store.add(listOf(record(RecordKind.DELETED, 9, text = "second")))
        assertEquals(listOf("first"), store.deleted(-100L, limit = 10).map { it.text })
        assertEquals(1, store.count(-100L, RecordKind.DELETED))
    }

    @Test
    fun revisionsComeOldestFirstAndKeepTheirBytes() {
        store.add(listOf(record(RecordKind.EDITED, 3, revision = 20), record(RecordKind.EDITED, 3, revision = 0)))
        val revisions = store.revisions(-100L, 3)
        assertEquals(listOf(0, 20), revisions.map { it.revision })
        assertContentEquals(byteArrayOf(1, 2, 3), revisions.first().tlMessage)
        assertEquals(214, revisions.first().apiLayer)
    }

    @Test
    fun findsWhichMessagesHaveEarlierVersions() {
        store.add(listOf(record(RecordKind.EDITED, 3), record(RecordKind.EDITED, 3, revision = 5), record(RecordKind.DELETED, 4)))
        assertEquals(setOf(3), store.withRevisions(-100L, listOf(2, 3, 4)))
        assertEquals(setOf(3), store.withRevisions(-100L, (1..2000).toList()))
        assertEquals(emptySet(), store.withRevisions(-200L, listOf(3)))
    }

    @Test
    fun forgettingMessagesDropsAllTheirRecordsInThatChatOnly() {
        store.add(listOf(record(RecordKind.EDITED, 3), record(RecordKind.DELETED, 3), record(RecordKind.DELETED, 4)))
        store.add(listOf(record(RecordKind.DELETED, 3, dialogId = -200L)))
        store.forget(-100L, listOf(3))
        assertEquals(listOf(4), store.deleted(-100L, limit = 10).map { it.messageId })
        assertEquals(emptyList(), store.revisions(-100L, 3))
        assertEquals(1, store.count(-200L, RecordKind.DELETED))
    }

    @Test
    fun chatsWithDeletedMessagesComeMostRecentFirstWithTheirCounts() {
        store.add(
            listOf(
                record(RecordKind.DELETED, 1, dialogId = 1L, recordedAt = 5_000),
                record(RecordKind.DELETED, 2, dialogId = 1L, recordedAt = 6_000),
                record(RecordKind.DELETED, 1, dialogId = 2L, recordedAt = 9_000),
                record(RecordKind.EDITED, 1, dialogId = 3L, recordedAt = 10_000),
                record(RecordKind.EDITED, 1, revision = 4, dialogId = 1L, recordedAt = 20_000),
            ),
        )
        val chats = store.chats(RecordKind.DELETED)
        assertEquals(listOf(2L, 1L), chats.map { it.dialogId })
        assertEquals(listOf(1, 2), chats.map { it.count })
        assertEquals(listOf(9_000L, 6_000L), chats.map { it.lastRecordedAt })
        assertEquals(listOf(1L, 3L), store.chats(RecordKind.EDITED).map { it.dialogId })
        assertEquals(listOf(1, 1), store.chats(RecordKind.EDITED).map { it.count })
    }

    @Test
    fun clearingAChatDropsOneKindOfItsRecordsOnly() {
        store.add(
            listOf(
                record(RecordKind.DELETED, 1, dialogId = 1L),
                record(RecordKind.EDITED, 2, dialogId = 1L),
                record(RecordKind.DELETED, 1, dialogId = 2L),
            ),
        )
        store.clear(1L, RecordKind.DELETED)
        assertEquals(0, store.count(1L, RecordKind.DELETED))
        assertEquals(1, store.count(1L, RecordKind.EDITED), "clearing deleted messages keeps edit histories")
        assertEquals(1, store.count(2L, RecordKind.DELETED))
    }

    @Test
    fun clearingErasesEveryChatAndKind() {
        store.add(listOf(record(RecordKind.DELETED, 1, dialogId = 1L), record(RecordKind.EDITED, 2, dialogId = 2L)))
        store.clearAll()
        assertEquals(emptyList(), store.chats(RecordKind.DELETED) + store.chats(RecordKind.EDITED))
    }
}
