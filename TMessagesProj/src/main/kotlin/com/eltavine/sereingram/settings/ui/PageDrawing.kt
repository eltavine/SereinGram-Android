package com.eltavine.sereingram.settings.ui

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.settings.PageLayout
import com.eltavine.sereingram.settings.SettingsPage
import org.telegram.messenger.LocaleController.getString
import org.telegram.ui.Components.UItem

/**
 * Adds the items of [page] as the settings are now: each section that shows under its
 * header, its rows as their binders draw them, closed by its note. Returns what it laid
 * out, which says what each item stands for.
 */
internal fun drawPage(page: SettingsPage, host: RowHost, items: MutableList<UItem>): PageLayout {
    val layout = PageLayout.of(page, host.state)
    layout.sections.forEach { section ->
        section.section.header?.let { items.add(UItem.asHeader(getString(it))) }
        section.rows.forEach { row -> items.addAll(draw(row, host)) }
        items.add(UItem.asShadow(section.section.note?.let(::getString)))
    }
    return layout
}

// A row that fails to draw is left out whole, rather than drawn in part.
private fun draw(row: PageLayout.Row, host: RowHost): List<UItem> = Faults.guard("settings row", fallback = emptyList()) {
    ArrayList<UItem>().also { binderOf(row.row).draw(row.row, row.id, row.enabled, host, it) }
}
