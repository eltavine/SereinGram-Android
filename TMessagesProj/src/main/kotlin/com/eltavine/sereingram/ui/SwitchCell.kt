package com.eltavine.sereingram.ui

import android.content.Context
import android.view.View
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.TextCheckCell
import org.telegram.ui.Components.RecyclerListView
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import org.telegram.ui.Components.UniversalRecyclerView

/**
 * Telegram's switch row, with what the switch does in grey under its title. The
 * row keeps its place while it changes, so the switch slides as it does elsewhere.
 * The description sits at a fixed height below the title, so titles stay on one line.
 */
internal class SwitchCell private constructor() : UItem.UItemFactory<TextCheckCell>() {
    override fun createView(context: Context, listView: RecyclerListView?, currentAccount: Int, classGuid: Int, resourcesProvider: Theme.ResourcesProvider?) =
        TextCheckCell(context, resourcesProvider)

    override fun bindView(view: View, item: UItem, divider: Boolean, adapter: UniversalAdapter?, listView: UniversalRecyclerView?) {
        val cell = view as TextCheckCell
        val same = cell.itemId == item.id
        val drawn = if (same) cell.isChecked else item.checked
        val summary = item.subtext?.toString()
        if (summary.isNullOrEmpty()) {
            cell.setTextAndCheck(item.text, drawn, divider)
        } else {
            cell.setTextAndValueAndCheck(item.text.toString(), summary, drawn, true, divider)
        }
        if (same) {
            cell.setChecked(item.checked)
        }
        cell.setEnabled(item.enabled, null)
        cell.itemId = item.id
    }

    override fun equals(a: UItem, b: UItem): Boolean = a.id == b.id

    companion object {
        init {
            setup(SwitchCell())
        }

        fun of(id: Int, title: CharSequence, summary: CharSequence?, checked: Boolean, enabled: Boolean = true): UItem =
            UItem.ofFactory(SwitchCell::class.java).apply {
                this.id = id
                text = title
                subtext = summary
                this.checked = checked
                this.enabled = enabled
            }
    }
}
