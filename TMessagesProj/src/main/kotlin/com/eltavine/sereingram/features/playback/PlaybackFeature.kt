package com.eltavine.sereingram.features.playback

import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.PlaybackHooks
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R

/** Stops after each voice message and sets how far a double tap jumps in a video. */
object PlaybackFeature : SereinModule, SettingsContributor {
    override val id: String = "playback"

    override val options: List<Option<*>> = PlaybackOptions.all

    override fun start(context: ModuleContext) {
        val options = context.options
        PlaybackHooks.voiceQueuePolicies.install { options.get(PlaybackOptions.stopAfterVoice) }
        PlaybackHooks.seekPolicies.install { seekMillis(options.get(PlaybackOptions.doubleTapSeekSeconds)) }
    }

    override val settingsIcon: Int = R.drawable.msg_voice_headphones

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_playback_title,
        listOf(
            SettingsSection(
                rows = listOf(SettingsRow.Toggle(PlaybackOptions.stopAfterVoice, R.string.serein_playback_stop_after_voice)),
                note = R.string.serein_playback_stop_after_voice_note,
            ),
            SettingsSection(
                header = R.string.serein_playback_videos,
                rows = listOf(
                    SettingsRow.Choice(PlaybackOptions.doubleTapSeekSeconds, R.string.serein_playback_double_tap_seek, SEEK_CHOICES) { seconds ->
                        LocaleController.formatPluralString("Seconds", seconds)
                    },
                ),
                note = R.string.serein_playback_double_tap_seek_note,
            ),
        ),
    )
}
