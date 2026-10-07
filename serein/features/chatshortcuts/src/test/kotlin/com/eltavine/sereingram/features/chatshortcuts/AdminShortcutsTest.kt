package com.eltavine.sereingram.features.chatshortcuts

import kotlin.test.Test
import kotlin.test.assertEquals

class AdminShortcutsTest {
    private fun rights(
        isChannel: Boolean = false,
        isSupergroup: Boolean = true,
        isAdmin: Boolean = false,
        canBan: Boolean = false,
        canInvite: Boolean = false,
        canViewStats: Boolean = false,
    ) = ChatRights(isChannel, isSupergroup, isAdmin, canBan, canInvite, canViewStats)

    private fun offered(rights: ChatRights) = AdminShortcut.entries.filter { offers(it, rights) }.toSet()

    @Test
    fun membersOfAGroupSeeWhoIsInIt() {
        assertEquals(setOf(AdminShortcut.ADMINISTRATORS, AdminShortcut.MEMBERS), offered(rights()))
    }

    @Test
    fun adminsGetTheScreensTheirRightsOpen() {
        val admin = rights(isAdmin = true, canBan = true, canInvite = true, canViewStats = true)
        assertEquals(AdminShortcut.entries.toSet(), offered(admin))
        val basicGroupAdmin = rights(isSupergroup = false, isAdmin = true)
        assertEquals(setOf(AdminShortcut.ADMINISTRATORS, AdminShortcut.MEMBERS), offered(basicGroupAdmin))
    }

    @Test
    fun channelSubscribersGetNothingAndChannelAdminsNoPermissions() {
        assertEquals(emptySet(), offered(rights(isChannel = true, isSupergroup = false)))
        val channelAdmin = rights(isChannel = true, isSupergroup = false, isAdmin = true, canBan = true, canViewStats = true)
        assertEquals(
            setOf(AdminShortcut.RECENT_ACTIONS, AdminShortcut.ADMINISTRATORS, AdminShortcut.MEMBERS, AdminShortcut.STATISTICS),
            offered(channelAdmin),
        )
    }

    @Test
    fun storageKeysNeverChange() {
        assertEquals(
            listOf(
                "shortcuts_recent_actions",
                "shortcuts_administrators",
                "shortcuts_members",
                "shortcuts_permissions",
                "shortcuts_statistics",
                "shortcuts_invite_links",
            ),
            AdminShortcut.entries.map { it.option.key },
        )
    }
}
