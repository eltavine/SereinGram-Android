package com.eltavine.sereingram.features.messagemenu

import android.content.DialogInterface
import android.os.Bundle
import android.widget.TextView
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.MessageMenuHooks
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ChatObject
import org.telegram.messenger.LocaleController.formatString
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.MessageObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.messenger.UserObject
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.ChatActivity
import org.telegram.ui.Components.BulletinFactory

/** Extra items in the menu of a message, after NagramX's issues. */
object MessageMenuFeature : SereinModule, SettingsContributor {
    override val id: String = "message_menu"

    override val options: List<Option<*>> = MessageMenuOptions.all

    override fun start(context: ModuleContext) {
        MessageMenuHooks.entries.install(BlockSenderEntry(context.options))
        MessageMenuHooks.entries.install(PrivateReplyEntry(context.options))
    }

    override val settingsIcon: Int = R.drawable.msg_list

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_message_menu_title,
        listOf(
            SettingsSection(
                header = R.string.serein_message_menu_items,
                rows = listOf(
                    SettingsRow.Toggle(MessageMenuOptions.blockSender, R.string.serein_message_menu_block_sender),
                    SettingsRow.Toggle(MessageMenuOptions.replyPrivately, R.string.serein_message_menu_reply_privately),
                ),
                note = R.string.serein_message_menu_items_note,
            ),
        ),
    )
}

private fun sender(account: Int, message: MessageObject): Sender {
    val userId = message.senderId
    return Sender(
        userId = userId,
        isSelf = userId == UserConfig.getInstance(account).clientUserId,
        isBlocked = MessagesController.getInstance(account).blockePeers.indexOfKey(userId) >= 0,
        inGroup = message.dialogId < 0,
    )
}

private class BlockSenderEntry(private val options: Options) : MessageMenuHooks.Entry {
    override val option: Int = MessageMenuHooks.FIRST_OPTION + 101

    override val icon: Int = R.drawable.msg_block

    override fun title(account: Int, message: Any): CharSequence = getString(R.string.BlockUser)

    override fun isShown(account: Int, message: Any): Boolean =
        offersBlock(options.get(MessageMenuOptions.blockSender), sender(account, message as MessageObject))

    override fun onSelected(account: Int, message: Any, host: Any) {
        val fragment = host as BaseFragment
        val context = fragment.parentActivity ?: return
        val controller = MessagesController.getInstance(account)
        val user = controller.getUser((message as MessageObject).senderId) ?: return
        val dialog = AlertDialog.Builder(context, fragment.resourceProvider)
            .setTitle(getString(R.string.BlockUser))
            .setMessage(AndroidUtilities.replaceTags(formatString(R.string.AreYouSureBlockContact2, UserObject.getUserName(user))))
            .setPositiveButton(getString(R.string.BlockContact)) { _, _ ->
                controller.blockPeer(user.id)
                BulletinFactory.of(fragment).createBanBulletin(true).show()
            }
            .setNegativeButton(getString(R.string.Cancel), null)
            .create()
        fragment.showDialog(dialog)
        dialog.redPositive()
    }
}

/** Opens the private chat with the sender, replying there to their message, as Telegram's "Reply in Another Chat" does. */
private class PrivateReplyEntry(private val options: Options) : MessageMenuHooks.Entry {
    override val option: Int = MessageMenuHooks.FIRST_OPTION + 102

    override val icon: Int = R.drawable.msg_forward_replace

    override fun title(account: Int, message: Any): CharSequence = getString(R.string.serein_message_menu_reply_privately)

    override fun isShown(account: Int, message: Any): Boolean {
        val shown = message as MessageObject
        return offersPrivateReply(options.get(MessageMenuOptions.replyPrivately), sender(account, shown), replyable(account, shown))
    }

    // The same messages Telegram keeps out of replies from another chat.
    private fun replyable(account: Int, message: MessageObject): Boolean {
        val controller = MessagesController.getInstance(account)
        val chat = controller.getChat(-message.dialogId)
        return message.isSent && !message.scheduled && !message.isSponsored &&
            message.messageOwner !is TLRPC.TL_messageService && message.messageOwner?.noforwards != true &&
            !message.isVoiceOnce && !message.isRoundOnce && message.type != MessageObject.TYPE_GIFT_STARS &&
            !controller.isChatNoForwards(chat) && !ChatObject.isMonoForum(chat)
    }

    override fun onSelected(account: Int, message: Any, host: Any) {
        val fragment = host as BaseFragment
        val shown = message as MessageObject
        val args = Bundle().apply { putLong("user_id", shown.senderId) }
        if (!MessagesController.getInstance(account).checkCanOpenChat(args, fragment)) {
            return
        }
        val privateChat = ChatActivity(args)
        if (fragment.presentFragment(privateChat)) {
            privateChat.showFieldPanelForReplyQuote(shown, null)
        }
    }
}

private fun AlertDialog.redPositive() {
    (getButton(DialogInterface.BUTTON_POSITIVE) as? TextView)?.setTextColor(Theme.getColor(Theme.key_text_RedBold))
}
