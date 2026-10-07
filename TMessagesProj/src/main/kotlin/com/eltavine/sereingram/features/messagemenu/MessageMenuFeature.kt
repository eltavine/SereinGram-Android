package com.eltavine.sereingram.features.messagemenu

import android.content.DialogInterface
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
import org.telegram.messenger.LocaleController.formatString
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.MessageObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.messenger.UserObject
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.BulletinFactory

/** Extra items in the menu of a message, after NagramX's issues. */
object MessageMenuFeature : SereinModule, SettingsContributor {
    override val id: String = "message_menu"

    override val options: List<Option<*>> = MessageMenuOptions.all

    override fun start(context: ModuleContext) {
        MessageMenuHooks.entries.install(BlockSenderEntry(context.options))
    }

    override val settingsIcon: Int = R.drawable.msg_list

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_message_menu_title,
        listOf(
            SettingsSection(
                header = R.string.serein_message_menu_items,
                rows = listOf(SettingsRow.Toggle(MessageMenuOptions.blockSender, R.string.serein_message_menu_block_sender)),
                note = R.string.serein_message_menu_block_sender_note,
            ),
        ),
    )
}

private class BlockSenderEntry(private val options: Options) : MessageMenuHooks.Entry {
    override val option: Int = MessageMenuHooks.FIRST_OPTION + 101

    override val icon: Int = R.drawable.msg_block

    override fun title(account: Int, message: Any): CharSequence = getString(R.string.BlockUser)

    override fun isShown(account: Int, message: Any): Boolean {
        val shown = message as MessageObject
        val userId = shown.senderId
        val sender = Sender(
            userId = userId,
            isSelf = userId == UserConfig.getInstance(account).clientUserId,
            isBlocked = MessagesController.getInstance(account).blockePeers.indexOfKey(userId) >= 0,
            inGroup = shown.dialogId < 0,
        )
        return offersBlock(options.get(MessageMenuOptions.blockSender), sender)
    }

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

private fun AlertDialog.redPositive() {
    (getButton(DialogInterface.BUTTON_POSITIVE) as? TextView)?.setTextColor(Theme.getColor(Theme.key_text_RedBold))
}
