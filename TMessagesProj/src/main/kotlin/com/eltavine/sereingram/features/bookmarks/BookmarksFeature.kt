package com.eltavine.sereingram.features.bookmarks

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.ModuleContext
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.SereinModule
import com.eltavine.sereingram.hooks.ChatMenuHooks
import com.eltavine.sereingram.hooks.MessageMenuHooks
import com.eltavine.sereingram.ports.Bookmark
import com.eltavine.sereingram.ports.BookmarkStore
import com.eltavine.sereingram.settings.SettingsContributor
import com.eltavine.sereingram.settings.SettingsPage
import com.eltavine.sereingram.settings.SettingsRow
import com.eltavine.sereingram.settings.SettingsSection
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.MessageObject
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import java.util.concurrent.Executors

/** Bookmarks on messages to jump back to later, after NagramX's request. */
class BookmarksFeature(private val stores: (account: Int) -> BookmarkStore) : SereinModule, SettingsContributor {
    override val id: String = "bookmarks"

    override val options: List<Option<*>> = emptyList()

    private val index = BookmarkIndex()
    private val io = Executors.newSingleThreadExecutor { Thread(it, "serein-bookmarks") }

    override fun start(context: ModuleContext) {
        MessageMenuHooks.entries.install(BookmarkMessageEntry(this))
        ChatMenuHooks.entries.install(BookmarksChatEntry(this))
        io.execute {
            for (account in 0 until UserConfig.MAX_ACCOUNT_COUNT) {
                if (UserConfig.getInstance(account).isClientActivated) {
                    load(account)
                }
            }
        }
    }

    internal fun isBookmarked(account: Int, dialogId: Long, messageId: Int): Boolean {
        loadLater(account)
        return index.contains(account, dialogId, messageId)
    }

    internal fun hasAny(account: Int, dialogId: Long): Boolean {
        loadLater(account)
        return index.hasAny(account, dialogId)
    }

    /** Bookmarks [message], or removes its bookmark; returns whether it is bookmarked now. */
    internal fun toggle(account: Int, message: MessageObject): Boolean {
        val bookmarked = !isBookmarked(account, message.dialogId, message.id)
        index.set(account, message.dialogId, message.id, bookmarked)
        val bookmark = if (bookmarked) bookmark(message) else null
        io.execute {
            Faults.guard("bookmark write", fallback = Unit) {
                val store = stores(account)
                if (bookmark != null) store.add(bookmark) else store.remove(message.dialogId, message.id)
            }
        }
        return bookmarked
    }

    internal fun remove(account: Int, dialogId: Long, messageId: Int, then: () -> Unit) {
        index.set(account, dialogId, messageId, false)
        io.execute {
            Faults.guard("bookmark write", fallback = Unit) { stores(account).remove(dialogId, messageId) }
            AndroidUtilities.runOnUIThread(then)
        }
    }

    /** The bookmarks of [dialogId], or of every chat when it is null, handed over on the UI thread. */
    internal fun list(account: Int, dialogId: Long?, then: (List<Bookmark>) -> Unit) {
        io.execute {
            val bookmarks = Faults.guard("bookmark read", fallback = emptyList()) {
                if (dialogId == null) stores(account).all() else stores(account).inChat(dialogId)
            }
            AndroidUtilities.runOnUIThread { then(bookmarks) }
        }
    }

    private fun loadLater(account: Int) {
        if (!index.isLoaded(account)) {
            io.execute { if (!index.isLoaded(account)) load(account) }
        }
    }

    private fun load(account: Int) {
        Faults.guard("bookmark read", fallback = Unit) { index.load(account, stores(account).all()) }
    }

    private fun bookmark(message: MessageObject): Bookmark {
        val text = message.messageOwner?.message?.takeIf { it.isNotBlank() } ?: message.messageText?.toString().orEmpty()
        return Bookmark(
            dialogId = message.dialogId,
            messageId = message.id,
            messageDate = message.messageOwner?.date ?: 0,
            senderId = message.senderId,
            text = snippet(text),
            createdAt = System.currentTimeMillis(),
        )
    }

    override val settingsIcon: Int = R.drawable.msg_fave

    override val settingsPage: SettingsPage = SettingsPage(
        R.string.serein_bookmarks_title,
        listOf(
            SettingsSection(
                rows = listOf(SettingsRow.Screen(R.string.serein_bookmarks_all, { BookmarksActivity(this, dialogId = null, chat = null) })),
                note = R.string.serein_bookmarks_note,
            ),
        ),
    )
}
