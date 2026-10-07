package com.eltavine.sereingram.ports

/** Why a version of a message was kept. The codes are stored and never reused. */
public enum class RecordKind(public val code: Int) {
    DELETED(1),
    EDITED(2),
    ;

    public companion object {
        public fun of(code: Int): RecordKind? = entries.firstOrNull { it.code == code }
    }
}

/**
 * One saved version of a message. [tlMessage] is the message serialized by
 * Telegram at [apiLayer]; [text] is kept apart so the record stays readable
 * when a later layer can no longer parse it.
 */
public class HistoryRecord(
    public val kind: RecordKind,
    public val dialogId: Long,
    public val messageId: Int,
    /** For [RecordKind.EDITED], the edit date of the replaced version, 0 for the original. */
    public val revision: Int,
    public val topicId: Long,
    public val date: Int,
    public val recordedAt: Long,
    public val fromId: Long,
    public val text: String,
    public val tlMessage: ByteArray,
    public val apiLayer: Int,
)

/** The saved history of one account. Calls block; callers keep them off the main thread. */
public interface HistoryStore {
    /** Adds [records]; a record that is already kept is left as it is. */
    public fun add(records: List<HistoryRecord>)

    /** Deleted messages of [dialogId] with ids below [beforeMessageId], newest first. */
    public fun deleted(dialogId: Long, limit: Int, beforeMessageId: Int = Int.MAX_VALUE): List<HistoryRecord>

    /** Earlier versions of a message, oldest first. */
    public fun revisions(dialogId: Long, messageId: Int): List<HistoryRecord>

    /** Which of [messageIds] have earlier versions kept. */
    public fun withRevisions(dialogId: Long, messageIds: Collection<Int>): Set<Int>

    public fun count(dialogId: Long, kind: RecordKind): Int

    public fun clear(dialogId: Long)
}
