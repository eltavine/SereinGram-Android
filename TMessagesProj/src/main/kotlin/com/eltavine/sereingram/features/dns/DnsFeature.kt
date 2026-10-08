package com.eltavine.sereingram.features.dns

import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.NetworkHooks
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import okhttp3.Cache
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.FileLog
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Resolves the app's host names with the system's DNS or a DNS-over-HTTPS server
 * the user chooses, after NagramX's custom DoH: proxies and Telegram's other host
 * names, and the web services that Nagram and SereinGram call.
 */
object DnsFeature : SereinModule, SettingsContributor {
    override val id: String = "dns"

    override val options: List<Option<*>> = DnsOptions.all

    override fun start(context: ModuleContext) {
        val resolver = Resolver(context.options, File(ApplicationLoader.applicationContext.cacheDir, "serein_dns"))
        NetworkHooks.resolvers.install(resolver::resolve)
    }

    private class Resolver(options: Options, private val cache: File) {
        private val mode = options.cached(DnsOptions.mode)
        private val customServer = options.cached(DnsOptions.customServer)

        // Answers carry the lifetime their server gives them, which the HTTP cache keeps to.
        private val bootstrap by lazy { OkHttpClient.Builder().cache(Cache(cache, CACHE_BYTES)).build() }

        @Volatile
        private var overHttps: Pair<String, DnsOverHttps>? = null

        fun resolve(host: String): List<InetAddress>? = when (val chosen = resolution(mode.value, customServer.value)) {
            Resolution.Default -> null
            Resolution.System -> system(host)
            is Resolution.OverHttps -> try {
                client(chosen.url).lookup(host)
            } catch (error: IOException) {
                // As Nagram does when its servers fail: the system still answers.
                FileLog.e("SereinGram: ${chosen.url} could not resolve $host", error)
                system(host)
            }
        }

        private fun system(host: String): List<InetAddress> = try {
            Dns.SYSTEM.lookup(host)
        } catch (_: UnknownHostException) {
            emptyList()
        }

        private fun client(url: String): DnsOverHttps {
            overHttps?.takeIf { it.first == url }?.let { return it.second }
            // A server of the user's own may well know names on their network, such as a proxy's.
            val built = DnsOverHttps.Builder().client(bootstrap).url(url.toHttpUrl()).includeIPv6(true).resolvePrivateAddresses(true).build()
            overHttps = url to built
            return built
        }
    }

    override val settingsIcon: Int = R.drawable.msg2_language

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_dns_title,
        listOf(
            SettingsSection(
                rows = listOf(
                    SettingsRow.Choice(DnsOptions.mode, R.string.serein_dns_mode, DnsMode.entries.map { it.code }) { code ->
                        getString(label(DnsMode.of(code)))
                    },
                    SettingsRow.Text(DnsOptions.customServer, R.string.serein_dns_custom_server, R.string.serein_dns_custom_server_hint),
                ),
                note = R.string.serein_dns_note,
            ),
        ),
    )

    private fun label(mode: DnsMode): Int = when (mode) {
        DnsMode.DEFAULT -> R.string.serein_dns_mode_default
        DnsMode.SYSTEM -> R.string.serein_dns_mode_system
        DnsMode.CUSTOM -> R.string.serein_dns_mode_custom
    }

    private const val CACHE_BYTES = 1L shl 20
}
