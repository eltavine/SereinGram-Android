package com.eltavine.sereingram.features.search

import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.SearchHooks
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import org.telegram.messenger.R

/** Keeps chat list search to the user's own chats, through Telegram's own switch for it. */
object SearchFeature : SereinModule, SettingsContributor {
    override val id: String = "search"

    override val options: List<Option<*>> = SearchOptions.all

    override fun start(context: ModuleContext) {
        val options = context.options
        SearchHooks.globalSearchPolicies.install { !options.get(SearchOptions.hideGlobalResults) }
    }

    override val settingsIcon: Int = R.drawable.msg_search

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_search_title,
        listOf(
            SettingsSection(
                rows = listOf(SettingsRow.Toggle(SearchOptions.hideGlobalResults, R.string.serein_search_hide_global)),
                note = R.string.serein_search_hide_global_note,
            ),
        ),
    )
}
