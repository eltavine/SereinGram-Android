package com.eltavine.sereingram.settings

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.core.textOption

/**
 * The colours the user chose for tiles of the SereinGram page, by the [SettingsRow.Subpage.key]
 * of each tile's row, kept together in one option of the device. A tint a later release no longer
 * has, or a key no row has, is skipped as it is read and dropped when the colours are next written.
 */
public object TileTints : SereinModule {
    override val id: String = "settings_tiles"

    public val tints: Option<String> = textOption("settings_tile_tints")

    override val options: List<Option<*>> = listOf(tints)

    public fun decode(text: String): Map<String, SettingsTint> = text.split(ENTRIES).mapNotNull { entry ->
        val (key, name) = entry.split(PAIR, limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
        SettingsTint.entries.firstOrNull { it.name == name }?.let { key to it }
    }.toMap()

    public fun encode(tints: Map<String, SettingsTint>): String =
        tints.entries.sortedBy { it.key }.joinToString(ENTRIES) { (key, tint) -> "$key$PAIR${tint.name}" }

    /** [text] with [key] given [tint], or no colour of its own when null. */
    public fun with(text: String, key: String, tint: SettingsTint?): String {
        val chosen = decode(text).toMutableMap()
        if (tint == null) chosen.remove(key) else chosen[key] = tint
        return encode(chosen)
    }

    private const val ENTRIES = ","
    private const val PAIR = "="
}
