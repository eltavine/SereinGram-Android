package com.eltavine.sereingram.features.ghost

import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.OptionScope
import com.eltavine.sereingram.core.booleanOption
import com.eltavine.sereingram.core.textOption

/** What ghost mode keeps back that a chat can be let in on. */
public enum class GhostAction { READ, TYPING }

/** Chats that ghost mode lets in on reads or typing, per account, after NagramX's exclusions. */
public object GhostOptions {
    public val readExceptions: Option<String> = textOption("ghost_read_exceptions", scope = OptionScope.ACCOUNT)
    public val typingExceptions: Option<String> = textOption("ghost_typing_exceptions", scope = OptionScope.ACCOUNT)

    /** A ghost beside the chat list title while ghost mode is on, after NagramX's indicator. */
    public val statusIndicator: Option<Boolean> = booleanOption("ghost_status_indicator", default = true)

    public val all: List<Option<*>> = listOf(readExceptions, typingExceptions, statusIndicator)

    public fun exceptions(action: GhostAction): Option<String> = when (action) {
        GhostAction.READ -> readExceptions
        GhostAction.TYPING -> typingExceptions
    }
}

/** Chat ids as an option stores them: comma separated, in the order they were added. */
public object DialogIds {
    public fun parse(text: String): Set<Long> =
        text.split(',').mapNotNullTo(LinkedHashSet()) { it.trim().toLongOrNull() }

    public fun format(ids: Set<Long>): String = ids.joinToString(",")

    public fun with(text: String, dialogId: Long, included: Boolean): String =
        format(if (included) parse(text) + dialogId else parse(text) - dialogId)
}

/** Lets the next read of a chat past ghost mode, if it comes within [ttlMillis]. */
public class ReadPasses(private val ttlMillis: Long = 15_000, private val clock: () -> Long) {
    private val passes = HashMap<Pair<Int, Long>, Long>()

    @Synchronized
    public fun grant(account: Int, dialogId: Long) {
        passes[account to dialogId] = clock() + ttlMillis
    }

    @Synchronized
    public fun use(account: Int, dialogId: Long): Boolean {
        val until = passes.remove(account to dialogId) ?: return false
        return clock() <= until
    }
}
