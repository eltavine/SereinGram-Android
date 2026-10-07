package com.eltavine.sereingram.features.ghost

import android.view.View
import com.eltavine.sereingram.support.Chats
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import org.telegram.ui.Components.UniversalFragment

/** The chats of this account that ghost mode lets in on reads or typing. */
internal class GhostExceptionsActivity(private val gate: GhostGate) : UniversalFragment() {
    private val shown = ArrayList<Long>()

    override fun getTitle(): CharSequence = getString(R.string.serein_ghost_exceptions)

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter) {
        shown.clear()
        shown += gate.exceptedChats(currentAccount)
        shown.forEachIndexed { index, dialogId ->
            items.add(UItem.asButton(index + 1, Chats.name(currentAccount, dialogId), exceptions(dialogId)))
        }
        val note = if (shown.isEmpty()) R.string.serein_ghost_exceptions_empty else R.string.serein_ghost_exceptions_note
        items.add(UItem.asShadow(getString(note)))
    }

    override fun onClick(item: UItem, view: View, position: Int, x: Float, y: Float) {
        val dialogId = shown.getOrNull(item.id - 1) ?: return
        presentFragment(Chats.screen(dialogId))
    }

    override fun onLongClick(item: UItem, view: View, position: Int, x: Float, y: Float): Boolean {
        val dialogId = shown.getOrNull(item.id - 1) ?: return false
        GhostAction.entries.forEach { gate.setExcepted(currentAccount, dialogId, it, excepted = false) }
        listView.adapter.update(true)
        return true
    }

    override fun onResume() {
        super.onResume()
        listView?.adapter?.update(true)
    }

    private fun exceptions(dialogId: Long): String {
        val reads = gate.isExcepted(currentAccount, dialogId, GhostAction.READ)
        val typing = gate.isExcepted(currentAccount, dialogId, GhostAction.TYPING)
        return getString(
            when {
                reads && typing -> R.string.serein_ghost_exception_both
                reads -> R.string.serein_ghost_chat_reads
                else -> R.string.serein_ghost_chat_typing
            },
        )
    }
}
