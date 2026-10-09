package com.eltavine.sereingram.features.sending

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.booleanOption

/** Whether a tap in the GIF panel sends at once, after OctoGram's prompts; off by default. */
public object SendingOptions {
    public val askBeforeGif: Option<Boolean> = booleanOption("sending_ask_before_gif")

    public val all: List<Option<*>> = listOf(askBeforeGif)
}
