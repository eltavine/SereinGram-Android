package com.eltavine.sereingram.hooks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TranscriptionHooksTest {
    private class Provider : TranscriptionHooks.Provider {
        val taps = mutableListOf<String>()

        override fun offers(account: Int, message: Any): Boolean = message == "voice"

        override fun tap(account: Int, message: Any, open: Boolean, button: Any): Boolean {
            if (open) return false
            taps += "$message"
            return true
        }
    }

    @Test
    fun aProviderOffersAndTakesTapsOnTheMessagesItCanTranscribe() {
        val provider = Provider()
        val install = TranscriptionHooks.providers.install(provider)
        try {
            assertTrue(TranscriptionHooks.offersTranscription(0, "voice"))
            assertFalse(TranscriptionHooks.offersTranscription(0, "photo"))
            assertFalse(TranscriptionHooks.offersTranscription(0, null))
            assertTrue(TranscriptionHooks.handlesTap(0, "voice", open = false, button = "button"))
            assertFalse(TranscriptionHooks.handlesTap(0, "voice", open = true, button = "button"))
            assertFalse(TranscriptionHooks.handlesTap(0, null, open = false, button = "button"))
            assertEquals(listOf("voice"), provider.taps)
        } finally {
            install.close()
        }
    }
}
