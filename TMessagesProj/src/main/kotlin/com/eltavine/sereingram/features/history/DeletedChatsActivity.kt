package com.eltavine.sereingram.features.history

import android.view.View
import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.ports.HistoryStore
import com.eltavine.sereingram.ports.KeptChat
import com.eltavine.sereingram.ports.RecordKind
import com.eltavine.sereingram.ui.ChatCell
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.LocaleController
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.messenger.Utilities
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import org.telegram.ui.Components.UniversalFragment

/** Every chat with kept deleted messages, the latest deletion first; each opens its own list. */
internal class DeletedChatsActivity(
    private val store: HistoryStore,
    private val forgetMedia: (dialogId: Long) -> Unit,
) : UniversalFragment() {
    private var chats: List<KeptChat>? = null

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        Utilities.globalQueue.postRunnable {
            val loaded = Faults.guard("deleted chats", fallback = emptyList()) { store.chats(RecordKind.DELETED) }
            AndroidUtilities.runOnUIThread {
                chats = loaded
                listView?.adapter?.update(true)
            }
        }
    }

    override fun getTitle(): CharSequence = getString(R.string.serein_history_deleted_all)

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter) {
        val shown = chats ?: return
        if (shown.isEmpty()) {
            items.add(UItem.asShadow(getString(R.string.serein_history_deleted_all_empty)))
            return
        }
        shown.forEachIndexed { index, chat ->
            items.add(ChatCell.of(index + 1, currentAccount, chat.dialogId, status = LocaleController.formatPluralString("Messages", chat.count)))
        }
        items.add(UItem.asShadow(getString(R.string.serein_history_deleted_all_note)))
    }

    override fun onClick(item: UItem, view: View, position: Int, x: Float, y: Float) {
        val chat = chats?.getOrNull(item.id - 1) ?: return
        presentFragment(DeletedMessagesActivity(store, chat.dialogId, null) { forgetMedia(chat.dialogId) })
    }

    override fun onLongClick(item: UItem, view: View, position: Int, x: Float, y: Float): Boolean = false
}
