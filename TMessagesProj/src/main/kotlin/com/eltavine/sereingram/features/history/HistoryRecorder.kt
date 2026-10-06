package com.eltavine.sereingram.features.history

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.ports.HistoryRecord
import com.eltavine.sereingram.ports.HistoryStore
import com.eltavine.sereingram.ports.RecordKind
import org.telegram.messenger.MessageObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.SerializedData
import org.telegram.tgnet.TLRPC
import java.util.concurrent.Executor

/**
 * Keeps the versions of messages Telegram is about to drop. It reads them on
 * Telegram's storage queue, where the hooks run, and writes on [writer].
 */
internal class HistoryRecorder(
    private val options: Options,
    private val stores: (account: Int) -> HistoryStore,
    private val writer: Executor,
) {
    fun beforeDeleted(account: Int, dialogId: Long, messageIds: List<Int>) {
        if (messageIds.isEmpty() || !options.get(HistoryOptions.saveDeleted, account)) {
            return
        }
        val saveInBots = options.get(HistoryOptions.saveInBotChats, account)
        val records = storedMessages(account, dialogId, messageIds)
            .filter { (uid, _) -> recordsDeletion(saveDeleted = true, saveInBots, Change(botChat = isBotChat(account, uid))) }
            .map { (uid, message) -> record(RecordKind.DELETED, uid, message, revision = 0) }
        write(account, records)
    }

    fun beforeEdited(account: Int, dialogId: Long, previous: Any, next: Any, sameMedia: Boolean) {
        if (!options.get(HistoryOptions.saveEdits, account)) {
            return
        }
        val old = previous as TLRPC.Message
        val new = next as TLRPC.Message
        val change = Change(
            botChat = isBotChat(account, dialogId),
            textChanged = old.message.orEmpty() != new.message.orEmpty(),
            mediaChanged = !sameMedia,
        )
        if (recordsEdit(saveEdits = true, options.get(HistoryOptions.saveInBotChats, account), change)) {
            write(account, listOf(record(RecordKind.EDITED, dialogId, old, revision = old.edit_date)))
        }
    }

    // The same rows MessagesStorage.markMessagesAsDeletedInternal is about to delete.
    private fun storedMessages(account: Int, dialogId: Long, messageIds: List<Int>): List<Pair<Long, TLRPC.Message>> {
        val ids = messageIds.joinToString(",")
        val where = if (dialogId != 0L) "uid = $dialogId" else "is_channel = 0"
        val selfId = UserConfig.getInstance(account).clientUserId
        val cursor = MessagesStorage.getInstance(account).database
            .queryFinalized("SELECT uid, data FROM messages_v2 WHERE mid IN($ids) AND $where")
        val messages = ArrayList<Pair<Long, TLRPC.Message>>()
        try {
            while (cursor.next()) {
                val data = cursor.byteBufferValue(1) ?: continue
                val message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false)
                message?.readAttachPath(data, selfId)
                data.reuse()
                if (message != null) {
                    messages += cursor.longValue(0) to message
                }
            }
        } finally {
            cursor.dispose()
        }
        return messages
    }

    private fun isBotChat(account: Int, dialogId: Long): Boolean =
        dialogId > 0 && MessagesController.getInstance(account).getUser(dialogId)?.bot == true

    private fun record(kind: RecordKind, dialogId: Long, message: TLRPC.Message, revision: Int): HistoryRecord {
        val data = SerializedData(message.objectSize)
        message.serializeToStream(data)
        val bytes = data.toByteArray()
        data.cleanup()
        return HistoryRecord(
            kind = kind,
            dialogId = dialogId,
            messageId = message.id,
            revision = revision,
            topicId = message.reply_to?.reply_to_top_id?.toLong() ?: 0L,
            date = message.date,
            recordedAt = System.currentTimeMillis(),
            fromId = MessageObject.getFromChatId(message),
            text = message.message.orEmpty(),
            tlMessage = bytes,
            apiLayer = TLRPC.LAYER,
        )
    }

    private fun write(account: Int, records: List<HistoryRecord>) {
        if (records.isNotEmpty()) {
            writer.execute { Faults.guard("history write", fallback = Unit) { stores(account).add(records) } }
        }
    }
}
