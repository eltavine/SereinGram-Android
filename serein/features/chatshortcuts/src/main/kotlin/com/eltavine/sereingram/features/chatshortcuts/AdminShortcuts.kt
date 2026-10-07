package com.eltavine.sereingram.features.chatshortcuts

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.booleanOption

/**
 * Screens of a group or channel reachable from its chat's menu instead of
 * through its profile, after Cherrygram, OctoGram, NagramX and exteraGram.
 */
public enum class AdminShortcut(public val option: Option<Boolean>) {
    RECENT_ACTIONS(booleanOption("shortcuts_recent_actions", default = true)),
    ADMINISTRATORS(booleanOption("shortcuts_administrators", default = true)),
    MEMBERS(booleanOption("shortcuts_members", default = true)),
    PERMISSIONS(booleanOption("shortcuts_permissions", default = true)),
    STATISTICS(booleanOption("shortcuts_statistics", default = true)),
    INVITE_LINKS(booleanOption("shortcuts_invite_links")),
}

/** What the user may do in a chat, as far as the shortcuts care. */
public class ChatRights(
    /** A broadcast channel, as opposed to a group. */
    public val isChannel: Boolean,
    public val isSupergroup: Boolean,
    public val isAdmin: Boolean,
    public val canBan: Boolean,
    public val canInvite: Boolean,
    public val canViewStats: Boolean,
)

/** Admin items in the menu of a message, after Swiftgram's. */
public object MessageShortcuts {
    public val restrictMember: Option<Boolean> = booleanOption("shortcuts_restrict_member", default = true)
}

/**
 * Whether the menu of a message from [senderId] offers to restrict them: Telegram restricts
 * members of supergroups only, and only for admins who may ban.
 */
public fun offersRestriction(enabled: Boolean, rights: ChatRights, senderId: Long, isSelf: Boolean): Boolean =
    enabled && rights.isSupergroup && rights.canBan && senderId > 0 && !isSelf

/** Whether [shortcut] leads somewhere the user may go in a chat with [rights]. */
public fun offers(shortcut: AdminShortcut, rights: ChatRights): Boolean = when (shortcut) {
    AdminShortcut.RECENT_ACTIONS -> rights.isAdmin && (rights.isChannel || rights.isSupergroup)
    AdminShortcut.ADMINISTRATORS, AdminShortcut.MEMBERS -> rights.isAdmin || !rights.isChannel
    AdminShortcut.PERMISSIONS -> rights.canBan && !rights.isChannel
    AdminShortcut.STATISTICS -> rights.canViewStats
    AdminShortcut.INVITE_LINKS -> rights.canInvite
}
