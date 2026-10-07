package com.eltavine.sereingram.features.bookmarks

import com.eltavine.sereingram.ports.Bookmark

/**
 * Which messages are bookmarked, per account, so that menus can tell on the
 * UI thread. An account counts as loaded once [load] gave it its bookmarks.
 */
public class BookmarkIndex {
    private val accounts = HashMap<Int, HashMap<Long, MutableSet<Int>>>()

    @Synchronized
    public fun load(account: Int, bookmarks: List<Bookmark>) {
        val chats = HashMap<Long, MutableSet<Int>>()
        bookmarks.forEach { chats.getOrPut(it.dialogId, ::HashSet) += it.messageId }
        accounts[account] = chats
    }

    @Synchronized
    public fun isLoaded(account: Int): Boolean = account in accounts

    @Synchronized
    public fun contains(account: Int, dialogId: Long, messageId: Int): Boolean =
        accounts[account]?.get(dialogId)?.contains(messageId) == true

    @Synchronized
    public fun hasAny(account: Int, dialogId: Long): Boolean = accounts[account]?.get(dialogId)?.isNotEmpty() == true

    @Synchronized
    public fun set(account: Int, dialogId: Long, messageId: Int, bookmarked: Boolean) {
        val chats = accounts.getOrPut(account, ::HashMap)
        if (bookmarked) {
            chats.getOrPut(dialogId, ::HashSet) += messageId
        } else {
            chats[dialogId]?.remove(messageId)
        }
    }
}

/** What a bookmark keeps of a message's text: enough to recognise it in a list. */
public fun snippet(text: String, limit: Int = 200): String {
    val line = text.trim().replace(Regex("\\s+"), " ")
    return if (line.length <= limit) line else line.take(limit - 1).trimEnd() + "…"
}
