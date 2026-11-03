package com.eltavine.sereingram.features.join

import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.DialogObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC

/** Count fresh server dialogs, including the archive; never present a partial count as complete. */
internal class JoinDiagnostics(private val account: Int, private val owner: Long) {
    private val joined = HashSet<Long>()
    private val cursors = HashSet<String>()
    private var pages = 0
    private var missingChat = false

    fun run(done: (Int, Boolean, String?) -> Unit) = fetch(0, 0, TLRPC.TL_inputPeerEmpty(), done)

    private fun fetch(id: Int, date: Int, peer: TLRPC.InputPeer, done: (Int, Boolean, String?) -> Unit) {
        if (UserConfig.getInstance(account).clientUserId != owner) return
        val request = TLRPC.TL_messages_getDialogs().apply {
            limit = 100; offset_id = id; offset_date = date; offset_peer = peer; hash = 0
        }
        ConnectionsManager.getInstance(account).sendRequest(request, { response, error -> AndroidUtilities.runOnUIThread {
            if (UserConfig.getInstance(account).clientUserId != owner) return@runOnUIThread
            val result = response as? TLRPC.messages_Dialogs
            if (error != null || result == null) { done(joined.size, false, error?.text ?: "Unexpected response"); return@runOnUIThread }
            if (result is TLRPC.TL_messages_dialogsNotModified) { done(joined.size, false, "Server did not return dialog data"); return@runOnUIThread }
            val controller = MessagesController.getInstance(account)
            controller.putUsers(result.users, false)
            controller.putChats(result.chats, false)
            for (dialog in result.dialogs) {
                val channel = dialog.peer?.channel_id ?: 0
                if (channel == 0L) continue
                val chat = result.chats.firstOrNull { it.id == channel }
                if (chat == null || chat is TLRPC.TL_channelForbidden) missingChat = true
                else if (!chat.left && !chat.kicked) joined.add(channel)
            }
            if (result is TLRPC.TL_messages_dialogs || result.dialogs.isEmpty()) {
                done(joined.size, !missingChat, null)
                return@runOnUIThread
            }
            val last = result.dialogs.lastOrNull { !it.pinned && it.peer != null }
            val top = last?.let { dialog -> result.messages.firstOrNull { message ->
                message.id == dialog.top_message && DialogObject.getPeerDialogId(message.peer_id) == DialogObject.getPeerDialogId(dialog.peer)
            } }
            val cursor = "${last?.peer?.channel_id}:${last?.peer?.chat_id}:${last?.peer?.user_id}:${last?.top_message}"
            if (++pages >= 20 || last == null || top == null || !cursors.add(cursor)) done(joined.size, false, null)
            else fetch(last.top_message, top.date, controller.getInputPeer(DialogObject.getPeerDialogId(last.peer)), done)
        } }, ConnectionsManager.RequestFlagFailOnServerErrors)
    }
}
