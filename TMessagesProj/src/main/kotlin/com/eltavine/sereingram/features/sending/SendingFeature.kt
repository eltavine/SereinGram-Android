package com.eltavine.sereingram.features.sending

import android.os.Handler
import android.os.Looper
import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.core.booleanOption
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
import xyz.nextalone.nagram.NaConfig

/** Asks before a tap in the GIF panel sends a GIF, against stray taps; stickers ask through Nagram's own setting. */
object SendingFeature : SereinModule, SettingsContributor {
    override val id: String = "sending"

    override val options: List<Option<*>> = SendingOptions.all

    /** SereinGram's own question before stickers, which Nagram's "Ask before sending sticker" took over. */
    private val askBeforeSticker = booleanOption("sending_ask_before_sticker")

    override fun start(context: ModuleContext) {
        val options = context.options
        // Nagram's settings start Telegram's MessagesController, which never adds its observers if it starts
        // before ApplicationLoader has created the UI handler, after SereinGram starts.
        Handler(Looper.getMainLooper()).post {
            Faults.guard("handing the sticker question to Nagram", fallback = Unit) {
                handStickersToNagram(options) { NaConfig.askBeforeSendingSticker.setConfigBool(true) }
            }
        }
        SendHooks.confirmers.install(Prompt(options))
    }

    /** Has [askNagram] turn Nagram's question on for whoever had SereinGram ask before stickers. */
    internal fun handStickersToNagram(options: Options, askNagram: () -> Unit) {
        if (options.get(askBeforeSticker)) {
            askNagram()
            options.reset(askBeforeSticker)
        }
    }

    private class Prompt(private val options: Options) : SendHooks.Confirmer {
        override fun asks(account: Int, host: Any, send: Runnable): Boolean {
            val chat = host as? BaseFragment
            val context = chat?.parentActivity
            if (!options.get(SendingOptions.askBeforeGif) || context == null) {
                return false
            }
            chat.showDialog(
                AlertDialog.Builder(context, chat.resourceProvider)
                    .setTitle(getString(R.string.serein_sending_confirm_gif))
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
                    SettingsRow.Toggle(SendingOptions.askBeforeGif, R.string.serein_sending_ask_before_gif, summary = R.string.serein_sending_ask_before_gif_info),
                ),
                note = R.string.serein_sending_ask_note,
            ),
        ),
    )
}
