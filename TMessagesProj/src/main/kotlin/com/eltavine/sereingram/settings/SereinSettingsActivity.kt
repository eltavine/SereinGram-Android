package com.eltavine.sereingram.settings

import android.view.View
import com.eltavine.sereingram.core.Option
import com.eltavine.sereingram.core.OptionScope
import com.eltavine.sereingram.core.Options
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.browser.Browser
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import org.telegram.ui.Components.UniversalFragment

/** Draws one [SettingsPage] with Telegram's list cells. */
class SereinSettingsActivity(
    private val page: SettingsPage,
    private val options: Options,
) : UniversalFragment() {
    private val rows = HashMap<Int, SettingsRow>()

    override fun getTitle(): CharSequence = getString(page.title)

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter) {
        rows.clear()
        page.sections.forEach { section ->
            section.header?.let { items.add(UItem.asHeader(getString(it))) }
            section.rows.forEach { row ->
                val id = rows.size + 1
                rows[id] = row
                items.add(item(id, row))
            }
            items.add(UItem.asShadow(section.note?.let(::getString)))
        }
    }

    private fun item(id: Int, row: SettingsRow): UItem = when (row) {
        is SettingsRow.Toggle -> UItem.asCheck(id, getString(row.title)).setChecked(options.get(row.option, account(row.option)))
        is SettingsRow.Subpage -> UItem.asButton(id, row.icon, getString(row.page.title))
        is SettingsRow.Link -> UItem.asButton(id, getString(row.title), row.value())
    }

    override fun onClick(item: UItem, view: View, position: Int, x: Float, y: Float) {
        when (val row = rows[item.id]) {
            is SettingsRow.Toggle -> {
                val account = account(row.option)
                options.set(row.option, !options.get(row.option, account), account)
                listView.adapter.update(true)
            }
            is SettingsRow.Subpage -> presentFragment(SereinSettingsActivity(row.page, options))
            is SettingsRow.Link -> Browser.openUrl(parentActivity, row.url)
            null -> Unit
        }
    }

    override fun onLongClick(item: UItem, view: View, position: Int, x: Float, y: Float): Boolean = false

    private fun account(option: Option<*>): Int =
        if (option.scope == OptionScope.ACCOUNT) currentAccount else Options.NO_ACCOUNT
}
