package com.eltavine.sereingram.features.localnames

import android.view.View
import com.eltavine.sereingram.support.Chats
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
            items.add(UItem.asButton(index + 1, names.getValue(peerId), feature.originals.of(currentAccount, peerId)?.full.orEmpty()))
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
        feature.set(currentAccount, peerId, null)
        listView.adapter.update(true)
        return true
    }

    override fun onResume() {
        super.onResume()
        listView?.adapter?.update(true)
    }
}
