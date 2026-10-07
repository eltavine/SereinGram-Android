package com.eltavine.sereingram.features.chatlock

import android.view.View
import com.eltavine.sereingram.core.DialogIds
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.support.Chats
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import org.telegram.ui.Components.UniversalFragment

/** The chats of this account that ask to be unlocked before they open. */
internal class LockedChatsActivity(private val options: Options) : UniversalFragment() {
    private val shown = ArrayList<Long>()

    override fun getTitle(): CharSequence = getString(R.string.serein_lock_chats)

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter) {
        shown.clear()
        shown += DialogIds.parse(options.get(ChatLockOptions.lockedChats, currentAccount))
        shown.forEachIndexed { index, dialogId -> items.add(UItem.asButton(index + 1, Chats.name(currentAccount, dialogId))) }
        items.add(UItem.asShadow(getString(if (shown.isEmpty()) R.string.serein_lock_list_empty else R.string.serein_lock_list_note)))
    }

    override fun onClick(item: UItem, view: View, position: Int, x: Float, y: Float) {
        val dialogId = shown.getOrNull(item.id - 1) ?: return
        presentFragment(Chats.screen(dialogId))
    }

    override fun onLongClick(item: UItem, view: View, position: Int, x: Float, y: Float): Boolean {
        val dialogId = shown.getOrNull(item.id - 1) ?: return false
        val locked = options.get(ChatLockOptions.lockedChats, currentAccount)
        options.set(ChatLockOptions.lockedChats, DialogIds.with(locked, dialogId, included = false), currentAccount)
        listView.adapter.update(true)
        return true
    }
}
