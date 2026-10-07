package com.eltavine.sereingram.features.reactions

import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.ReactionHooks
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import org.telegram.messenger.R
import org.telegram.ui.Components.Reactions.ReactionsLayoutInBubble

/** Offers the user's chosen reactions first above a message's menu. */
object ReactionsFeature : SereinModule, SettingsContributor {
    override val id: String = "reactions"

    override val options: List<Option<*>> = ReactionOptions.all

    override fun start(context: ModuleContext) {
        val options = context.options
        ReactionHooks.arrangers.install { _, reactions ->
            val pinned = pinnedEmoji(options.get(ReactionOptions.pinned))
            if (pinned.isEmpty()) {
                emptyList()
            } else {
                // Custom emoji reactions have no emoji of their own to pin them by.
                val emoji = reactions.map { reaction -> (reaction as ReactionsLayoutInBubble.VisibleReaction).emojicon.takeIf { reaction.documentId == 0L } }
                pinnedFirst(emoji, pinned)
            }
        }
    }

    override val settingsIcon: Int = R.drawable.msg_reactions

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_reactions_title,
        listOf(
            SettingsSection(
                rows = listOf(SettingsRow.Text(ReactionOptions.pinned, R.string.serein_reactions_pinned, R.string.serein_reactions_pinned_hint)),
                note = R.string.serein_reactions_pinned_note,
            ),
        ),
    )
}
