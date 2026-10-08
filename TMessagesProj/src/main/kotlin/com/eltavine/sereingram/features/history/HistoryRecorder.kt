package com.eltavine.sereingram.features.history

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.ports.HistoryRecord
import com.eltavine.sereingram.ports.HistoryStore
import com.eltavine.sereingram.ports.RecordKind
import org.telegram.messenger.MessageObject
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.TLRPC
import java.util.concurrent.Executor

/**
 * Keeps the versions of messages Telegram is about to drop. It reads them on
 * Telegram's storage queue, where the hooks run, and writes on [writer].
 * What the user deletes is not kept, and anything kept of it is dropped.
 */
internal class HistoryRecorder(
    private val options: Options,
    private val stores: (account: Int) -> HistoryStore,
    private val writer: Executor,
    private val revised: MessageSet,
    private val deletedByUser: MessageSet,
    private val backups: MediaBackups,
) {
    fun beforeUserDeletes(account: Int, dialogId: Long, messageIds: List<Int>) {
        val ids = messageIds.filter { it > 0 }
        if (ids.isEmpty()) {
            return
        }
        deletedByUser.add(account, dialogId, ids)
        if (options.get(HistoryOptions.saveDeleted, account) || options.get(HistoryOptions.saveEdits, account)) {
            writer.execute {
                Faults.guard("history forget", fallback = Unit) {
                    stores(account).forget(dialogId, ids)
                    backups.forget(account, dialogId, ids)
                }
            }
        }
    }

    fun beforeDeleted(account: Int, dialogId: Long, messageIds: List<Int>) {
        if (messageIds.isEmpty() || !options.get(HistoryOptions.saveDeleted, account)) {
            return
        }
        val saveInBots = options.get(HistoryOptions.saveInBotChats, account)
        val kept = storedMessages(account, dialogId, messageIds)
            .filterNot { (uid, message) -> deletedByUser.contains(account, uid, message.id) }
            .filter { (uid, message) ->
                recordsDeletion(saveDeleted = true, saveInBots, Change(botChat = isBotChat(account, uid), selfDestructing = isSelfDestructing(message)))
            }
        write(account, kept.map { (uid, message) -> record(RecordKind.DELETED, uid, message, revision = 0) })
        // Telegram leaves the files in its cache while deleted messages are kept, so copying can wait.
        kept.forEach { (uid, message) ->
            writer.execute { Faults.guard("history media backup", fallback = Unit) { backups.backUp(account, uid, message) } }
        }
    }

    // Telegram's sameMedia only holds for a photo or file that stayed, so the media are compared here.
    fun beforeEdited(account: Int, dialogId: Long, previous: Any, next: Any, @Suppress("UNUSED_PARAMETER") sameMedia: Boolean) {
        if (!options.get(HistoryOptions.saveEdits, account)) {
            return
        }
        val old = previous as TLRPC.Message
        val new = next as TLRPC.Message
        val change = Change(
            botChat = isBotChat(account, dialogId),
            textChanged = old.message.orEmpty() != new.message.orEmpty(),
            mediaChanged = mediaChanged(old.media, new.media),
            hidden = new.edit_hide,
            selfDestructing = isSelfDestructing(old),
        )
        if (recordsEdit(saveEdits = true, options.get(HistoryOptions.saveInBotChats, account), change)) {
            write(account, listOf(record(RecordKind.EDITED, dialogId, old, revision = old.edit_date)))
            revised.add(account, dialogId, listOf(old.id))
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

    private fun record(kind: RecordKind, dialogId: Long, message: TLRPC.Message, revision: Int): HistoryRecord =
        HistoryRecord(
            kind = kind,
            dialogId = dialogId,
            messageId = message.id,
            revision = revision,
            topicId = message.reply_to?.reply_to_top_id?.toLong() ?: 0L,
            date = message.date,
            recordedAt = System.currentTimeMillis(),
            fromId = MessageObject.getFromChatId(message),
            text = message.message.orEmpty(),
            tlMessage = MessageCodec.encode(message),
            apiLayer = TLRPC.LAYER,
        )

    private fun write(account: Int, records: List<HistoryRecord>) {
        if (records.isNotEmpty()) {
            writer.execute { Faults.guard("history write", fallback = Unit) { stores(account).add(records) } }
        }
    }
}

/**
 * Whether an edit replaced the message's own media. Link previews are not the
 * message's own, and what changes by itself within a kind of media, such as a
 * live location's position or a poll's results, is not an edit of it.
 */
internal fun mediaChanged(old: TLRPC.MessageMedia?, new: TLRPC.MessageMedia?): Boolean = identity(old) != identity(new)

private fun identity(media: TLRPC.MessageMedia?): Pair<Class<*>, Long>? = when (media) {
    null, is TLRPC.TL_messageMediaEmpty, is TLRPC.TL_messageMediaWebPage -> null
    is TLRPC.TL_messageMediaPhoto -> kindOf(media) to (media.photo?.id ?: 0L)
    is TLRPC.TL_messageMediaDocument -> kindOf(media) to (media.document?.id ?: 0L)
    is TLRPC.TL_messageMediaPoll -> kindOf(media) to (media.poll?.id ?: 0L)
    else -> kindOf(media) to 0L
}

// Telegram parses older layers into subclasses of the current ones, which are the kinds.
private fun kindOf(media: TLRPC.MessageMedia): Class<*> {
    var kind: Class<*> = media.javaClass
    while (kind.superclass != null && kind.superclass != TLRPC.MessageMedia::class.java) {
        kind = kind.superclass
    }
    return kind
}
