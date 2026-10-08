package com.eltavine.sereingram.ports

/** A message the user marked to come back to, with enough of it to list without Telegram's cache. */
public class Bookmark(
    public val dialogId: Long,
    public val messageId: Int,
    public val messageDate: Int,
    public val senderId: Long,
    public val text: String,
    public val createdAt: Long,
)

public interface BookmarkStore {
    /** Adding a message that is already bookmarked keeps the first bookmark. */
    public fun add(bookmark: Bookmark)

    public fun remove(dialogId: Long, messageId: Int)

    /** Every bookmark, most recently made first. */
    public fun all(): List<Bookmark>

    /** The bookmarks of one chat, newest message first. */
    public fun inChat(dialogId: Long): List<Bookmark>

    /** Erases every bookmark of the account, for good. */
    public fun clearAll()
}
