package com.eltavine.sereingram.features.bookmarks

import android.view.View
import com.eltavine.sereingram.ports.Bookmark
import com.eltavine.sereingram.support.Chats
import com.eltavine.sereingram.ui.RowAction
import com.eltavine.sereingram.ui.SettingsListFragment
import com.eltavine.sereingram.ui.showRowMenu
import org.telegram.messenger.LocaleController
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.ui.ChatActivity
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter

/**
 * The bookmarks of one chat, or of all of them when [dialogId] is null. Opened
 * from [chat], a bookmark of that chat scrolls it instead of opening another.
 */
internal class BookmarksActivity(
    private val feature: BookmarksFeature,
    private val dialogId: Long?,
    private val chat: ChatActivity?,
) : SettingsListFragment() {
    private var bookmarks: List<Bookmark>? = null

    // A bookmark may be added or removed in the chat it leads to.
    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        feature.list(currentAccount, dialogId) {
            bookmarks = it
            listView?.adapter?.update(true)
        }
    }

    override fun getTitle(): CharSequence =
        if (dialogId == null) getString(R.string.serein_bookmarks_all) else getString(R.string.serein_bookmarks_title)

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter) {
        val shown = bookmarks ?: return
        if (shown.isEmpty()) {
            items.add(UItem.asShadow(getString(R.string.serein_bookmarks_empty)))
            return
        }
        shown.forEachIndexed { index, bookmark ->
            items.add(UItem.asButton(index + 1, label(bookmark), LocaleController.formatShortDateTime(bookmark.messageDate.toLong())))
        }
        items.add(UItem.asShadow(getString(R.string.serein_bookmarks_list_note)))
    }

    private fun label(bookmark: Bookmark): String {
        val text = bookmark.text.ifEmpty { getString(R.string.serein_bookmarks_no_text) }
        return if (dialogId == null) Chats.name(currentAccount, bookmark.dialogId) + ": " + text else text
    }

    override fun onClick(item: UItem, view: View, position: Int, x: Float, y: Float) {
        val bookmark = bookmarks?.getOrNull(item.id - 1) ?: return
        val origin = chat
        if (origin != null && origin.dialogId == bookmark.dialogId) {
            finishFragment()
            origin.scrollToMessageId(bookmark.messageId, 0, true, 0, true, 0)
        } else {
            presentFragment(Chats.screen(bookmark.dialogId, bookmark.messageId))
        }
    }

    override fun onLongClick(item: UItem, view: View, position: Int, x: Float, y: Float): Boolean {
        val bookmark = bookmarks?.getOrNull(item.id - 1) ?: return false
        showRowMenu(view, RowAction(R.drawable.msg_delete, getString(R.string.serein_bookmarks_remove), destructive = true) {
            feature.remove(currentAccount, bookmark.dialogId, bookmark.messageId, ::reload)
        })
        return true
    }
}
