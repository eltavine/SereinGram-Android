package com.eltavine.sereingram.features.history

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.ports.HistoryRecord
import com.eltavine.sereingram.ports.HistoryStore
import org.telegram.messenger.LocaleController
import org.telegram.messenger.MessageObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.TLRPC

/**
 * Puts kept deleted messages back into the history Telegram loads from its
 * database, and marks them as deleted where Telegram draws the time.
 */
internal class HistoryRestorer(
    private val options: Options,
    private val stores: (account: Int) -> HistoryStore,
    private val kept: KeptMessages,
) {
    fun afterLoaded(
        account: Int,
        dialogId: Long,
        mode: Int,
        threadMessageId: Long,
        messages: MutableList<Any?>,
        users: MutableList<Any?>,
        chats: MutableList<Any?>,
    ) {
        if (mode != 0 || threadMessageId != 0L || !options.get(HistoryOptions.saveDeleted, account)) {
            return
        }
        val loadedIds = messages.mapNotNullTo(HashSet()) { message ->
            (message as? TLRPC.Message)?.id?.takeUnless(MessageObject::isEphemeralMessageId)
        }
        val span = BatchSpan.of(loadedIds) ?: return
        val candidates = stores(account).deleted(dialogId, RESTORE_LIMIT, beforeMessageId = span.newest)
        val restored = restorable(span, loadedIds, candidates).mapNotNull { decode(account, dialogId, it) }
        if (restored.isEmpty()) {
            return
        }
        restored.forEach { message ->
            messages.add(insertionIndex(messages.map { (it as? TLRPC.Message)?.id }, message.id), message)
        }
        addSenders(account, restored, users, chats)
        kept.add(account, dialogId, restored.map { it.id })
    }

    fun decorateTime(account: Int, dialogId: Long, messageId: Int, time: String): String =
        if (kept.contains(account, dialogId, messageId)) {
            LocaleController.getString(R.string.serein_history_deleted_mark) + " " + time
        } else {
            time
        }

    private fun decode(account: Int, dialogId: Long, record: HistoryRecord): TLRPC.Message {
        val selfId = UserConfig.getInstance(account).clientUserId
        val message = Faults.guard("history decode", fallback = null) { MessageCodec.decode(record.tlMessage, selfId) }
            ?: textOnly(account, dialogId, record)
        message.dialog_id = dialogId
        return message
    }

    // A record from a layer this build cannot parse still shows its text.
    private fun textOnly(account: Int, dialogId: Long, record: HistoryRecord): TLRPC.Message {
        val controller = MessagesController.getInstance(account)
        return TLRPC.TL_message().apply {
            id = record.messageId
            date = record.date
            message = record.text
            peer_id = controller.getPeer(dialogId)
            from_id = controller.getPeer(record.fromId)
            flags = flags or TLRPC.MESSAGE_FLAG_HAS_FROM_ID
            out = record.fromId == UserConfig.getInstance(account).clientUserId
        }
    }

    private fun addSenders(account: Int, restored: List<TLRPC.Message>, users: MutableList<Any?>, chats: MutableList<Any?>) {
        val userIds = ArrayList<Long>()
        val chatIds = ArrayList<Long>()
        restored.forEach { MessagesStorage.addUsersAndChatsFromMessage(it, userIds, chatIds, null) }
        val knownUsers = users.mapNotNullTo(HashSet()) { (it as? TLRPC.User)?.id }
        val knownChats = chats.mapNotNullTo(HashSet()) { (it as? TLRPC.Chat)?.id }
        val missingUsers = ArrayList(userIds.distinct().filterNot(knownUsers::contains))
        val missingChats = chatIds.distinct().filterNot(knownChats::contains)
        val storage = MessagesStorage.getInstance(account)
        if (missingUsers.isNotEmpty()) {
            val found = ArrayList<TLRPC.User>()
            storage.getUsersInternal(missingUsers, found)
            users.addAll(found)
        }
        if (missingChats.isNotEmpty()) {
            val found = ArrayList<TLRPC.Chat>()
            storage.getChatsInternal(missingChats.joinToString(","), found)
            chats.addAll(found)
        }
    }

    private companion object {
        // Bounds the work per batch in chats where thousands of messages were deleted.
        const val RESTORE_LIMIT = 500
    }
}
