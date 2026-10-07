package com.eltavine.sereingram.features.links

import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.LinkHooks
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import org.telegram.messenger.R

/** Asks before any link opens, after NagramX's "confirm all links". */
object LinksFeature : SereinModule, SettingsContributor {
    override val id: String = "links"

    override val options: List<Option<*>> = LinkOptions.all

    override fun start(context: ModuleContext) {
        val options = context.options
        LinkHooks.confirmPolicies.install { options.get(LinkOptions.confirmAll) }
    }

    override val settingsIcon: Int = R.drawable.msg_link

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_links_title,
        listOf(
            SettingsSection(
                header = R.string.serein_links_opening,
                rows = listOf(SettingsRow.Toggle(LinkOptions.confirmAll, R.string.serein_links_confirm_all)),
                note = R.string.serein_links_confirm_all_note,
            ),
        ),
    )
}
