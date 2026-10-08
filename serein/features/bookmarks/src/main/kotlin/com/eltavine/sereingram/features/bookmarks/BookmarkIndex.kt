package com.eltavine.sereingram.features.bookmarks

import com.eltavine.sereingram.ports.Bookmark

/**
 * Which messages are bookmarked, per account, so that menus can tell on the
 * UI thread. An account counts as loaded once [load] gave it its bookmarks;
 * changes made before then are kept apart and win over what is loaded, since
 * the load may have read the store before they reached it.
 */
public class BookmarkIndex {
    private val loaded = HashMap<Int, HashMap<Long, MutableSet<Int>>>()
    private val pending = HashMap<Int, LinkedHashMap<Pair<Long, Int>, Boolean>>()

    @Synchronized
    public fun load(account: Int, bookmarks: List<Bookmark>) {
        val chats = HashMap<Long, MutableSet<Int>>()
        bookmarks.forEach { chats.getOrPut(it.dialogId, ::HashSet) += it.messageId }
        pending.remove(account)?.forEach { (message, bookmarked) -> chats.mark(message.first, message.second, bookmarked) }
        loaded[account] = chats
    }

    @Synchronized
    public fun isLoaded(account: Int): Boolean = account in loaded

    @Synchronized
    public fun contains(account: Int, dialogId: Long, messageId: Int): Boolean {
        val chats = loaded[account] ?: return pending[account]?.get(dialogId to messageId) == true
        return chats[dialogId]?.contains(messageId) == true
    }

    @Synchronized
    public fun hasAny(account: Int, dialogId: Long): Boolean {
        val chats = loaded[account] ?: return pending[account]?.any { (message, bookmarked) -> message.first == dialogId && bookmarked } == true
        return chats[dialogId]?.isNotEmpty() == true
    }

    @Synchronized
    public fun set(account: Int, dialogId: Long, messageId: Int, bookmarked: Boolean) {
        val chats = loaded[account]
        if (chats == null) {
            pending.getOrPut(account, ::LinkedHashMap)[dialogId to messageId] = bookmarked
        } else {
            chats.mark(dialogId, messageId, bookmarked)
        }
    }

    private fun HashMap<Long, MutableSet<Int>>.mark(dialogId: Long, messageId: Int, bookmarked: Boolean) {
        if (bookmarked) {
            getOrPut(dialogId, ::HashSet) += messageId
        } else {
            get(dialogId)?.remove(messageId)
        }
    }
}

/** What a bookmark keeps of a message's text: enough to recognise it in a list. */
public fun snippet(text: String, limit: Int = 200): String {
    val line = text.trim().replace(Regex("\\s+"), " ")
    return if (line.length <= limit) line else line.take(limit - 1).trimEnd() + "…"
}
