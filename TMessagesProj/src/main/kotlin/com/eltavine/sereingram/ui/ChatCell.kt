package com.eltavine.sereingram.ui

import android.content.Context
import android.view.View
import com.eltavine.sereingram.support.Chats
import org.telegram.messenger.ChatObject
import org.telegram.messenger.DialogObject
import org.telegram.messenger.LocaleController
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.MessagesController
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.UserCell
import org.telegram.ui.Components.RecyclerListView
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import org.telegram.ui.Components.UniversalRecyclerView

/**
 * A chat in a list, drawn the way Telegram lists people and groups: avatar, name and a
 * grey line under the name. Without a status of its own, the line is Telegram's: when a
 * person was last seen, or how many members a group or channel has, as its own lists say.
 */
internal class ChatCell private constructor() : UItem.UItemFactory<UserCell>() {
    override fun createView(context: Context, listView: RecyclerListView?, currentAccount: Int, classGuid: Int, resourcesProvider: Theme.ResourcesProvider?) =
        UserCell(context, 6, 0, false, resourcesProvider)

    override fun bindView(view: View, item: UItem, divider: Boolean, adapter: UniversalAdapter?, listView: UniversalRecyclerView?) {
        val cell = view as UserCell
        // Drawn as Saved Messages, the cell leaves out any status, which a list may have to show.
        cell.setSelfAsSavedMessages(item.subtext == null)
        cell.setData(item.`object`, item.object2 as? TLRPC.EncryptedChat, item.text, item.subtext, 0, divider)
    }

    // A list keeps its rows numbered in order, so a chat is told apart by itself.
    override fun equals(a: UItem, b: UItem): Boolean = a.dialogId == b.dialogId

    companion object {
        init {
            setup(ChatCell())
        }

        /** [name] stands for the chat's own, which is also shown while the chat is not loaded. */
        fun of(id: Int, account: Int, dialogId: Long, status: CharSequence? = null, name: CharSequence? = null): UItem {
            val controller = MessagesController.getInstance(account)
            val encrypted = if (DialogObject.isEncryptedDialog(dialogId)) controller.getEncryptedChat(DialogObject.getEncryptedChatId(dialogId)) else null
            val peer: Any? = when {
                encrypted != null -> controller.getUser(encrypted.user_id)
                DialogObject.isUserDialog(dialogId) -> controller.getUser(dialogId)
                DialogObject.isChatDialog(dialogId) -> controller.getChat(-dialogId)
                else -> null
            }
            val savedMessages = encrypted == null && dialogId == UserConfig.getInstance(account).clientUserId
            return UItem.ofFactory(ChatCell::class.java).apply {
                this.id = id
                this.dialogId = dialogId
                `object` = peer
                object2 = encrypted
                text = name ?: when {
                    savedMessages && status != null -> getString(R.string.SavedMessages)
                    peer == null -> Chats.name(account, dialogId)
                    else -> null
                }
                // Telegram writes its own line only for people; a reused row would keep the one it had before.
                subtext = status ?: when (peer) {
                    is TLRPC.User -> null
                    is TLRPC.Chat -> membersOf(peer)
                    else -> ""
                }
            }
        }

        private fun membersOf(chat: TLRPC.Chat): String {
            val channel = ChatObject.isChannelAndNotMegaGroup(chat)
            return when {
                chat.participants_count != 0 -> LocaleController.formatPluralStringComma(if (channel) "Subscribers" else "Members", chat.participants_count)
                channel -> getString(if (ChatObject.isPublic(chat)) R.string.ChannelPublic else R.string.ChannelPrivate)
                else -> getString(if (ChatObject.isPublic(chat)) R.string.MegaPublic else R.string.MegaPrivate)
            }
        }
    }
}
