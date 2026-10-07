package com.eltavine.sereingram.features.ghost

import com.eltavine.sereingram.core.Options
import org.telegram.messenger.MessagesController
import org.telegram.tgnet.TLRPC

/**
 * Decides which ghost-mode requests go out anyway: those of chats with an
 * exception, and the next read of a chat marked read on purpose. Nagram's
 * filter asks about the requests it holds back, the interceptor about the rest,
 * so that each request uses up a pass at most once.
 */
internal class GhostGate(private val options: Options, private val passes: ReadPasses) {
    fun exempts(account: Int, request: Any): Boolean = unheldRead(request) == null && letsThrough(account, request)

    fun intercept(account: Int, request: Any): Boolean {
        val read = unheldRead(request) ?: return true
        if (letsThrough(account, request)) {
            return true
        }
        return when (hold(read, NagramGhost.readsHidden)) {
            Hold.SEND -> true
            Hold.DROP -> false
            Hold.SEND_UNCOUNTED -> {
                (request as TLRPC.TL_messages_getMessagesViews).increment = false
                true
            }
        }
    }

    fun isExcepted(account: Int, dialogId: Long, action: GhostAction): Boolean =
        dialogId in DialogIds.parse(options.get(GhostOptions.exceptions(action), account))

    fun exceptedChats(account: Int): Set<Long> =
        GhostAction.entries.flatMapTo(LinkedHashSet()) { DialogIds.parse(options.get(GhostOptions.exceptions(it), account)) }

    fun setExcepted(account: Int, dialogId: Long, action: GhostAction, excepted: Boolean) {
        val option = GhostOptions.exceptions(action)
        options.set(option, DialogIds.with(options.get(option, account), dialogId, excepted), account)
    }

    /** Tells the chat its messages were read, once, even in ghost mode. */
    fun markRead(account: Int, dialogId: Long) {
        val controller = MessagesController.getInstance(account)
        val dialog = controller.dialogs_dict.get(dialogId) ?: return
        passes.grant(account, dialogId)
        controller.markDialogAsRead(dialogId, dialog.top_message, dialog.top_message, dialog.last_message_date, false, 0, 0, true, 0)
    }

    private fun letsThrough(account: Int, request: Any): Boolean {
        val target = ghostTarget(request) ?: return false
        return target.action == GhostAction.READ && passes.use(account, target.dialogId) ||
            isExcepted(account, target.dialogId, target.action)
    }
}
