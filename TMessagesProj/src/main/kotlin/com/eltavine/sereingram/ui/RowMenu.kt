package com.eltavine.sereingram.ui

import android.view.View
import org.telegram.ui.Components.ItemOptions
import org.telegram.ui.Components.UniversalFragment

/**
 * Telegram's menu over a long-pressed row of the list, with [actions] in it, so that a
 * long press asks before anything goes rather than removing it at once.
 */
internal fun UniversalFragment.showRowMenu(row: View, vararg actions: RowAction) {
    val menu = ItemOptions.makeOptions(this, row)
    actions.forEach { action -> menu.add(action.icon, action.title, action.destructive) { action.run() } }
    menu.setScrimViewBackground(listView.getClipBackground(row)).show()
}

internal class RowAction(val icon: Int, val title: CharSequence, val destructive: Boolean = false, val run: () -> Unit)
