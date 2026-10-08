package com.eltavine.sereingram.features.declutter

import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.ChatHooks
import com.eltavine.sereingram.hooks.MessageHooks
import com.eltavine.sereingram.hooks.SettingsHooks
import com.eltavine.sereingram.settings.SettingsCategory
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import com.eltavine.sereingram.settings.SettingsTint
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

    override val settingsCategory: SettingsCategory = SettingsCategory.INTERFACE

    override val settingsTint: SettingsTint = SettingsTint.GRAY

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_declutter_title,
        summary = R.string.serein_declutter_summary,
        sections = listOf(
            SettingsSection(
                header = R.string.serein_declutter_settings,
                rows = listOf(
                    SettingsRow.Toggle(
                        DeclutterOptions.hidePremiumSection,
                        R.string.serein_declutter_hide_premium,
                        summary = R.string.serein_declutter_hide_premium_info,
                    ),
                    SettingsRow.Toggle(DeclutterOptions.hideHelpSection, R.string.serein_declutter_hide_help, summary = R.string.serein_declutter_hide_help_info),
                ),
            ),
            SettingsSection(
                header = R.string.serein_declutter_chats,
                rows = listOf(
                    SettingsRow.Toggle(DeclutterOptions.hideShareButton, R.string.serein_declutter_hide_share, summary = R.string.serein_declutter_hide_share_info),
                    SettingsRow.Toggle(
                        DeclutterOptions.hideReactions,
                        R.string.serein_declutter_hide_reactions,
                        summary = R.string.serein_declutter_hide_reactions_info,
                    ),
                ),
                note = R.string.serein_declutter_chats_note,
            ),
            SettingsSection(
                header = R.string.serein_declutter_channels,
                rows = listOf(
                    SettingsRow.Toggle(
                        DeclutterOptions.hideChannelGiftButton,
                        R.string.serein_declutter_hide_channel_gift,
                        summary = R.string.serein_declutter_hide_channel_gift_info,
                    ),
                    SettingsRow.Toggle(
                        DeclutterOptions.hideChannelMessageButton,
                        R.string.serein_declutter_hide_channel_message,
                        summary = R.string.serein_declutter_hide_channel_message_info,
                    ),
                ),
            ),
        ),
    )
}
