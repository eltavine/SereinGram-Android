package com.eltavine.sereingram.features.dns

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DnsTest {
    @Test
    fun modesComeFromTheirStoredCodes() {
        assertEquals(DnsMode.SYSTEM, DnsMode.of(1))
        assertEquals(DnsMode.CUSTOM, DnsMode.of(2))
        assertEquals(DnsMode.DEFAULT, DnsMode.of(99), "a mode a later release added reads as the default")
    }

    @Test
    fun theOptionsChooseWhereHostNamesAreResolved() {
        assertEquals(Resolution.Default, resolution(0, "https://dns.example/dns-query"))
        assertEquals(Resolution.System, resolution(1, ""))
        assertEquals(Resolution.OverHttps("https://dns.example/dns-query"), resolution(2, " https://dns.example/dns-query "))
        assertEquals(Resolution.Default, resolution(2, "dns.example"), "an unusable server leaves resolving as it is")
    }

    @Test
    fun onlyHttpsAddressesWithAHostAreServers() {
        assertEquals("https://1.1.1.1/dns-query", dohServer("https://1.1.1.1/dns-query"))
        assertEquals("https://[2606:4700:4700::1111]/dns-query", dohServer("https://[2606:4700:4700::1111]/dns-query"))
        assertEquals("HTTPS://dns.example/q", dohServer("HTTPS://dns.example/q"))
        assertNull(dohServer("http://dns.example/dns-query"), "DNS over plain HTTP would be readable on the way")
        assertNull(dohServer("https:///dns-query"))
        assertNull(dohServer("https://user:secret@dns.example/dns-query"))
        assertNull(dohServer("1.1.1.1"))
        assertNull(dohServer("https://exa mple/"))
        assertNull(dohServer(""))
    }

    @Test
    fun storageKeysNeverChange() {
        assertEquals(listOf("dns_mode", "dns_custom_server"), DnsOptions.all.map { it.key })
    }
}
