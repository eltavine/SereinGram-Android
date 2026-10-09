package com.eltavine.sereingram.features.search

import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.SearchHooks
import com.eltavine.sereingram.settings.SettingsCategory
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import com.eltavine.sereingram.settings.SettingsTint
import org.telegram.messenger.R

/** Keeps chat list search to the user's own chats, through Telegram's own switch for it. */
object SearchFeature : SereinModule, SettingsContributor {
    override val id: String = "search"

    override val options: List<Option<*>> = SearchOptions.all

    override fun start(context: ModuleContext) {
        val options = context.options
        SearchHooks.globalSearchPolicies.install { !options.get(SearchOptions.hideGlobalResults) }
        SearchHooks.appsTabPolicies.install { !options.get(SearchOptions.hideAppsTab) }
    }

    override val settingsIcon: Int = R.drawable.msg_search

    override val settingsCategory: SettingsCategory = SettingsCategory.INTERFACE

    override val settingsTint: SettingsTint = SettingsTint.SLATE

    override val settingsOrder: Int = 1

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_search_title,
        summary = R.string.serein_search_summary,
        sections = listOf(
            SettingsSection(
                header = R.string.serein_search_chat_list,
                rows = listOf(
                    SettingsRow.Toggle(
                        SearchOptions.hideGlobalResults,
                        R.string.serein_search_hide_global,
                        summary = R.string.serein_search_hide_global_info,
                        restarts = true,
                    ),
                    SettingsRow.Toggle(SearchOptions.hideAppsTab, R.string.serein_search_hide_apps, summary = R.string.serein_search_hide_apps_info, restarts = true),
                ),
                note = R.string.serein_search_note,
            ),
        ),
    )
}
