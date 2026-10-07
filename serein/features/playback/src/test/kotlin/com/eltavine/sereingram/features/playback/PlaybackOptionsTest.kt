package com.eltavine.sereingram.features.playback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaybackOptionsTest {
    @Test
    fun storageKeysNeverChange() {
        assertEquals(listOf("playback_stop_after_voice", "playback_double_tap_seek_seconds"), PlaybackOptions.all.map { it.key })
        assertEquals(10, PlaybackOptions.doubleTapSeekSeconds.default)
        assertTrue(PlaybackOptions.doubleTapSeekSeconds.default in SEEK_CHOICES)
    }

    @Test
    fun jumpsStayWithinSensibleBounds() {
        assertEquals(15_000, seekMillis(15))
        assertEquals(1_000, seekMillis(0))
        assertEquals(300_000, seekMillis(3_600))
    }
}
