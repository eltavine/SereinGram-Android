package com.eltavine.sereingram.features.history

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.hooks.HistoryHooks
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.MessageObject
import org.telegram.ui.ChatActivity

/** Leaves messages deleted while their chat is open on screen, marked as deleted. */
internal class KeptInChat(
    private val options: Options,
    private val kept: MessageMap<Long>,
    private val deletedByUser: MessageSet,
) : HistoryHooks.ChatKeeper {
    override fun keep(account: Int, channelId: Long, messageIds: List<Int>, chat: Any): Collection<Int> {
        val activity = chat as ChatActivity
        if (activity.chatMode != ChatActivity.MODE_DEFAULT || !options.get(HistoryOptions.saveDeleted, account)) {
            return emptyList()
        }
        val saveInBots = options.get(HistoryOptions.saveInBotChats, account)
        val ids = messageIds.toHashSet()
        val shown = activity.messages.filter { message ->
            val dialogId = message.dialogId
            message.id in ids &&
                message.id > 0 &&
                !MessageObject.isEphemeralMessageId(message.id) &&
                sameIdSpace(account, dialogId, channelId) &&
                !deletedByUser.contains(account, dialogId, message.id) &&
                recordsDeletion(
                    saveDeleted = true,
                    saveInBots,
                    Change(botChat = isBotChat(account, dialogId), selfDestructing = isSelfDestructing(message.messageOwner)),
                )
        }
        if (shown.isEmpty()) {
            return emptyList()
        }
        val now = System.currentTimeMillis()
        shown.forEach { kept.put(account, it.dialogId, it.id, now) }
        AndroidUtilities.runOnUIThread {
            Faults.guard("history redraw", fallback = Unit) {
                shown.forEach { message ->
                    message.forceUpdate = true
                    activity.chatAdapter?.updateRowWithMessageObject(message, false, false)
                }
            }
        }
        return shown.map { it.id }
    }

    // Message ids count per channel in channels and per account everywhere else.
    private fun sameIdSpace(account: Int, dialogId: Long, channelId: Long): Boolean =
        if (channelId != 0L) dialogId == -channelId else !isChannel(account, dialogId)
}
