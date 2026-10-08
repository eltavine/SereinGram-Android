package com.eltavine.sereingram.features.chatshortcuts

import android.os.Bundle
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.ChatMenuHooks
import com.eltavine.sereingram.hooks.MessageMenuHooks
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ChatObject
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.MessageObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ChannelAdminLogActivity
import org.telegram.ui.ChatRightsEditActivity
import org.telegram.ui.ChatUsersActivity
import org.telegram.ui.Components.BulletinFactory
import org.telegram.ui.ManageLinksActivity
import org.telegram.ui.StatisticActivity
import tw.nekomimi.nekogram.NekoConfig

/** Admin screens of a group or channel in its chat's menu, each one optional. */
object ChatShortcutsFeature : SereinModule, SettingsContributor {
    override val id: String = "chat_shortcuts"

    override val options: List<Option<*>> = AdminShortcut.entries.map { it.option } + MessageShortcuts.restrictMember

    override fun start(context: ModuleContext) {
        AdminShortcut.entries.forEach { ChatMenuHooks.entries.install(ShortcutEntry(it, context.options)) }
        MessageMenuHooks.entries.install(RestrictEntry(context.options))
    }

    override val settingsIcon: Int = R.drawable.msg_admins

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_shortcuts_title,
        listOf(
            SettingsSection(
                header = R.string.serein_shortcuts_header,
                rows = AdminShortcut.entries.map { SettingsRow.Toggle(it.option, title(it)) },
                note = R.string.serein_shortcuts_note,
            ),
            SettingsSection(
                header = R.string.serein_shortcuts_message_header,
                rows = listOf(SettingsRow.Toggle(MessageShortcuts.restrictMember, R.string.serein_shortcuts_restrict)),
                note = R.string.serein_shortcuts_restrict_note,
            ),
        ),
    )
}

/** Opens Telegram's restrictions of the sender of a message, after looking up their current ones. */
private class RestrictEntry(private val options: Options) : MessageMenuHooks.Entry {
    override val option: Int = MessageMenuHooks.FIRST_OPTION + 401

    override val icon: Int = R.drawable.msg_permissions

    override fun title(account: Int, message: Any): CharSequence = getString(R.string.ChangePermissions)

    override fun isShown(account: Int, message: Any): Boolean {
        val shown = message as MessageObject
        if (shown.dialogId >= 0 || shown.scheduled || !shown.isSent) {
            return false
        }
        val controller = MessagesController.getInstance(account)
        val chat = controller.getChat(-shown.dialogId) ?: return false
        val sender = shown.senderId
        val isSelf = sender == UserConfig.getInstance(account).clientUserId
        return offersRestriction(options.get(MessageShortcuts.restrictMember), rights(chat, null), sender, isSelf) &&
            !nagramOffers(controller.getChatFull(chat.id), sender)
    }

    // Nagram's own item appears for members the chat has already loaded; this one covers the rest.
    private fun nagramOffers(full: TLRPC.ChatFull?, sender: Long): Boolean =
        NekoConfig.showChangePermissions.Bool() && full?.participants?.participants?.any { it.user_id == sender } == true

    override fun onSelected(account: Int, message: Any, host: Any) {
        val fragment = host as BaseFragment
        val shown = message as MessageObject
        val controller = MessagesController.getInstance(account)
        val chat = controller.getChat(-shown.dialogId) ?: return
        val userId = shown.senderId
        val request = TLRPC.TL_channels_getParticipant().apply {
            channel = controller.getInputChannel(chat.id)
            participant = controller.getInputPeer(userId)
        }
        ConnectionsManager.getInstance(account).sendRequest(request) { response, _ ->
            AndroidUtilities.runOnUIThread {
                val member = (response as? TLRPC.TL_channels_channelParticipant)?.participant
                if (member is TLRPC.TL_channelParticipantAdmin || member is TLRPC.TL_channelParticipantCreator) {
                    BulletinFactory.of(fragment).createSimpleBulletin(R.raw.error, getString(R.string.serein_shortcuts_restrict_admin)).show()
                    return@runOnUIThread
                }
                val restrictions = ChatRightsEditActivity(
                    userId,
                    chat.id,
                    null,
                    chat.default_banned_rights,
                    member?.banned_rights,
                    "",
                    ChatRightsEditActivity.TYPE_BANNED,
                    true,
                    false,
                    null,
                )
                fragment.presentFragment(restrictions)
            }
        }
    }
}

