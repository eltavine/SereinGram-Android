package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Handlers

/** Rows that SereinGram adds to Telegram's main settings list, after Nagram's own row. */
public object SettingsHooks {
    public class Entry(
        /** Item id in the settings list; Telegram uses ids below 1000. */
        @JvmField public val id: Int,
        @JvmField public val icon: Int,
        @JvmField public val iconColorTop: Int,
        @JvmField public val iconColorBottom: Int,
        private val title: () -> CharSequence,
        private val subtitle: () -> CharSequence?,
        private val open: (host: Any) -> Unit,
    ) {
        init {
            require(id >= 1000) { "settings entry id $id may clash with Telegram's" }
        }

        public fun title(): CharSequence = title.invoke()

        public fun subtitle(): CharSequence? = subtitle.invoke()

        internal fun open(host: Any) = open.invoke(host)
    }

    public val mainEntries: Handlers<Entry> = Handlers()

    @JvmStatic
    public fun mainSettingsEntries(): List<Entry> = mainEntries.all

    /** Opens the entry with [id] from [host], the settings fragment; false when the id is not SereinGram's. */
    @JvmStatic
    public fun openMainSettingsEntry(id: Int, host: Any): Boolean {
        val entry = mainEntries.all.firstOrNull { it.id == id } ?: return false
        entry.open(host)
        return true
    }
}
