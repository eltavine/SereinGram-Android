package com.eltavine.sereingram.features.transcription

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
    fun onlyServersInTheUsersOwnNetworkMayGoWithoutHttps() {
        val local = listOf(
            "http://localhost:8000/v1",
            "http://127.0.0.1:8000",
            "http://10.0.0.5",
            "http://172.20.1.1",
            "http://169.254.10.1",
            "http://100.101.102.103:8000",
            "http://[::1]:8000",
            "http://[fd12:3456::1]",
            "http://[fe80::1]",
            "HTTP://Whisper.Local/v1",
            "http://nas.lan:9000",
            "http://whisper.home.arpa",
        )
        local.forEach { assertNotNull(config(baseUrl = it).endpoint, it) }
        val public = listOf(
            "http://api.openai.com/v1",
            "http://8.8.8.8",
            "http://100.128.0.1",
            "http://[2001:4860::8888]",
            "http://localhost.example.com",
            "http://192.168.1.2@example.com",
            "http://2130706433",
            "http://",
        )
        public.forEach { assertNull(config(baseUrl = it).endpoint, it) }
        assertEquals("https://example.com/v1/audio/transcriptions", config(baseUrl = "https://example.com/v1").endpoint)
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

    @Test
    fun languagesAreTwoLetterCodes() {
        listOf("en", "zh", "DE").forEach { assertTrue(isLanguageCode(it), it) }
        listOf("", "e", "eng", "zh-CN", "z1", "中文").forEach { assertFalse(isLanguageCode(it), it) }
    }
}
