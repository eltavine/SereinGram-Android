package com.eltavine.sereingram.features.transcription

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TranscriptionClientTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val config = TranscriptionConfig(
        enabled = true,
        baseUrl = "https://api.groq.com/openai/v1",
        apiKey = " gsk-123 ",
        model = "whisper-large-v3-turbo",
        language = "zh",
    )

    @Test
    fun audioGoesAsAFormWithTheKeyAndTheTextComesBack() = runBlocking {
        var form = ""
        val engine = MockEngine { request ->
            assertEquals("https://api.groq.com/openai/v1/audio/transcriptions", request.url.toString())
            assertEquals("Bearer gsk-123", request.headers["Authorization"])
            form = String(request.body.toByteArray())
            respond("  你好，世界\n")
        }
        val voice = folder.newFile("-100_42.ogg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        assertEquals("你好，世界", TranscriptionClient(HttpClient(engine)).transcribe(config, voice))
        listOf("name=model", "whisper-large-v3-turbo", "name=response_format", "text", "name=language", "zh", "filename=\"-100_42.ogg\"", "audio/ogg")
            .forEach { assertTrue("form lacks $it", form.contains(it)) }
    }

    @Test
    fun aRefusalIsAnErrorWithWhatTheServiceSaid() {
        val engine = MockEngine { respond("invalid api key", HttpStatusCode.Unauthorized) }
        val voice = folder.newFile("voice.ogg")
        val error = assertThrows(IllegalStateException::class.java) {
            runBlocking { TranscriptionClient(HttpClient(engine)).transcribe(config, voice) }
        }
        assertTrue(error.message!!.contains("401") && error.message!!.contains("invalid api key"))
    }
}
