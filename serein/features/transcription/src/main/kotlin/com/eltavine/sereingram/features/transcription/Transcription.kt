package com.eltavine.sereingram.features.transcription

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.booleanOption
import com.eltavine.sereingram.core.textOption

/**
 * A speech-to-text service that speaks OpenAI's transcription API, as OpenAI,
 * Groq and many self-hosted Whisper servers do, after NagramX, Cherrygram and
 * OctoGram and the requests for it in exteraGram and Swiftgram.
 */
public object TranscriptionOptions {
    public val enabled: Option<Boolean> = booleanOption("transcription_enabled")
    public val baseUrl: Option<String> = textOption("transcription_base_url", default = "https://api.openai.com/v1")
    public val apiKey: Option<String> = textOption("transcription_api_key", secret = true)
    public val model: Option<String> = textOption("transcription_model", default = "whisper-1")

    /** An ISO 639-1 code that helps the service; empty lets it find out. */
    public val language: Option<String> = textOption("transcription_language")

    public val all: List<Option<*>> = listOf(enabled, baseUrl, apiKey, model, language)
}

public class TranscriptionConfig(
    public val enabled: Boolean,
    public val baseUrl: String,
    public val apiKey: String,
    public val model: String,
    public val language: String,
) {
    /** Where audio goes, or null when [baseUrl] is not a web address. */
    public val endpoint: String?
        get() = baseUrl.trim().trimEnd('/').takeIf { it.startsWith("https://") || it.startsWith("http://") }
            ?.let { "$it/audio/transcriptions" }

    public val isUsable: Boolean
        get() = enabled && apiKey.isNotBlank() && model.isNotBlank() && endpoint != null
}

/** OpenAI takes files of up to 25 MB. */
public const val MAX_UPLOAD_BYTES: Long = 25L * 1024 * 1024

/** The type a service needs to read [fileName], from its extension. */
public fun audioMimeType(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
    "ogg", "oga", "opus" -> "audio/ogg"
    "mp4" -> "video/mp4"
    "m4a" -> "audio/mp4"
    "mp3" -> "audio/mpeg"
    "wav" -> "audio/wav"
    "webm" -> "audio/webm"
    else -> "application/octet-stream"
}
