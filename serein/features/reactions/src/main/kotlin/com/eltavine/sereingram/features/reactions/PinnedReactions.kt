package com.eltavine.sereingram.features.reactions

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.textOption
import java.text.BreakIterator

/** Reactions offered first above a message's menu, after NagramX's and OctoGram's pinned reactions. */
public object ReactionOptions {
    /** Emoji in the order to offer them, written with or without spaces or commas between. */
    public val pinned: Option<String> = textOption("reactions_pinned")

    public val all: List<Option<*>> = listOf(pinned)
}

/** The emoji of [text], each once, in order; anything that is not an emoji is left out. */
public fun pinnedEmoji(text: String): List<String> {
    val characters = BreakIterator.getCharacterInstance().apply { setText(text) }
    val emoji = ArrayList<String>()
    var start = characters.first()
    var end = characters.next()
    while (end != BreakIterator.DONE) {
        val cluster = text.substring(start, end)
        if (isEmoji(cluster) && cluster !in emoji) {
            emoji += cluster
        }
        start = end
        end = characters.next()
    }
    return emoji
}

/**
 * The positions of [offered] to show first: those whose emoji are [pinned], in the pinned
 * order. Reactions the chat does not offer are not added, and null entries are custom ones.
 */
public fun pinnedFirst(offered: List<String?>, pinned: List<String>): List<Int> =
    pinned.mapNotNull { emoji -> offered.indexOfFirst { it != null && sameEmoji(it, emoji) }.takeIf { it >= 0 } }

// Telegram writes some reactions with the emoji variation selector and some without.
private fun sameEmoji(a: String, b: String) = a.withoutVariation() == b.withoutVariation()

private fun String.withoutVariation() = replace("\uFE0F", "")

// Emoji are symbols, or characters such as ‼ made emoji by what follows them in their cluster.
private fun isEmoji(cluster: String): Boolean {
    val first = cluster.codePointAt(0)
    if (Character.isLetterOrDigit(first) || Character.isWhitespace(first)) {
        return false
    }
    return Character.getType(first) == Character.OTHER_SYMBOL.toInt() || cluster.length > Character.charCount(first)
}
