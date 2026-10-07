package com.eltavine.sereingram.hooks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackHooksTest {
    @Test
    fun voiceMessagesPlayOnUntilAPolicyStopsThem() {
        assertFalse(PlaybackHooks.stopsAfterEachVoice())
        val installs = listOf(
            PlaybackHooks.voiceQueuePolicies.install { throw IllegalStateException() },
            PlaybackHooks.voiceQueuePolicies.install { false },
        )
        try {
            assertFalse(PlaybackHooks.stopsAfterEachVoice())
            PlaybackHooks.voiceQueuePolicies.install { true }.use {
                assertTrue(PlaybackHooks.stopsAfterEachVoice())
            }
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }

    @Test
    fun aDoubleTapJumpsAsFarAsTheFirstPolicyAsks() {
        assertEquals(PlaybackHooks.TELEGRAM_SEEK_MILLIS, PlaybackHooks.doubleTapSeekMillis())
        val installs = listOf(
            PlaybackHooks.seekPolicies.install { throw IllegalStateException() },
            PlaybackHooks.seekPolicies.install { null },
            PlaybackHooks.seekPolicies.install { -5 },
            PlaybackHooks.seekPolicies.install { 30_000 },
            PlaybackHooks.seekPolicies.install { 5_000 },
        )
        try {
            assertEquals(30_000, PlaybackHooks.doubleTapSeekMillis())
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }
}
