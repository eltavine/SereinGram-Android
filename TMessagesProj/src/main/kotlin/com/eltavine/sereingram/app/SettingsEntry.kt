package com.eltavine.sereingram.app

import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.hooks.SettingsHooks
import com.eltavine.sereingram.settings.SereinSettingsActivity
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import org.telegram.messenger.BuildConfig
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.BaseFragment

private const val ENTRY_ID = 1001
private const val SOURCE_URL = "https://github.com/eltavine/SereinGram-Android"
private const val RELEASES_URL = "$SOURCE_URL/releases"
private const val CHAT_URL = "https://t.me/SereinGram_chat"

/** The SereinGram row in Telegram's settings and the page it opens. */
internal fun installSettingsEntry(options: Options, sections: List<SettingsSection>): AutoCloseable {
    val page = SettingsPage(R.string.NekoX, sections + aboutSection())
    val entry = SettingsHooks.Entry(
        ENTRY_ID,
        R.drawable.serein_menu,
        0xFF3F3F46.toInt(),
        0xFF18181B.toInt(),
        { getString(R.string.NekoX) },
        { getString(R.string.serein_settings_entry_info) },
    ) { host -> (host as BaseFragment).presentFragment(SereinSettingsActivity(page, options)) }
    return SettingsHooks.mainEntries.install(entry)
}

private fun aboutSection() = SettingsSection(
    header = R.string.serein_settings_about,
    rows = listOf(
        SettingsRow.Link(R.string.serein_settings_version, RELEASES_URL) { BuildConfig.VERSION_NAME },
        SettingsRow.Link(R.string.serein_settings_source, SOURCE_URL),
        SettingsRow.Link(R.string.serein_settings_chat, CHAT_URL),
    ),
    note = R.string.serein_settings_about_note,
)
