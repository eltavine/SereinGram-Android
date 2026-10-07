package com.eltavine.sereingram.features.ghost

import org.telegram.messenger.DialogObject
import org.telegram.tgnet.TLRPC

/** A read or typing request and the chat it tells. */
internal class GhostTarget(val action: GhostAction, val dialogId: Long)

internal fun ghostTarget(request: Any): GhostTarget? = when (request) {
    is TLRPC.TL_messages_readHistory -> read(DialogObject.getPeerDialogId(request.peer))
    is TLRPC.TL_channels_readHistory -> read(-request.channel.channel_id)
    is TLRPC.TL_channels_readMessageContents -> read(-request.channel.channel_id)
    is TLRPC.TL_messages_readDiscussion -> read(DialogObject.getPeerDialogId(request.peer))
    is TLRPC.TL_messages_readEncryptedHistory -> read(DialogObject.makeEncryptedDialogId(request.peer.chat_id.toLong()))
    is TLRPC.TL_messages_getMessagesViews -> read(DialogObject.getPeerDialogId(request.peer))
    is TLRPC.TL_messages_setTyping -> GhostTarget(GhostAction.TYPING, DialogObject.getPeerDialogId(request.peer))
    is TLRPC.TL_messages_setEncryptedTyping ->
        GhostTarget(GhostAction.TYPING, DialogObject.makeEncryptedDialogId(request.peer.chat_id.toLong()))
    else -> null
}

private fun read(dialogId: Long) = GhostTarget(GhostAction.READ, dialogId)

/** Reads that Nagram's ghost mode does not hold back by itself. */
internal fun unheldRead(request: Any): UnheldRead? = when (request) {
    is TLRPC.TL_messages_readDiscussion -> UnheldRead.DISCUSSION
    is TLRPC.TL_messages_readEncryptedHistory -> UnheldRead.SECRET_CHAT
    is TLRPC.TL_messages_getMessagesViews -> UnheldRead.VIEW_COUNT.takeIf { request.increment }
    else -> null
}
