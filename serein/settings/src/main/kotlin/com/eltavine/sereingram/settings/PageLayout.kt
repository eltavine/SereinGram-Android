package com.eltavine.sereingram.settings

import com.eltavine.sereingram.core.Faults

/**
 * What a page shows while the settings are as a [SettingsState] has them: the
 * sections that have a row to show, and in them the rows that show, each marked
 * usable or not. A condition that throws is reported and taken to hold, which
 * leaves its row as it would be without one.
 */
public class PageLayout private constructor(public val sections: List<Section>) {
    public class Section internal constructor(public val section: SettingsSection, public val rows: List<Row>)

    /**
     * A row the page shows. Its [id] stays the row's whichever other rows show, as long as
     * the page is declared the same, and the [ITEM_SPAN] ids from it on are the row's, to
     * number the items it is drawn as.
     */
    public class Row internal constructor(public val id: Int, public val row: SettingsRow, public val enabled: Boolean)

    private val rows: Map<Int, Row> = sections.flatMap { it.rows }.associateBy { it.id }

    /** The row that the item with [itemId] was drawn for. */
    public fun rowOf(itemId: Int): Row? = rows[itemId - Math.floorMod(itemId, ITEM_SPAN)]

    public companion object {
        /** How many items one row may be drawn as, such as the choices of an inline [SettingsRow.Choice]. */
        public const val ITEM_SPAN: Int = 256

        public fun of(page: SettingsPage, state: SettingsState): PageLayout {
            var declared = 0
            val sections = page.sections.mapNotNull { section ->
                val ids = section.rows.map { ++declared * ITEM_SPAN }
                if (!holds(section.shownWhen, state)) {
                    return@mapNotNull null
                }
                val rows = section.rows.zip(ids)
                    .filter { (row, _) -> holds(row.shownWhen, state) }
                    .map { (row, id) -> Row(id, row, holds(row.enabledWhen, state)) }
                if (rows.isEmpty()) null else Section(section, rows)
            }
            return PageLayout(sections)
        }

        private fun holds(condition: SettingsCondition, state: SettingsState): Boolean =
            condition === SettingsCondition.ALWAYS || Faults.guard("settings condition", fallback = true) { condition.holds(state) }
    }
}
