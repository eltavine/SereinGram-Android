package com.eltavine.sereingram.ui

import android.content.Context
import android.view.View
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.RadioButtonCell
import org.telegram.ui.Components.RecyclerListView
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import org.telegram.ui.Components.UniversalRecyclerView

/**
 * One choice among several, with what it means in grey under it, as Telegram offers
 * private and public groups. The chosen one's button animates over when it changes.
 */
internal class RadioCell private constructor() : UItem.UItemFactory<RadioButtonCell>() {
    override fun createView(context: Context, listView: RecyclerListView?, currentAccount: Int, classGuid: Int, resourcesProvider: Theme.ResourcesProvider?) =
        RadioButtonCell(context)

    override fun bindView(view: View, item: UItem, divider: Boolean, adapter: UniversalAdapter?, listView: UniversalRecyclerView?) {
        val cell = view as RadioButtonCell
        val same = cell.itemId == item.id
        val drawn = if (same) cell.isChecked else item.checked
        val description = item.subtext?.toString()
        // Telegram draws these without dividers; the cell's own would not line up with its text.
        if (description.isNullOrEmpty()) {
            cell.setTextAndValue(item.text.toString(), false, drawn)
        } else {
            cell.setTextAndValueAndCheck(item.text.toString(), description, false, drawn)
        }
        if (same) {
            cell.setChecked(item.checked, true)
        }
        dim(cell, item.enabled)
        cell.itemId = item.id
    }

    override fun equals(a: UItem, b: UItem): Boolean = a.id == b.id

    companion object {
        init {
            setup(RadioCell())
        }

        fun of(id: Int, title: CharSequence, description: CharSequence?, checked: Boolean, enabled: Boolean = true): UItem =
            UItem.ofFactory(RadioCell::class.java).apply {
                this.id = id
                text = title
                subtext = description
                this.checked = checked
                this.enabled = enabled
            }
    }
}
