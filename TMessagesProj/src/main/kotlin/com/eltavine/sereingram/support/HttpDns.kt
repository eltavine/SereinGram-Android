package com.eltavine.sereingram.support

import com.eltavine.sereingram.hooks.NetworkHooks
import okhttp3.Dns
import java.net.InetAddress

/** What SereinGram's HTTP clients resolve host names with: the DNS the user chose, else the system's. */
object HttpDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> = NetworkHooks.resolve(hostname) ?: Dns.SYSTEM.lookup(hostname)
}