private class ShortcutEntry(private val shortcut: AdminShortcut, private val options: Options) : ChatMenuHooks.Entry {
    override val id: Int = ChatMenuHooks.FIRST_ID + 10 + shortcut.ordinal

    override val icon: Int = when (shortcut) {
        AdminShortcut.RECENT_ACTIONS -> R.drawable.msg_log
        AdminShortcut.ADMINISTRATORS -> R.drawable.msg_admins
        AdminShortcut.MEMBERS -> R.drawable.msg_groups
        AdminShortcut.PERMISSIONS -> R.drawable.msg_permissions
        AdminShortcut.STATISTICS -> R.drawable.msg_stats
        AdminShortcut.INVITE_LINKS -> R.drawable.msg_link2
    }

    override fun title(account: Int, dialogId: Long): CharSequence = getString(title(shortcut))

    override fun isShown(account: Int, dialogId: Long): Boolean {
        if (dialogId >= 0 || !options.get(shortcut.option)) {
            return false
        }
        val controller = MessagesController.getInstance(account)
        val chat = controller.getChat(-dialogId) ?: return false
        if (ChatObject.isNotInChat(chat)) {
            return false
        }
        return offers(shortcut, rights(chat, controller.getChatFull(chat.id)))
    }

    override fun onSelected(account: Int, dialogId: Long, chat: Any) {
        val controller = MessagesController.getInstance(account)
        val group = controller.getChat(-dialogId) ?: return
        val screen = screen(group, controller.getChatFull(group.id)) ?: return
        (chat as BaseFragment).presentFragment(screen)
    }

    private fun screen(chat: TLRPC.Chat, full: TLRPC.ChatFull?): BaseFragment? = when (shortcut) {
        AdminShortcut.RECENT_ACTIONS -> ChannelAdminLogActivity(chat)
        AdminShortcut.ADMINISTRATORS -> users(chat, full, ChatUsersActivity.TYPE_ADMIN)
        AdminShortcut.MEMBERS -> users(chat, full, ChatUsersActivity.TYPE_USERS)
        AdminShortcut.PERMISSIONS -> users(chat, full, ChatUsersActivity.TYPE_KICKED)
        AdminShortcut.STATISTICS -> StatisticActivity.create(chat, false)
        AdminShortcut.INVITE_LINKS -> full?.let { info -> ManageLinksActivity(chat.id, 0, 0).apply { setInfo(info, info.exported_invite) } }
    }

    private fun users(chat: TLRPC.Chat, full: TLRPC.ChatFull?, type: Int) = ChatUsersActivity(
        Bundle().apply {
            putLong("chat_id", chat.id)
            putInt("type", type)
        },
    ).apply { setInfo(full) }
}

private fun rights(chat: TLRPC.Chat, full: TLRPC.ChatFull?) = ChatRights(
    isChannel = ChatObject.isChannelAndNotMegaGroup(chat),
    isSupergroup = ChatObject.isMegagroup(chat),
    isAdmin = chat.creator || ChatObject.hasAdminRights(chat),
    canBan = ChatObject.canBlockUsers(chat),
    canInvite = ChatObject.canUserDoAdminAction(chat, ChatObject.ACTION_INVITE),
    canViewStats = full?.can_view_stats == true,
    hasFullInfo = full != null,
)

private fun title(shortcut: AdminShortcut): Int = when (shortcut) {
    AdminShortcut.RECENT_ACTIONS -> R.string.EventLog
    AdminShortcut.ADMINISTRATORS -> R.string.ChannelAdministrators
    AdminShortcut.MEMBERS -> R.string.ChannelMembers
    AdminShortcut.PERMISSIONS -> R.string.ChannelPermissions
    AdminShortcut.STATISTICS -> R.string.Statistics
    AdminShortcut.INVITE_LINKS -> R.string.InviteLinks
}
