package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers
import java.net.InetAddress

/** How the app reaches the network, as far as SereinGram has a say in it. */
public object NetworkHooks {
    public fun interface Resolver {
        /** The addresses of [host], an empty list when it has none, or null to leave it to Telegram and Nagram. */
        public fun resolve(host: String): List<InetAddress>?
    }

    public val resolvers: Handlers<Resolver> = Handlers()

    /** Runs where host names are looked up, off the main thread; a resolver that throws leaves the lookup as it was. */
    @JvmStatic
    public fun resolve(host: String): List<InetAddress>? =
        resolvers.all.firstNotNullOfOrNull { resolver -> Faults.guard("host resolver", fallback = null) { resolver.resolve(host) } }
}
