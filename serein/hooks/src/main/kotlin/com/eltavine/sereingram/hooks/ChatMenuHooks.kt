package com.eltavine.sereingram.hooks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.Handlers

/** Items SereinGram adds to the menu behind the three dots of a chat. */
public object ChatMenuHooks {
    public interface Entry {
        /** Item id in the menu; Telegram's and Nagram's stay below [FIRST_ID]. */
        public val id: Int

        public val icon: Int

        public fun isShown(account: Int, dialogId: Long): Boolean

        public fun title(account: Int, dialogId: Long): CharSequence

        /** [chat] is the chat screen the menu belongs to. */
        public fun onSelected(account: Int, dialogId: Long, chat: Any)
    }

    /** Telegram's menu, as far as entries need it. */
    public fun interface Menu {
        public fun add(id: Int, icon: Int, title: CharSequence)
    }

    public const val FIRST_ID: Int = 40_000

    /** One entry per id, so a selected item always reaches the entry that showed it. */
    public val entries: Handlers<Entry> = Handlers { it.id }

    @JvmStatic
    public fun fill(account: Int, dialogId: Long, menu: Menu) {
        entries.all.forEach { entry ->
            Faults.guard("chat menu entry", fallback = Unit) {
                if (entry.id >= FIRST_ID && entry.isShown(account, dialogId)) {
                    val title = entry.title(account, dialogId)
                    menu.add(entry.id, entry.icon, title)
                }
            }
        }
    }

    /** Handles [id] when it is one of SereinGram's items; false leaves it to Telegram. */
    @JvmStatic
    public fun select(id: Int, account: Int, dialogId: Long, chat: Any): Boolean {
        if (id < FIRST_ID) {
            return false
        }
        val entry = entries.all.firstOrNull { it.id == id } ?: return false
        Faults.guard("chat menu entry", fallback = Unit) { entry.onSelected(account, dialogId, chat) }
        return true
    }
}
