package com.eltavine.sereingram.features.input

import android.os.Build
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.InputHooks
import com.eltavine.sereingram.settings.SettingsCategory
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import com.eltavine.sereingram.settings.SettingsTint
import org.telegram.messenger.R

/** Pastes copied text without its formatting, after NagramX's request. */
object InputFeature : SereinModule, SettingsContributor {
    override val id: String = "input"

    override val options: List<Option<*>> = InputOptions.all

    // Android only knows pasting as plain text from 6.0 on.
    private val canPastePlainText: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    override fun start(context: ModuleContext) {
        val options = context.options
        InputHooks.pastePolicies.install { canPastePlainText && options.get(InputOptions.plainPaste) }
    }

    override val settingsIcon: Int = R.drawable.msg_copy

    override val settingsCategory: SettingsCategory = SettingsCategory.CHATS

    override val settingsTint: SettingsTint = SettingsTint.ORANGE_DEEP

    override val settingsOrder: Int = 2

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_input_title,
        summary = R.string.serein_input_summary,
        sections = listOf(
            SettingsSection(
                rows = listOf(
                    SettingsRow.Toggle(
                        InputOptions.plainPaste,
                        R.string.serein_input_plain_paste,
                        summary = R.string.serein_input_plain_paste_info,
                        enabledWhen = { canPastePlainText },
                    ),
                ),
                note = R.string.serein_input_plain_paste_note,
            ),
        ),
    )
}
