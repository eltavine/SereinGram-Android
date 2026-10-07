package com.eltavine.sereingram.adapters.room

import android.content.Context
import androidx.room.Room
import com.eltavine.sereingram.ports.Bookmark
import com.eltavine.sereingram.ports.BookmarkStore

/** Bookmarks in a Room database of their own, `serein_bookmarks_<account>.db`, apart from history. */
public class RoomBookmarkStore internal constructor(private val database: BookmarkDatabase) : BookmarkStore {
    public constructor(context: Context, account: Int) : this(
        Room.databaseBuilder(context.applicationContext, BookmarkDatabase::class.java, "serein_bookmarks_$account.db").build(),
    )

    private val dao = database.bookmarks()

    override fun add(bookmark: Bookmark) {
        dao.insert(
            BookmarkEntity(
                id = 0,
                dialogId = bookmark.dialogId,
                messageId = bookmark.messageId,
                messageDate = bookmark.messageDate,
                senderId = bookmark.senderId,
                text = bookmark.text,
                createdAt = bookmark.createdAt,
            ),
        )
    }

    override fun remove(dialogId: Long, messageId: Int) {
        dao.delete(dialogId, messageId)
    }

    override fun all(): List<Bookmark> = dao.all().map(::bookmark)

    override fun inChat(dialogId: Long): List<Bookmark> = dao.inChat(dialogId).map(::bookmark)

    internal fun close() {
        database.close()
    }

    private fun bookmark(entity: BookmarkEntity) = Bookmark(
        dialogId = entity.dialogId,
        messageId = entity.messageId,
        messageDate = entity.messageDate,
        senderId = entity.senderId,
        text = entity.text,
        createdAt = entity.createdAt,
    )
}
