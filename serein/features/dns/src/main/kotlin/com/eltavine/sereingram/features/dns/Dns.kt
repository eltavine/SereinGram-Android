package com.eltavine.sereingram.features.dns

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.intOption
import com.eltavine.sereingram.core.textOption
import java.net.URI

/** How SereinGram resolves host names. The codes are stored and never reused. */
public enum class DnsMode(public val code: Int) {
    /** As Telegram and Nagram do by themselves: their DNS over HTTPS, then the system's. */
    DEFAULT(0),

    /** The system's resolver, which follows Android's private DNS setting. */
    SYSTEM(1),

    /** A DNS-over-HTTPS server of the user's choice, as RFC 8484 describes. */
    CUSTOM(2),
    ;

    public companion object {
        /** A code a later release added reads as the default. */
        public fun of(code: Int): DnsMode = entries.firstOrNull { it.code == code } ?: DEFAULT
    }
}

/** DNS settings after NagramX's custom DoH and Nagram's system DNS switch, for all of the app's lookups. */
public object DnsOptions {
    public val mode: Option<Int> = intOption("dns_mode", default = DnsMode.DEFAULT.code)
    public val customServer: Option<String> = textOption("dns_custom_server")

    public val all: List<Option<*>> = listOf(mode, customServer)
}

/** Where host names are resolved. */
public sealed interface Resolution {
    /** Telegram and Nagram resolve them as they do by themselves. */
    public data object Default : Resolution

    public data object System : Resolution

    public data class OverHttps(val url: String) : Resolution
}

/** What the options choose; a custom server that is no usable address leaves resolving as it is by default. */
public fun resolution(mode: Int, customServer: String): Resolution = when (DnsMode.of(mode)) {
    DnsMode.DEFAULT -> Resolution.Default
    DnsMode.SYSTEM -> Resolution.System
    DnsMode.CUSTOM -> dohServer(customServer)?.let(Resolution::OverHttps) ?: Resolution.Default
}

/** [text] as the address of a DNS-over-HTTPS server: an https URL with a host and no user, else null. */
public fun dohServer(text: String): String? {
    val address = text.trim()
    val uri = runCatching { URI(address) }.getOrNull() ?: return null
    val usable = uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank() && uri.rawUserInfo == null
    return address.takeIf { usable }
}
