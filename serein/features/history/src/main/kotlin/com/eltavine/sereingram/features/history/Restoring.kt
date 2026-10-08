package com.eltavine.sereingram.features.history

import com.eltavine.sereingram.ports.HistoryRecord

/**
 * The span of a loaded batch of history, by the ids of messages the server
 * knows; local and ephemeral messages have ids of their own and do not count.
 */
public class BatchSpan(public val oldest: Int, public val newest: Int) {
    public companion object {
        /** Null when the batch spans no gap a deleted message could sit in. */
        public fun of(messageIds: Collection<Int>): BatchSpan? {
            val ids = messageIds.filter { it > 0 }
            if (ids.size < 2) {
                return null
            }
            val oldest = ids.min()
            val newest = ids.max()
            return if (newest - oldest > 1) BatchSpan(oldest, newest) else null
        }
    }
}

/**
 * Saved deletions that belong inside [span] and are not in the batch already.
 * Only the inside counts: what lies beyond a batch's edges is the neighbouring
 * batch's to restore, so paging through history stays as Telegram computes it.
 */
public fun restorable(span: BatchSpan, loadedIds: Set<Int>, candidates: List<HistoryRecord>): List<HistoryRecord> =
    candidates.filter { it.messageId in (span.oldest + 1) until span.newest && it.messageId !in loadedIds }

/**
 * Where a message with [id] goes in a batch Telegram sorts newest first:
 * before the first server message with a smaller id. Null and non-positive
 * entries are local messages, which keep their places.
 */
public fun insertionIndex(ids: List<Int?>, id: Int): Int =
    ids.indexOfFirst { it != null && it in 1 until id }.let { if (it < 0) ids.size else it }

/** A set of messages by account and chat, bounded so that it cannot grow without end. */
public class MessageSet(capacity: Int = 10_000) {
    private val messages = MessageMap<Unit>(capacity)

    public fun add(account: Int, dialogId: Long, messageIds: Collection<Int>) {
        messageIds.forEach { messages.put(account, dialogId, it, Unit) }
    }

    public fun contains(account: Int, dialogId: Long, messageId: Int): Boolean = messages[account, dialogId, messageId] != null

    public fun forget(account: Int) {
        messages.forget(account)
    }
}

/** A value per message by account and chat, bounded so that it cannot grow without end. */
public class MessageMap<V : Any>(private val capacity: Int = 10_000) {
    private data class Key(val account: Int, val dialogId: Long, val messageId: Int)

    private val values = object : LinkedHashMap<Key, V>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, V>?): Boolean = size > capacity
    }

    @Synchronized
    public fun put(account: Int, dialogId: Long, messageId: Int, value: V) {
        values[Key(account, dialogId, messageId)] = value
    }

    @Synchronized
    public operator fun get(account: Int, dialogId: Long, messageId: Int): V? = values[Key(account, dialogId, messageId)]

    @Synchronized
    public fun forget(account: Int) {
        values.keys.removeIf { it.account == account }
    }
}
