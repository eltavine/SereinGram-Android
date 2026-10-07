package com.eltavine.sereingram.features.transcription

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TranscriptionTest {
    private fun config(enabled: Boolean = true, baseUrl: String = "https://api.groq.com/openai/v1/", apiKey: String = "key", model: String = "whisper-large-v3-turbo") =
        TranscriptionConfig(enabled, baseUrl, apiKey, model, language = "")

    @Test
    fun audioGoesToTheServicesTranscriptionPath() {
        assertEquals("https://api.groq.com/openai/v1/audio/transcriptions", config().endpoint)
        assertEquals("http://192.168.1.2:8000/v1/audio/transcriptions", config(baseUrl = " http://192.168.1.2:8000/v1 ").endpoint)
        assertNull(config(baseUrl = "api.openai.com").endpoint)
    }

    @Test
    fun aServiceNeedsAKeyAModelAndAnAddress() {
        assertTrue(config().isUsable)
        assertFalse(config(enabled = false).isUsable)
        assertFalse(config(apiKey = " ").isUsable)
        assertFalse(config(model = "").isUsable)
        assertFalse(config(baseUrl = "ftp://example.com").isUsable)
    }

    @Test
    fun voiceAndVideoMessagesAreSentAsWhatTheyAre() {
        assertEquals("audio/ogg", audioMimeType("-1001234_42.ogg"))
        assertEquals("video/mp4", audioMimeType("round.MP4"))
        assertEquals("application/octet-stream", audioMimeType("noextension"))
    }

    @Test
    fun storageKeysNeverChange() {
        assertEquals(
            listOf("transcription_enabled", "transcription_base_url", "transcription_api_key", "transcription_model", "transcription_language"),
            TranscriptionOptions.all.map { it.key },
        )
    }

    @Test
    fun noBackupHoldsTheKeyOrChangesWhereItGoes() {
        assertTrue(TranscriptionOptions.apiKey.secret)
        assertEquals(
            listOf("transcription_base_url", "transcription_api_key"),
            TranscriptionOptions.all.filterNot { it.backedUp }.map { it.key },
        )
    }
}
