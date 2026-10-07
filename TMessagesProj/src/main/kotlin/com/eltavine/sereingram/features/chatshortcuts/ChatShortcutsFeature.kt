package com.eltavine.sereingram.features.chatshortcuts

import android.os.Bundle
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.ChatMenuHooks
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import org.telegram.messenger.ChatObject
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.MessagesController
import org.telegram.messenger.R
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ChannelAdminLogActivity
import org.telegram.ui.ChatUsersActivity
import org.telegram.ui.ManageLinksActivity
import org.telegram.ui.StatisticActivity

/** Admin screens of a group or channel in its chat's menu, each one optional. */
object ChatShortcutsFeature : SereinModule, SettingsContributor {
    override val id: String = "chat_shortcuts"

    override val options: List<Option<*>> = AdminShortcut.entries.map { it.option }

    override fun start(context: ModuleContext) {
        AdminShortcut.entries.forEach { ChatMenuHooks.entries.install(ShortcutEntry(it, context.options)) }
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
        ),
    )
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
        val group = MessagesController.getInstance(account).getChat(-dialogId) ?: return
        (chat as BaseFragment).presentFragment(screen(group))
    }

    private fun rights(chat: TLRPC.Chat, full: TLRPC.ChatFull?) = ChatRights(
        isChannel = ChatObject.isChannelAndNotMegaGroup(chat),
        isSupergroup = ChatObject.isMegagroup(chat),
        isAdmin = chat.creator || ChatObject.hasAdminRights(chat),
        canBan = ChatObject.canBlockUsers(chat),
        canInvite = ChatObject.canUserDoAdminAction(chat, ChatObject.ACTION_INVITE),
        canViewStats = full?.can_view_stats == true,
    )

    private fun screen(chat: TLRPC.Chat): BaseFragment = when (shortcut) {
        AdminShortcut.RECENT_ACTIONS -> ChannelAdminLogActivity(chat)
        AdminShortcut.ADMINISTRATORS -> users(chat, ChatUsersActivity.TYPE_ADMIN)
        AdminShortcut.MEMBERS -> users(chat, ChatUsersActivity.TYPE_USERS)
        AdminShortcut.PERMISSIONS -> users(chat, ChatUsersActivity.TYPE_KICKED)
        AdminShortcut.STATISTICS -> StatisticActivity.create(chat)
        AdminShortcut.INVITE_LINKS -> ManageLinksActivity(chat.id, 0, 0)
    }

    private fun users(chat: TLRPC.Chat, type: Int) = ChatUsersActivity(
        Bundle().apply {
            putLong("chat_id", chat.id)
            putInt("type", type)
        },
    )
}

private fun title(shortcut: AdminShortcut): Int = when (shortcut) {
    AdminShortcut.RECENT_ACTIONS -> R.string.EventLog
    AdminShortcut.ADMINISTRATORS -> R.string.ChannelAdministrators
    AdminShortcut.MEMBERS -> R.string.ChannelMembers
    AdminShortcut.PERMISSIONS -> R.string.ChannelPermissions
    AdminShortcut.STATISTICS -> R.string.Statistics
    AdminShortcut.INVITE_LINKS -> R.string.InviteLinks
}
