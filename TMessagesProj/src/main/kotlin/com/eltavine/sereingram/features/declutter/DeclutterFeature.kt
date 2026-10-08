package com.eltavine.sereingram.features.declutter

import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.ChatHooks
import com.eltavine.sereingram.hooks.MessageHooks
import com.eltavine.sereingram.hooks.SettingsHooks
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import org.telegram.messenger.MessageObject
import org.telegram.messenger.R
import org.telegram.ui.Components.chat.layouts.ChatActivityChannelButtonsLayout

/** Hides Telegram elements, after NagramX's, OctoGram's and Swiftgram's options of the same names. */
object DeclutterFeature : SereinModule, SettingsContributor {
    override val id: String = "declutter"

    override val options: List<Option<*>> = DeclutterOptions.all

    override fun start(context: ModuleContext) {
        val options = context.options
        ChatHooks.shareButtonPolicies.install { _, _, saved ->
            allowShareButton(options.get(DeclutterOptions.hideShareButton), saved)
        }
        // Asked whenever a message draws.
        val hideReactions = options.cached(DeclutterOptions.hideReactions)
        MessageHooks.reactionsPolicies.install { _, message ->
            val tags = (message as MessageObject).messageOwner?.reactions?.reactions_as_tags == true
            hidesReactions(hideReactions.value, tags)
        }
        ChatHooks.channelButtonPolicies.install { button ->
            when (button) {
                ChatActivityChannelButtonsLayout.BUTTON_GIFT -> !options.get(DeclutterOptions.hideChannelGiftButton)
                ChatActivityChannelButtonsLayout.BUTTON_DIRECT -> !options.get(DeclutterOptions.hideChannelMessageButton)
                else -> true
            }
        }
        SettingsHooks.mainFilters.install(MainSettingsDeclutter(options))
    }

    override val settingsIcon: Int = R.drawable.msg_archive_hide

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_declutter_title,
        listOf(
            SettingsSection(
                header = R.string.serein_declutter_settings,
                rows = listOf(
                    SettingsRow.Toggle(DeclutterOptions.hidePremiumSection, R.string.serein_declutter_hide_premium),
                    SettingsRow.Toggle(DeclutterOptions.hideHelpSection, R.string.serein_declutter_hide_help),
                ),
                note = R.string.serein_declutter_settings_note,
            ),
            SettingsSection(
                header = R.string.serein_declutter_chats,
                rows = listOf(
                    SettingsRow.Toggle(DeclutterOptions.hideShareButton, R.string.serein_declutter_hide_share),
                    SettingsRow.Toggle(DeclutterOptions.hideReactions, R.string.serein_declutter_hide_reactions),
                ),
                note = R.string.serein_declutter_chats_note,
            ),
            SettingsSection(
                header = R.string.serein_declutter_channels,
                rows = listOf(
                    SettingsRow.Toggle(DeclutterOptions.hideChannelGiftButton, R.string.serein_declutter_hide_channel_gift),
                    SettingsRow.Toggle(DeclutterOptions.hideChannelMessageButton, R.string.serein_declutter_hide_channel_message),
                ),
                note = R.string.serein_declutter_channels_note,
            ),
        ),
    )
}
