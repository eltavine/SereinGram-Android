package com.eltavine.sereingram.features.localnames

import android.view.View
import com.eltavine.sereingram.support.Chats
import com.eltavine.sereingram.ui.ChatCell
import com.eltavine.sereingram.ui.RowAction
import com.eltavine.sereingram.ui.showRowMenu
import org.telegram.messenger.LocaleController.formatString
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import org.telegram.ui.Components.UniversalFragment

/** Every local name of this account, with the name Telegram gives where it is known. */
internal class LocalNamesActivity(private val feature: LocalNamesFeature) : UniversalFragment() {
    private val shown = ArrayList<Long>()

    override fun getTitle(): CharSequence = getString(R.string.serein_local_names_all)

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter) {
        shown.clear()
        val names = feature.names.all(currentAccount)
        shown += names.keys.sorted()
        shown.forEachIndexed { index, peerId ->
            val original = feature.originals.of(currentAccount, peerId)?.full?.takeIf { it.isNotBlank() }
            val status = original?.let { formatString(R.string.serein_local_names_original, it) }
            items.add(ChatCell.of(index + 1, currentAccount, peerId, status = status, name = names.getValue(peerId)))
        }
        val note = if (shown.isEmpty()) R.string.serein_local_names_empty else R.string.serein_local_names_list_note
        items.add(UItem.asShadow(getString(note)))
    }

    override fun onClick(item: UItem, view: View, position: Int, x: Float, y: Float) {
        val peerId = shown.getOrNull(item.id - 1) ?: return
        presentFragment(Chats.screen(peerId))
    }

    override fun onLongClick(item: UItem, view: View, position: Int, x: Float, y: Float): Boolean {
        val peerId = shown.getOrNull(item.id - 1) ?: return false
        showRowMenu(view, RowAction(R.drawable.msg_delete, getString(R.string.serein_local_names_remove), destructive = true) { remove(peerId) })
        return true
    }

    private fun remove(peerId: Long) {
        feature.set(currentAccount, peerId, null)
        listView?.adapter?.update(true)
    }

    override fun onResume() {
        super.onResume()
        listView?.adapter?.update(true)
    }
}
