package com.eltavine.sereingram.core

/** Sets of Telegram chat ids as a text option stores them: comma separated, in the order they were added. */
public object DialogIds {
    public fun parse(text: String): Set<Long> =
        text.split(',').mapNotNullTo(LinkedHashSet()) { it.trim().toLongOrNull() }

    public fun format(ids: Set<Long>): String = ids.joinToString(",")

    public fun with(text: String, dialogId: Long, included: Boolean): String =
        format(if (included) parse(text) + dialogId else parse(text) - dialogId)
}
