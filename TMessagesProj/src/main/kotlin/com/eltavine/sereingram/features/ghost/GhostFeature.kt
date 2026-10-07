package com.eltavine.sereingram.features.ghost

import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.RequestHooks
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import org.telegram.messenger.R
import org.telegram.tgnet.TLRPC

/**
 * Rounds out Nagram's ghost mode after AyuGram and NagramX: reads Nagram lets
 * through are held back as well, and its settings get a home in SereinGram's.
 */
object GhostFeature : SereinModule, SettingsContributor {
    override val id: String = "ghost"

    override val options: List<Option<*>> = emptyList()

    override fun start(context: ModuleContext) {
        RequestHooks.interceptors.install { _, request -> holdUnheldRead(request) }
    }

    private fun holdUnheldRead(request: Any): Boolean {
        val read = unheldRead(request) ?: return true
        return when (hold(read, NagramGhost.readsHidden)) {
            Hold.SEND -> true
            Hold.DROP -> false
            Hold.SEND_UNCOUNTED -> {
                (request as TLRPC.TL_messages_getMessagesViews).increment = false
                true
            }
        }
    }

    private fun unheldRead(request: Any): UnheldRead? = when (request) {
        is TLRPC.TL_messages_readDiscussion -> UnheldRead.DISCUSSION
        is TLRPC.TL_messages_readEncryptedHistory -> UnheldRead.SECRET_CHAT
        is TLRPC.TL_messages_getMessagesViews -> UnheldRead.VIEW_COUNT.takeIf { request.increment }
        else -> null
    }

    override val settingsIcon: Int = R.drawable.icon_ghost

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_ghost_title,
        listOf(
            SettingsSection(
                rows = listOf(
                    SettingsRow.Switch(R.string.serein_ghost_active, { NagramGhost.isActive }) {
                        NagramGhost.setActive(!NagramGhost.isActive)
                    },
                    SettingsRow.Screen(R.string.serein_ghost_options, NagramGhost::settings),
                ),
                note = R.string.serein_ghost_note,
            ),
        ),
    )
}
