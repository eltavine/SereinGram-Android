package com.eltavine.sereingram.ui

import android.content.Context
import android.view.View
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.RecyclerListView
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import org.telegram.ui.Components.UniversalRecyclerView
import org.telegram.ui.SettingsActivity

/**
 * The row of Telegram's own main settings: an icon on a coloured tile, the title, a
 * grey line under it and a value at the end. Unlike Telegram's list of them, the row
 * keeps its place when its value changes, so a status that appears does not redraw it.
 */
internal class FeatureCell private constructor() : UItem.UItemFactory<SettingsActivity.SettingCell>() {
    override fun createView(context: Context, listView: RecyclerListView?, currentAccount: Int, classGuid: Int, resourcesProvider: Theme.ResourcesProvider?) =
        SettingsActivity.SettingCell(context, resourcesProvider)

    override fun bindView(view: View, item: UItem, divider: Boolean, adapter: UniversalAdapter?, listView: UniversalRecyclerView?) {
        val cell = view as SettingsActivity.SettingCell
        cell.set(item.intValue, item.longValue.toInt(), item.iconResId, item.text, item.subtext, item.textValue)
        dim(cell, item.enabled)
    }

    override fun equals(a: UItem, b: UItem): Boolean = a.id == b.id

    companion object {
        init {
            setup(FeatureCell())
        }

        fun of(id: Int, colorTop: Int, colorBottom: Int, icon: Int, title: CharSequence, summary: CharSequence?, value: CharSequence?, enabled: Boolean = true): UItem =
            UItem.ofFactory(FeatureCell::class.java).apply {
                this.id = id
                intValue = colorTop
                longValue = colorBottom.toLong()
                iconResId = icon
                text = title
                subtext = summary
                textValue = value
                this.enabled = enabled
            }
    }
}
