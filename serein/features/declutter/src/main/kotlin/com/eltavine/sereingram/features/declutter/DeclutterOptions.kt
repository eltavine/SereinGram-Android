package com.eltavine.sereingram.features.declutter

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.booleanOption

/** Telegram elements the user can hide, ported from NagramX, OctoGram and Swiftgram; each is off by default. */
public object DeclutterOptions {
    public val hidePremiumSection: Option<Boolean> = booleanOption("hide_premium_section")
    public val hideHelpSection: Option<Boolean> = booleanOption("hide_help_section")
    public val hideShareButton: Option<Boolean> = booleanOption("hide_share_button")
    public val hideChannelGiftButton: Option<Boolean> = booleanOption("hide_channel_gift_button")
    public val hideChannelMessageButton: Option<Boolean> = booleanOption("hide_channel_message_button")
    public val hideReactions: Option<Boolean> = booleanOption("hide_reactions")

    public val all: List<Option<*>> = listOf(
        hidePremiumSection,
        hideHelpSection,
        hideShareButton,
        hideChannelGiftButton,
        hideChannelMessageButton,
        hideReactions,
    )
}

/** The share button beside a message stays in Saved Messages, where it leads back to the original chat. */
public fun allowShareButton(hide: Boolean, saved: Boolean): Boolean = saved || !hide

/** Tags on saved messages are reactions too, but they are the user's own way of sorting, so they stay. */
public fun hidesReactions(hide: Boolean, tags: Boolean): Boolean = hide && !tags
