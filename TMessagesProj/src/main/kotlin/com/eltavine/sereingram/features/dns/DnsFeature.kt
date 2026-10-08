package com.eltavine.sereingram.features.dns

import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.NetworkHooks
import com.eltavine.sereingram.settings.SettingsCategory
import com.eltavine.sereingram.settings.SettingsCondition
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import com.eltavine.sereingram.settings.SettingsTint
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

    override val settingsCategory: SettingsCategory = SettingsCategory.PRIVACY

    override val settingsTint: SettingsTint = SettingsTint.CYAN

    override val settingsOrder: Int = 3

    private val customMode = SettingsCondition.isSetTo(DnsOptions.mode, DnsMode.CUSTOM.code)

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_dns_title,
        summary = R.string.serein_dns_summary,
        status = { state ->
            when (resolution(state[DnsOptions.mode], state[DnsOptions.customServer])) {
                Resolution.Default -> getString(R.string.serein_settings_not_set_up).takeIf { DnsMode.of(state[DnsOptions.mode]) == DnsMode.CUSTOM }
                Resolution.System -> getString(R.string.serein_dns_status_system)
                is Resolution.OverHttps -> getString(R.string.serein_dns_status_custom)
            }
        },
        sections = listOf(
            SettingsSection(
                rows = listOf(
                    SettingsRow.Choice(
                        DnsOptions.mode,
                        R.string.serein_dns_mode,
                        choices = DnsMode.entries.map { it.code },
                        label = { code -> getString(label(DnsMode.of(code))) },
                        describe = { code -> getString(description(DnsMode.of(code))) },
                        style = SettingsRow.ChoiceStyle.INLINE,
                    ),
                    SettingsRow.Text(
                        DnsOptions.customServer,
                        R.string.serein_dns_custom_server,
                        placeholder = R.string.serein_dns_custom_server_none,
                        hint = R.string.serein_dns_custom_server_hint,
                        summary = R.string.serein_dns_custom_server_info,
                        kind = SettingsRow.TextKind.URL,
                        check = { text -> R.string.serein_dns_custom_server_invalid.takeIf { dohServer(text) == null } },
                        requiredWhen = customMode,
                        shownWhen = customMode,
                    ),
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

    private fun description(mode: DnsMode): Int = when (mode) {
        DnsMode.DEFAULT -> R.string.serein_dns_mode_default_info
        DnsMode.SYSTEM -> R.string.serein_dns_mode_system_info
        DnsMode.CUSTOM -> R.string.serein_dns_mode_custom_info
    }

    private const val CACHE_BYTES = 1L shl 20
}
