package com.eltavine.sereingram.app

import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.hooks.SettingsHooks
import com.eltavine.sereingram.settings.SettingsCategory
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import com.eltavine.sereingram.settings.featureSections
import com.eltavine.sereingram.settings.ui.SereinSettingsActivity
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
    val page = rootPage(sections)
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

/** The SereinGram page: [sections] and, closing them, what SereinGram is and where it lives. */
internal fun rootPage(sections: List<SettingsSection>): SettingsPage = SettingsPage(R.string.NekoX, sections + aboutSection())

/** The groups of the SereinGram page that open the pages of [contributors]. */
internal fun featureSectionsOf(contributors: List<SettingsContributor>): List<SettingsSection> =
    featureSections(contributors, note = ::categoryNote, header = ::categoryTitle)

// How tiles take a colour or a picture of the user's own is told with the group of the app's look.
private fun categoryNote(category: SettingsCategory): Int? = R.string.serein_tiles_hint.takeIf { category == SettingsCategory.INTERFACE }

/** The header of each group of features on the SereinGram page. */
private fun categoryTitle(category: SettingsCategory): Int = when (category) {
    SettingsCategory.PRIVACY -> R.string.serein_settings_category_privacy
    SettingsCategory.MESSAGES -> R.string.serein_settings_category_messages
    SettingsCategory.CHATS -> R.string.serein_settings_category_chats
    SettingsCategory.MEDIA -> R.string.serein_settings_category_media
    SettingsCategory.INTERFACE -> R.string.serein_settings_category_interface
    SettingsCategory.MORE -> R.string.serein_settings_category_more
}

private fun aboutSection() = SettingsSection(
    header = R.string.serein_settings_about,
    rows = listOf(
        SettingsRow.Link(
            R.string.serein_settings_version,
            RELEASES_URL,
            summary = R.string.serein_settings_version_info,
            icon = R.drawable.msg_info,
            value = { BuildConfig.VERSION_NAME },
        ),
        SettingsRow.Link(R.string.serein_settings_source, SOURCE_URL, summary = R.string.serein_settings_source_info, icon = R.drawable.msg_link2),
        SettingsRow.Link(R.string.serein_settings_chat, CHAT_URL, summary = R.string.serein_settings_chat_info, icon = R.drawable.msg_discussion),
    ),
    note = R.string.serein_settings_about_note,
)
