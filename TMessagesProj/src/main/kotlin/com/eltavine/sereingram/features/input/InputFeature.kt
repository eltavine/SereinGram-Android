package com.eltavine.sereingram.features.input

import android.os.Build
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.InputHooks
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import org.telegram.messenger.R

/** Pastes copied text without its formatting, after NagramX's request. */
object InputFeature : SereinModule, SettingsContributor {
    override val id: String = "input"

    override val options: List<Option<*>> = InputOptions.all

    override fun start(context: ModuleContext) {
        val options = context.options
        // Android only knows pasting as plain text from 6.0 on.
        InputHooks.pastePolicies.install {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && options.get(InputOptions.plainPaste)
        }
    }

    override val settingsIcon: Int = R.drawable.msg_copy

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_input_title,
        listOf(
            SettingsSection(
                rows = listOf(SettingsRow.Toggle(InputOptions.plainPaste, R.string.serein_input_plain_paste)),
                note = R.string.serein_input_plain_paste_note,
            ),
        ),
    )
}
