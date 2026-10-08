package com.eltavine.sereingram.features.history

import com.eltavine.sereingram.ports.HistoryRecord

/**
 * The message ids a batch of history loaded from Telegram's database stands
 * for, by the ids of messages the server knows; local and ephemeral messages
 * have ids of their own and do not count. A batch stands for the ids from its
 * oldest message, or from the chat's start when nothing older is cached, up to
 * the next cached message above it, or without end when it holds the newest.
 * Every gap between cached messages thus belongs to exactly one batch.
 */
public class Coverage(public val after: Int, public val before: Int) {
    public operator fun contains(id: Int): Boolean = id > after && id < before

    public companion object {
        public const val WITHOUT_END: Int = Int.MAX_VALUE

        /**
         * What a batch of [loadedIds] stands for, given whether the chat has cached
         * messages older than the batch and the next cached message newer than it.
         * Null for an empty batch of a chat that has cached messages elsewhere.
         */
        public fun of(loadedIds: Collection<Int>, olderCached: Boolean, nextNewerCached: Int?): Coverage? {
            val ids = loadedIds.filter { it > 0 }
            if (ids.isEmpty() && (olderCached || nextNewerCached != null)) {
                return null
            }
            val after = if (olderCached) ids.min() else 0
            return Coverage(after, nextNewerCached ?: WITHOUT_END)
        }
    }
}

/** Saved deletions that a batch stands for and that it does not hold already. */
public fun restorable(coverage: Coverage, loadedIds: Set<Int>, candidates: List<HistoryRecord>): List<HistoryRecord> =
    candidates.filter { it.messageId in coverage && it.messageId !in loadedIds }

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
