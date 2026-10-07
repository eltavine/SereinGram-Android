package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/** Items SereinGram adds to the menu Telegram opens on a message. */
public object MessageMenuHooks {
    public interface Entry {
        /** Option id in the menu; Telegram's and Nagram's stay below [FIRST_OPTION]. */
        public val option: Int

        public val icon: Int

        /** [message] is Telegram's object for the message the menu was opened on. */
        public fun isShown(account: Int, message: Any): Boolean

        public fun title(account: Int, message: Any): CharSequence

        /** [host] is the chat screen the menu belongs to. */
        public fun onSelected(account: Int, message: Any, host: Any)
    }

    public const val FIRST_OPTION: Int = 30_000

    public val entries: Handlers<Entry> = Handlers()

    /** Appends the shown entries to Telegram's parallel lists of titles, options and icons. */
    @JvmStatic
    public fun fill(
        account: Int,
        message: Any,
        items: MutableList<CharSequence>,
        options: MutableList<Int>,
        icons: MutableList<Int>,
    ) {
        entries.all.forEach { entry ->
            Faults.guard("message menu entry", fallback = Unit) {
                if (entry.option >= FIRST_OPTION && entry.isShown(account, message)) {
                    val title = entry.title(account, message)
                    items += title
                    options += entry.option
                    icons += entry.icon
                }
            }
        }
    }

    /** Handles [option] when it is one of SereinGram's; false leaves it to Telegram. */
    @JvmStatic
    public fun select(option: Int, account: Int, message: Any, host: Any): Boolean {
        val entry = entries.all.firstOrNull { it.option == option } ?: return false
        Faults.guard("message menu entry", fallback = Unit) { entry.onSelected(account, message, host) }
        return true
    }
}
