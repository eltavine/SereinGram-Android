package com.eltavine.sereingram.support

import android.os.Bundle
import org.telegram.messenger.DialogObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.UserObject
import org.telegram.ui.ChatActivity

/** Telegram chats as features name and open them. */
object Chats {
    /** The name Telegram shows for a chat, or its id while the chat is not cached. */
    fun name(account: Int, dialogId: Long): String {
        val controller = MessagesController.getInstance(account)
        val name = when {
            DialogObject.isEncryptedDialog(dialogId) ->
                controller.getEncryptedChat(DialogObject.getEncryptedChatId(dialogId))
                    ?.let { UserObject.getUserName(controller.getUser(it.user_id)) }
            DialogObject.isUserDialog(dialogId) -> controller.getUser(dialogId)?.let(UserObject::getUserName)
            else -> controller.getChat(-dialogId)?.title
        }
        return name ?: dialogId.toString()
    }

    /** The screen of a chat, scrolled to [messageId] when there is one. */
    fun screen(dialogId: Long, messageId: Int = 0): ChatActivity = when {
        DialogObject.isEncryptedDialog(dialogId) ->
            ChatActivity(Bundle().apply { putInt("enc_id", DialogObject.getEncryptedChatId(dialogId)) })
        messageId > 0 -> ChatActivity.of(dialogId, messageId)
        else -> ChatActivity.of(dialogId)
    }
}
