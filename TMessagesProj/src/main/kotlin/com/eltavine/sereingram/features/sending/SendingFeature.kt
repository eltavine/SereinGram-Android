package com.eltavine.sereingram.features.sending

import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.SendHooks
import com.eltavine.sereingram.settings.SettingsCategory
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import com.eltavine.sereingram.settings.SettingsTint
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.BaseFragment

/** Asks before a tap in the sticker and GIF panel sends something, against stray taps. */
object SendingFeature : SereinModule, SettingsContributor {
    override val id: String = "sending"

    override val options: List<Option<*>> = SendingOptions.all

    override fun start(context: ModuleContext) {
        SendHooks.confirmers.install(Prompt(context.options))
    }

    private class Prompt(private val options: Options) : SendHooks.Confirmer {
        override fun asks(account: Int, tapped: SendHooks.Tapped, host: Any, send: Runnable): Boolean {
            val (option, question) = when (tapped) {
                SendHooks.Tapped.STICKER -> SendingOptions.askBeforeSticker to R.string.serein_sending_confirm_sticker
                SendHooks.Tapped.GIF -> SendingOptions.askBeforeGif to R.string.serein_sending_confirm_gif
            }
            val chat = host as? BaseFragment
            val context = chat?.parentActivity
            if (!options.get(option) || context == null) {
                return false
            }
            chat.showDialog(
                AlertDialog.Builder(context, chat.resourceProvider)
                    .setTitle(getString(question))
                    .setPositiveButton(getString(R.string.Send)) { _, _ -> send.run() }
                    .setNegativeButton(getString(R.string.Cancel), null)
                    .create(),
            )
            return true
        }
    }

    override val settingsIcon: Int = R.drawable.msg_emoji_stickers

    override val settingsCategory: SettingsCategory = SettingsCategory.CHATS

    override val settingsTint: SettingsTint = SettingsTint.VIOLET

    override val settingsOrder: Int = 3

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_sending_title,
        summary = R.string.serein_sending_summary,
        sections = listOf(
            SettingsSection(
                rows = listOf(
                    SettingsRow.Toggle(
                        SendingOptions.askBeforeSticker,
                        R.string.serein_sending_ask_before_sticker,
                        summary = R.string.serein_sending_ask_before_sticker_info,
                    ),
                    SettingsRow.Toggle(SendingOptions.askBeforeGif, R.string.serein_sending_ask_before_gif, summary = R.string.serein_sending_ask_before_gif_info),
                ),
                note = R.string.serein_sending_ask_note,
            ),
        ),
    )
}
