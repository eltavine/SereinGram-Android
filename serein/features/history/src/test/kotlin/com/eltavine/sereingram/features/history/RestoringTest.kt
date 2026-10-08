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
    fun aBatchStandsForTheGapUpToTheNextCachedMessage() {
        val coverage = Coverage.of(listOf(-3, 10, 20, 15), olderCached = true, nextNewerCached = 30)!!
        assertEquals(10, coverage.after)
        assertEquals(30, coverage.before)
        assertTrue(25 in coverage, "a deletion between this batch and the newer one is this batch's")
        assertFalse(30 in coverage)
        assertFalse(10 in coverage)
        assertFalse(5 in coverage, "an older deletion is the older batch's")
    }

    @Test
    fun theNewestAndTheOldestBatchesReachTheEndsOfTheChat() {
        val newest = Coverage.of(listOf(10, 20), olderCached = true, nextNewerCached = null)!!
        assertTrue(21 in newest, "a message deleted after the last one still shows")
        val oldest = Coverage.of(listOf(10, 20), olderCached = false, nextNewerCached = 30)!!
        assertTrue(1 in oldest, "a message deleted before the first one still shows")
        val only = Coverage.of(listOf(10), olderCached = false, nextNewerCached = null)!!
        assertTrue(5 in only && 15 in only, "a chat with one message left gets the rest back")
    }

    @Test
    fun anEmptyBatchStandsForTheWholeChatOnlyWhenNothingIsCached() {
        assertNull(Coverage.of(emptyList(), olderCached = true, nextNewerCached = null))
        assertNull(Coverage.of(listOf(-4), olderCached = false, nextNewerCached = 12))
        val everything = Coverage.of(emptyList(), olderCached = false, nextNewerCached = null)!!
        assertTrue(1 in everything && Int.MAX_VALUE - 1 in everything)
    }

    @Test
    fun deletionsTheBatchStandsForAndDoesNotHoldAreRestored() {
        val coverage = Coverage.of(listOf(10, 15, 20), olderCached = true, nextNewerCached = 30)!!
        val candidates = listOf(25, 20, 18, 15, 12, 10, 4).map(::deleted)
        assertEquals(listOf(25, 18, 12), restorable(coverage, setOf(10, 15, 20), candidates).map { it.messageId })
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
    fun messageSetsAreSeparatedByAccountAndChatAndBounded() {
        val kept = MessageSet(capacity = 2)
        kept.add(account = 0, dialogId = 1, messageIds = listOf(5))
        assertTrue(kept.contains(0, 1, 5))
        assertFalse(kept.contains(1, 1, 5))
        assertFalse(kept.contains(0, 2, 5))
        kept.add(account = 0, dialogId = 1, messageIds = listOf(6, 7))
        assertFalse(kept.contains(0, 1, 5))
        assertTrue(kept.contains(0, 1, 7))
    }

    @Test
    fun messageMapsKeepTheLatestValueAndForgetTheLeastRecentlyUsed() {
        val deletedAt = MessageMap<Long>(capacity = 2)
        deletedAt.put(0, 1, 5, 100L)
        deletedAt.put(0, 1, 5, 200L)
        deletedAt.put(0, 1, 6, 300L)
        assertEquals(200L, deletedAt[0, 1, 5])
        deletedAt.put(0, 1, 7, 400L)
        assertNull(deletedAt[0, 1, 6])
        assertEquals(200L, deletedAt[0, 1, 5])
        assertNull(deletedAt[1, 1, 5])
    }

    @Test
    fun aForgottenAccountLeavesNothingForTheNextOneInItsPlace() {
        val deletedAt = MessageMap<Long>()
        deletedAt.put(0, -100, 5, 100L)
        deletedAt.put(1, -100, 5, 200L)
        deletedAt.forget(0)
        assertNull(deletedAt[0, -100, 5])
        assertEquals(200L, deletedAt[1, -100, 5])
        val revised = MessageSet()
        revised.add(0, -100, listOf(5))
        revised.forget(0)
        assertFalse(revised.contains(0, -100, 5))
    }
}
