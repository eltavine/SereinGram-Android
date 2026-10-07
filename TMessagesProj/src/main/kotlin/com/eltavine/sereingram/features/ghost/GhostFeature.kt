package com.eltavine.sereingram.features.ghost

import android.os.SystemClock
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.ChatMenuHooks
import com.eltavine.sereingram.hooks.DialogsHooks
import com.eltavine.sereingram.hooks.RequestHooks
import com.eltavine.sereingram.hooks.SendHooks
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.Components.BulletinFactory

/**
 * Rounds out Nagram's ghost mode after AyuGram and NagramX: reads Nagram lets
 * through are held back as well, chats can be let in on reads or typing, a
 * chat can be marked read on purpose, and its settings get a home here.
 */
object GhostFeature : SereinModule, SettingsContributor {
    override val id: String = "ghost"

    override val options: List<Option<*>> = GhostOptions.all

    private val passes = ReadPasses { SystemClock.elapsedRealtime() }

    override fun start(context: ModuleContext) {
        val gate = GhostGate(context.options, passes)
        RequestHooks.ghostExemptions.install(gate::exempts)
        RequestHooks.interceptors.install(gate::intercept)
        ChatMenuHooks.entries.install(GhostChatEntry(gate))
        DialogsHooks.titleStatuses.install(GhostIndicator(context.options))
        SendHooks.rewriters.install(GhostSending(context.options))
        context.options.addListener { option, _ ->
            if (option == GhostOptions.statusIndicator) {
                GhostIndicator.refresh()
            }
        }
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
                    SettingsRow.Toggle(GhostOptions.statusIndicator, R.string.serein_ghost_indicator),
                    SettingsRow.Screen(R.string.serein_ghost_options, { NagramGhost.settings() }),
                ),
                note = R.string.serein_ghost_note,
            ),
            SettingsSection(
                rows = listOf(SettingsRow.Toggle(GhostOptions.sendScheduled, R.string.serein_ghost_send_scheduled)),
                note = R.string.serein_ghost_send_scheduled_note,
            ),
            SettingsSection(
                rows = listOf(
                    SettingsRow.Screen(R.string.serein_ghost_exceptions, { options ->
                        GhostExceptionsActivity(GhostGate(options, passes))
                    }),
                ),
                note = R.string.serein_ghost_exceptions_note,
            ),
            SettingsSection(
                header = R.string.serein_ghost_quick,
                rows = listOf(SettingsRow.Action(R.string.serein_ghost_add_shortcut, ::addShortcut)),
                note = R.string.serein_ghost_quick_note,
            ),
        ),
    )

    private fun addShortcut(page: BaseFragment) {
        val context = page.parentActivity ?: return
        if (!GhostShortcut.request(context)) {
            BulletinFactory.of(page).createSimpleBulletin(R.raw.error, getString(R.string.serein_ghost_shortcut_unsupported)).show()
        }
    }
}
