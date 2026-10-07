package com.eltavine.sereingram.features.history

import org.telegram.messenger.ChatObject
import org.telegram.messenger.MessagesController

internal fun isBotChat(account: Int, dialogId: Long): Boolean =
    dialogId > 0 && MessagesController.getInstance(account).getUser(dialogId)?.bot == true

internal fun isChannel(account: Int, dialogId: Long): Boolean =
    dialogId < 0 && ChatObject.isChannel(MessagesController.getInstance(account).getChat(-dialogId))
