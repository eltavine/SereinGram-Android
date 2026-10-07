package com.eltavine.sereingram.features.history

import com.eltavine.sereingram.ports.HistoryRecord
import com.eltavine.sereingram.ports.RecordKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RestoringTest {
    private fun deleted(id: Int) = HistoryRecord(
        RecordKind.DELETED, dialogId = 1, messageId = id, revision = 0, topicId = 0, date = id,
        recordedAt = 0, fromId = 2, text = "", tlMessage = byteArrayOf(), apiLayer = 0,
    )

    @Test
    fun aBatchNeedsRoomBetweenTwoServerMessages() {
        assertNull(BatchSpan.of(listOf(10)))
        assertNull(BatchSpan.of(listOf(10, 11)))
        assertNull(BatchSpan.of(listOf(-5, 10)))
        val span = BatchSpan.of(listOf(-3, 10, 20, 15))!!
        assertEquals(10, span.oldest)
        assertEquals(20, span.newest)
    }

    @Test
    fun onlyDeletionsInsideTheBatchAndNotLoadedAreRestored() {
        val span = BatchSpan.of(listOf(10, 15, 20))!!
        val candidates = listOf(25, 20, 18, 15, 12, 10, 4).map(::deleted)
        assertEquals(listOf(18, 12), restorable(span, setOf(10, 15, 20), candidates).map { it.messageId })
    }

    @Test
    fun restoredMessagesGoWhereTheirIdBelongsNewestFirst() {
        assertEquals(1, insertionIndex(listOf(20, 15, 10), 18))
        assertEquals(2, insertionIndex(listOf(20, 15, 10), 12))
        assertEquals(2, insertionIndex(listOf(-2, 20, 10), 15))
        assertEquals(2, insertionIndex(listOf(20, null, 10), 15))
        assertEquals(3, insertionIndex(listOf(20, 15, 10), 5))
    }

    @Test
    fun keptMessagesAreSeparatedByAccountAndChatAndBounded() {
        val kept = KeptMessages(capacity = 2)
        kept.add(account = 0, dialogId = 1, messageIds = listOf(5))
        assertTrue(kept.contains(0, 1, 5))
        assertFalse(kept.contains(1, 1, 5))
        assertFalse(kept.contains(0, 2, 5))
        kept.add(account = 0, dialogId = 1, messageIds = listOf(6, 7))
        assertFalse(kept.contains(0, 1, 5))
        assertTrue(kept.contains(0, 1, 7))
    }
}
