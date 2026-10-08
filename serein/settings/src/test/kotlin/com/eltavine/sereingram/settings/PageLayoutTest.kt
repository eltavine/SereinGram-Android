package com.eltavine.sereingram.settings

import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.core.MemoryKeyValueStore
import com.eltavine.sereingram.core.OptionScope
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.core.booleanOption
import com.eltavine.sereingram.core.intOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PageLayoutTest {
    private val stores = HashMap<Pair<OptionScope, Int>, MemoryKeyValueStore>()
    private val options = Options { scope, account -> stores.getOrPut(scope to account) { MemoryKeyValueStore() } }
    private val state = SettingsState(options, account = 0)

    private val backup = booleanOption("backup_media", default = true)
    private val inGroups = booleanOption("backup_in_groups")
    private val mode = intOption("resolver_mode")

    private val keep = SettingsRow.Toggle(backup, title = 1)
    private val custom = SettingsRow.Toggle(inGroups, title = 2, shownWhen = SettingsCondition.isSetTo(mode, 2))
    private val groups = SettingsRow.Toggle(inGroups, title = 3, enabledWhen = SettingsCondition.isOn(backup))

    @Test
    fun hiddenRowsLeaveTheIdsOfTheOthersAlone() {
        val page = SettingsPage(title = 0, sections = listOf(SettingsSection(listOf(keep, custom, groups))))
        val withoutCustom = PageLayout.of(page, state).sections.single().rows
        options.set(mode, 2)
        val withCustom = PageLayout.of(page, state).sections.single().rows

        assertEquals(listOf(keep, groups), withoutCustom.map { it.row })
        assertEquals(listOf(keep, custom, groups), withCustom.map { it.row })
        assertEquals(withoutCustom.last().id, withCustom.last().id)
        assertEquals(withCustom.map { it.id }.distinct(), withCustom.map { it.id })
    }

    @Test
    fun idsCountRowsAcrossSectionsThatAreHidden() {
        val hidden = SettingsSection(listOf(keep), shownWhen = SettingsCondition.isSetTo(mode, 1))
        val page = SettingsPage(title = 0, sections = listOf(hidden, SettingsSection(listOf(groups))))
        val shownWhileHidden = PageLayout.of(page, state).sections.single().rows.single().id
        options.set(mode, 1)

        assertEquals(shownWhileHidden, PageLayout.of(page, state).sections.last().rows.single().id)
    }

    @Test
    fun sectionsHideWithTheirLastRow() {
        val page = SettingsPage(title = 0, sections = listOf(SettingsSection(listOf(custom), header = 7), SettingsSection(listOf(keep))))

        assertEquals(listOf(keep), PageLayout.of(page, state).sections.single().rows.map { it.row })
    }

    @Test
    fun rowsAreUnusableWhileTheirConditionFails() {
        val page = SettingsPage(title = 0, sections = listOf(SettingsSection(listOf(keep, groups))))
        assertEquals(listOf(true, true), PageLayout.of(page, state).sections.single().rows.map { it.enabled })
        options.set(backup, false)
        assertEquals(listOf(true, false), PageLayout.of(page, state).sections.single().rows.map { it.enabled })
    }

    @Test
    fun everyItemOfARowLeadsBackToIt() {
        val page = SettingsPage(title = 0, sections = listOf(SettingsSection(listOf(keep, groups))))
        val layout = PageLayout.of(page, state)
        val second = layout.sections.single().rows.last()

        assertSame(second, layout.rowOf(second.id))
        assertSame(second, layout.rowOf(second.id + PageLayout.ITEM_SPAN - 1))
        assertNull(layout.rowOf(0))
        assertNull(layout.rowOf(-1))
    }

    @Test
    fun editorsAreFoundOnThePagesAPageOpens() {
        val inner = SettingsPage(title = 9, sections = listOf(SettingsSection(listOf(groups))))
        val link = SettingsRow.Link(title = 4, url = "https://example.org")
        val page = SettingsPage(title = 0, sections = listOf(SettingsSection(listOf(keep, link, SettingsRow.Subpage(inner)))))

        assertEquals(listOf<SettingsRow>(keep, groups), page.editors())
    }

    @Test
    fun aConditionThatThrowsIsReportedAndHolds() {
        val reported = ArrayList<String>()
        val broken = SettingsCondition { error("an option went away") }
        val page = SettingsPage(title = 0, sections = listOf(SettingsSection(listOf(SettingsRow.Toggle(backup, title = 1, enabledWhen = broken)))))
        val layout = Faults.reporters.install { message, _ -> reported += message }.use { PageLayout.of(page, state) }

        assertTrue(layout.sections.single().rows.single().enabled)
        assertEquals(listOf("settings condition"), reported)
    }
}
