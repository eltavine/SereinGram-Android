package com.eltavine.sereingram.features.transcription

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.booleanOption
import com.eltavine.sereingram.core.textOption
import com.google.common.net.InetAddresses
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.URI

/**
 * A speech-to-text service that speaks OpenAI's transcription API, as OpenAI,
 * Groq and many self-hosted Whisper servers do, after NagramX, Cherrygram and
 * OctoGram and the requests for it in exteraGram and Swiftgram.
 */
public object TranscriptionOptions {
    public val enabled: Option<Boolean> = booleanOption("transcription_enabled")
    /** The key goes to this address, so no backup may change where it goes. */
    public val baseUrl: Option<String> = textOption("transcription_base_url", default = "https://api.openai.com/v1", backedUp = false)
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
    /**
     * Where audio goes, or null when [baseUrl] is no web address. Over http the key and the audio
     * travel unencrypted, so http is only for servers in the user's own network.
     */
    public val endpoint: String?
        get() {
            val address = baseUrl.trim().trimEnd('/')
            val uri = runCatching { URI(address) }.getOrNull() ?: return null
            val usable = when (uri.scheme?.lowercase()) {
                "https" -> uri.rawAuthority != null
                "http" -> uri.host?.let(::isLocal) == true
                else -> false
            }
            return "$address/audio/transcriptions".takeIf { usable }
        }

    public val isUsable: Boolean
        get() = enabled && apiKey.isNotBlank() && model.isNotBlank() && endpoint != null
}

/** Endings of names that only resolve in the user's own network, after RFC 6761, 6762 and 8375. */
private val LOCAL_NAMES = listOf(".localhost", ".local", ".lan", ".home.arpa", ".internal")

/**
 * Whether [host], as a URL holds it, is a machine in the user's own network: a local name, or an
 * address the internet does not route.
 */
internal fun isLocal(host: String): Boolean {
    val name = host.lowercase().removeSuffix(".")
    if (name == "localhost" || LOCAL_NAMES.any(name::endsWith)) return true
    if (!InetAddresses.isUriInetAddress(name)) return false
    val address = InetAddresses.forUriString(name)
    val bytes = address.address
    val uniqueLocal = address is Inet6Address && bytes[0].toInt() and 0xfe == 0xfc
    // Shared by carriers' NAT and by VPNs such as Tailscale.
    val sharedSpace = address is Inet4Address && bytes[0].toInt() == 100 && bytes[1].toInt() and 0xc0 == 0x40
    return address.isLoopbackAddress || address.isSiteLocalAddress || address.isLinkLocalAddress || uniqueLocal || sharedSpace
}

/** Whether [text] is an ISO 639-1 code, the two letters such as en that services take for the language. */
public fun isLanguageCode(text: String): Boolean = text.length == 2 && text.all { it in 'a'..'z' || it in 'A'..'Z' }

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
