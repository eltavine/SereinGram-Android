package com.eltavine.sereingram.adapters.room

import android.content.Context
import androidx.room.Room
import com.eltavine.sereingram.ports.HistoryRecord
import com.eltavine.sereingram.ports.HistoryStore
import com.eltavine.sereingram.ports.KeptChat
import com.eltavine.sereingram.ports.RecordKind

/**
 * Message history in a Room database of its own, `serein_history_<account>.db`,
 * apart from Telegram's cache so that clearing the cache keeps it.
 */
public class RoomHistoryStore internal constructor(private val database: HistoryDatabase) : HistoryStore {
    public constructor(context: Context, account: Int) : this(
        Room.databaseBuilder(context.applicationContext, HistoryDatabase::class.java, "serein_history_$account.db").build(),
    )

    private val dao = database.records()

    override fun add(records: List<HistoryRecord>) {
        if (records.isNotEmpty()) {
            dao.insert(records.map(::entity))
        }
    }

    override fun deleted(dialogId: Long, limit: Int, beforeMessageId: Int): List<HistoryRecord> =
        dao.page(RecordKind.DELETED.code, dialogId, beforeMessageId, limit).mapNotNull(::record)

    override fun revisions(dialogId: Long, messageId: Int): List<HistoryRecord> =
        dao.versions(RecordKind.EDITED.code, dialogId, messageId).mapNotNull(::record)

    override fun withRevisions(dialogId: Long, messageIds: Collection<Int>): Set<Int> =
        messageIds.distinct().chunked(MAX_IDS).flatMapTo(HashSet()) { dao.present(RecordKind.EDITED.code, dialogId, it) }

    override fun forget(dialogId: Long, messageIds: Collection<Int>) {
        messageIds.distinct().chunked(MAX_IDS).forEach { dao.forget(dialogId, it) }
    }

    override fun count(dialogId: Long, kind: RecordKind): Int = dao.count(kind.code, dialogId)

    override fun chats(kind: RecordKind): List<KeptChat> =
        dao.chats(kind.code).map { KeptChat(it.dialogId, it.count, it.lastRecordedAt) }

    override fun clear(dialogId: Long) {
        dao.clear(dialogId)
    }

    internal fun close() {
        database.close()
    }

    private fun entity(record: HistoryRecord) = RecordEntity(
        id = 0,
        kind = record.kind.code,
        dialogId = record.dialogId,
        messageId = record.messageId,
        revision = record.revision,
        topicId = record.topicId,
        date = record.date,
        recordedAt = record.recordedAt,
        fromId = record.fromId,
        text = record.text,
        tlMessage = record.tlMessage,
        apiLayer = record.apiLayer,
    )

    // A row written by a newer release with a kind this one does not know is skipped.
    private fun record(entity: RecordEntity): HistoryRecord? = RecordKind.of(entity.kind)?.let { kind ->
        HistoryRecord(
            kind = kind,
            dialogId = entity.dialogId,
            messageId = entity.messageId,
            revision = entity.revision,
            topicId = entity.topicId,
            date = entity.date,
            recordedAt = entity.recordedAt,
            fromId = entity.fromId,
            text = entity.text,
            tlMessage = entity.tlMessage,
            apiLayer = entity.apiLayer,
        )
    }

    private companion object {
        // Older SQLite builds allow at most 999 bound variables per query.
        const val MAX_IDS = 900
    }
}
