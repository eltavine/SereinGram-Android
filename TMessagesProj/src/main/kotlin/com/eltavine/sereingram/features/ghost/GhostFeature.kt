package com.eltavine.sereingram.features.ghost

import android.os.SystemClock
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.ChatMenuHooks
import com.eltavine.sereingram.hooks.DialogsHooks
import com.eltavine.sereingram.hooks.RequestHooks
import com.eltavine.sereingram.hooks.SecretChatHooks
import com.eltavine.sereingram.hooks.SendHooks
import com.eltavine.sereingram.settings.SettingsCategory
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import com.eltavine.sereingram.settings.SettingsTint
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
        SecretChatHooks.readPolicies.install(gate::sendsSecretRead)
        ChatMenuHooks.entries.install(GhostChatEntry(gate))
        DialogsHooks.titleStatuses.install(GhostIndicator(context.options))
        val sending = GhostSending(context.options)
        SendHooks.rewriters.install(sending)
        SendHooks.forwardSchedulers.install(sending)
        context.options.addListener { option, _ ->
            if (option == GhostOptions.statusIndicator) {
                GhostIndicator.refresh()
            }
        }
    }

    override val settingsIcon: Int = R.drawable.icon_ghost

    override val settingsCategory: SettingsCategory = SettingsCategory.PRIVACY

    override val settingsTint: SettingsTint = SettingsTint.INDIGO

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_ghost_title,
        summary = R.string.serein_ghost_summary,
        status = { getString(R.string.serein_settings_on).takeIf { NagramGhost.isActive } },
        sections = listOf(
            SettingsSection(
                rows = listOf(
                    SettingsRow.Switch(
                        R.string.serein_ghost_active,
                        isOn = { NagramGhost.isActive },
                        toggle = { NagramGhost.setActive(!NagramGhost.isActive) },
                        summary = R.string.serein_ghost_active_info,
                    ),
                    SettingsRow.Toggle(GhostOptions.statusIndicator, R.string.serein_ghost_indicator, summary = R.string.serein_ghost_indicator_info),
                    SettingsRow.Screen(
                        R.string.serein_ghost_options,
                        open = { NagramGhost.settings() },
                        summary = R.string.serein_ghost_options_info,
                        icon = R.drawable.msg_settings,
                    ),
                ),
                note = R.string.serein_ghost_note,
            ),
            SettingsSection(
                header = R.string.serein_ghost_sending,
                rows = listOf(
                    SettingsRow.Toggle(GhostOptions.sendScheduled, R.string.serein_ghost_send_scheduled, summary = R.string.serein_ghost_send_scheduled_info),
                ),
                note = R.string.serein_ghost_send_scheduled_note,
            ),
            SettingsSection(
                rows = listOf(
                    SettingsRow.Screen(
                        R.string.serein_ghost_exceptions,
                        open = { state -> GhostExceptionsActivity(GhostGate(state.options, passes)) },
                        summary = R.string.serein_ghost_exceptions_info,
                        icon = R.drawable.msg_contacts,
                        value = { state -> GhostGate(state.options, passes).exceptedChats(state.account).size.takeIf { it > 0 }?.toString() },
                    ),
                ),
                note = R.string.serein_ghost_exceptions_note,
            ),
            SettingsSection(
                header = R.string.serein_ghost_quick,
                rows = listOf(
                    SettingsRow.Action(
                        R.string.serein_ghost_add_shortcut,
                        run = { page -> addShortcut(page as BaseFragment) },
                        summary = R.string.serein_ghost_add_shortcut_info,
                        icon = R.drawable.msg_home,
                    ),
                ),
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
