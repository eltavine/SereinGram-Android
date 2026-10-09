package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers
import java.util.Locale

/**
 * Telegram looks its strings up in packs that its build makes of the app's string
 * files, and loads only its own and Nagram's. The packs named here load after Nagram's,
 * so that their strings are found and take precedence.
 */
public object LocaleHooks {
    public fun interface StringPacks {
        /** The assets of the packs with strings in [locale], later ones winning; a missing asset is skipped. */
        public fun packsFor(locale: Locale): List<String>
    }

    /** Telegram keeps the packs it loaded until the locale changes, so these are installed before it shows a string. */
    public val stringPacks: Handlers<StringPacks> = Handlers()

    @JvmStatic
    public fun packsFor(locale: Locale?): List<String> {
        locale ?: return emptyList()
        return stringPacks.all.flatMap { packs -> Faults.guard("string packs", fallback = emptyList()) { packs.packsFor(locale) } }
    }
}
