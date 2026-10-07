package com.eltavine.sereingram.features.playback

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.booleanOption

/** How voice and video messages play, after Cherrygram's and NagramX's options; off by default. */
public object PlaybackOptions {
    public val stopAfterVoice: Option<Boolean> = booleanOption("playback_stop_after_voice")

    public val all: List<Option<*>> = listOf(stopAfterVoice)
}
