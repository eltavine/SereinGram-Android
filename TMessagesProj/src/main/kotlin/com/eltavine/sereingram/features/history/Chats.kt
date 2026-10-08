package com.eltavine.sereingram.features.history

import org.telegram.messenger.ChatObject
import org.telegram.messenger.MessagesController
import org.telegram.tgnet.TLRPC

internal fun isBotChat(account: Int, dialogId: Long): Boolean =
    dialogId > 0 && MessagesController.getInstance(account).getUser(dialogId)?.bot == true

internal fun isChannel(account: Int, dialogId: Long): Boolean =
    dialogId < 0 && ChatObject.isChannel(MessagesController.getInstance(account).getChat(-dialogId))

/** An auto-delete or secret chat timer, or media that can be viewed once. */
internal fun isSelfDestructing(message: TLRPC.Message): Boolean =
    message.ttl_period > 0 || message.ttl > 0 || (message.media?.ttl_seconds ?: 0) > 0
