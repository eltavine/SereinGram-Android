package com.eltavine.sereingram.features.transcription

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import java.io.File

/** Sends audio to a service that speaks OpenAI's transcription API and returns its text. */
internal class TranscriptionClient(private val http: HttpClient) {
    suspend fun transcribe(config: TranscriptionConfig, file: File): String {
        val endpoint = requireNotNull(config.endpoint) { "no service address" }
        val response = http.submitFormWithBinaryData(
            url = endpoint,
            formData = formData {
                append("model", config.model.trim())
                append("response_format", "text")
                if (config.language.isNotBlank()) {
                    append("language", config.language.trim())
                }
                val headers = Headers.build {
                    append(HttpHeaders.ContentType, audioMimeType(file.name))
                    append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                }
                append("file", file.readBytes(), headers)
            },
        ) {
            header(HttpHeaders.Authorization, "Bearer ${config.apiKey.trim()}")
        }
        val body = response.bodyAsText()
        check(response.status.isSuccess()) { "${response.status.value} ${body.take(200)}" }
        return body.trim()
    }
}
