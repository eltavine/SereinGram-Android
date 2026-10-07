package com.eltavine.sereingram.features.playback

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.booleanOption
import com.eltavine.sereingram.core.intOption

/** How voice messages and videos play, after Cherrygram's, NagramX's and exteraGram's options. */
public object PlaybackOptions {
    public val stopAfterVoice: Option<Boolean> = booleanOption("playback_stop_after_voice")

    /** Telegram jumps 10 seconds. */
    public val doubleTapSeekSeconds: Option<Int> = intOption("playback_double_tap_seek_seconds", default = 10)

    public val all: List<Option<*>> = listOf(stopAfterVoice, doubleTapSeekSeconds)
}

/** The jumps offered; a stored value outside them still works, within [seekMillis]'s bounds. */
public val SEEK_CHOICES: List<Int> = listOf(5, 10, 15, 20, 30, 60)

/** A double tap jumps [seconds], kept between one second and five minutes. */
public fun seekMillis(seconds: Int): Long = seconds.coerceIn(1, 300) * 1000L
