package com.eltavine.sereingram.hooks

import kotlin.test.Test
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
}
