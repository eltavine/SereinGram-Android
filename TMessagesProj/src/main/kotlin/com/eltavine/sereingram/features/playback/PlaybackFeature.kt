package com.eltavine.sereingram.features.playback

import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.PlaybackHooks
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import org.telegram.messenger.R

/** Stops at the end of a voice or video message instead of playing the chat's next one. */
object PlaybackFeature : SereinModule, SettingsContributor {
    override val id: String = "playback"

    override val options: List<Option<*>> = PlaybackOptions.all

    override fun start(context: ModuleContext) {
        val options = context.options
        PlaybackHooks.voiceQueuePolicies.install { options.get(PlaybackOptions.stopAfterVoice) }
    }

    override val settingsIcon: Int = R.drawable.msg_voice_headphones

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_playback_title,
        listOf(
            SettingsSection(
                rows = listOf(SettingsRow.Toggle(PlaybackOptions.stopAfterVoice, R.string.serein_playback_stop_after_voice)),
                note = R.string.serein_playback_stop_after_voice_note,
            ),
        ),
    )
}
