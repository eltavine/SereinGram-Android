package com.eltavine.sereingram.settings

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.eltavine.sereingram.app.SereinApp
import com.eltavine.sereingram.app.featureSectionsOf
import com.eltavine.sereingram.app.rootPage
import com.eltavine.sereingram.core.Faults
import com.eltavine.sereingram.features.backup.BackupSettings
import com.eltavine.sereingram.core.KeyValueStore
import com.eltavine.sereingram.core.MemoryKeyValueStore
import com.eltavine.sereingram.core.OptionScope
import com.eltavine.sereingram.core.Options
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.telegram.messenger.ApplicationLoader

/** The settings pages of the real modules, as the SereinGram page lists and draws them. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class SettingsPagesTest {
    private val contributors: List<SettingsContributor> by lazy {
        val application = ApplicationProvider.getApplicationContext<Application>()
        ApplicationLoader.applicationContext = application
        SereinApp.modules(application).modules.filterIsInstance<SettingsContributor>()
    }

    private val stores = HashMap<Pair<OptionScope, Int>, KeyValueStore>()
    private val state = SettingsState(Options { scope, account -> stores.getOrPut(scope to account) { MemoryKeyValueStore() } }, account = 0)

    @Test
    fun everyFeatureSaysWhatItHoldsAndWhereItGoes() {
        contributors.forEach { feature ->
            val name = feature::class.java.simpleName
            assertNotNull("$name says nothing under its row", feature.settingsPage.summary)
            assertNotEquals("$name has no group of its own", SettingsCategory.MORE, feature.settingsCategory)
        }
    }

    // A tile's colour and picture are kept under its key, and a colour shared would make two features look alike.
    @Test
    fun everyFeatureTileHasAKeyAndAColourOfItsOwn() {
        assertEquals(contributors.size, contributors.mapNotNull { it.settingsKey }.distinct().size)
        assertEquals(contributors.size, contributors.map { it.settingsTint }.distinct().size)
    }

    @Test
    fun everyPageLaysOutWithoutAFault() {
        val faults = ArrayList<String>()
        Faults.reporters.install { message, error -> faults += "$message: $error" }.use {
            contributors.forEach { PageLayout.of(it.settingsPage, state) }
        }
        assertEquals(emptyList<String>(), faults)
    }

    // A slider's title is its header and its section's note explains it; an inline choice says what each choice does.
    @Test
    fun everyRowSaysWhatItDoes() {
        val root = rootPage(featureSectionsOf(contributors) + BackupSettings.section(state.options, emptyList()))
        val silent = rowsOf(root).filter { row ->
            when {
                row is SettingsRow.Choice && row.style == SettingsRow.ChoiceStyle.SLIDER -> false
                row is SettingsRow.Choice && row.style == SettingsRow.ChoiceStyle.INLINE -> row.describe == null
                else -> row.summary == null
            }
        }
        assertEquals(emptyList<String>(), silent.map { ApplicationProvider.getApplicationContext<Application>().getString(titleOf(it)) })
    }

    @Test
    fun noTextRowRejectsTheDefaultOfItsOption() {
        contributors.flatMap { feature -> feature.settingsPage.sections.flatMap { it.rows } }
            .filterIsInstance<SettingsRow.Text>()
            .filter { it.option.default.isNotEmpty() }
            .forEach { row -> assertNull(row.option.key, row.check?.invoke(row.option.default)) }
    }

    private fun rowsOf(page: SettingsPage): List<SettingsRow> = page.sections.flatMap { it.rows }.flatMap { row ->
        listOf(row) + ((row as? SettingsRow.Subpage)?.let { rowsOf(it.page) } ?: emptyList())
    }

    private fun titleOf(row: SettingsRow): Int = when (row) {
        is SettingsRow.Toggle -> row.title
        is SettingsRow.Text -> row.title
        is SettingsRow.Choice -> row.title
        is SettingsRow.Switch -> row.title
        is SettingsRow.Subpage -> row.page.title
        is SettingsRow.Screen -> row.title
        is SettingsRow.Link -> row.title
        is SettingsRow.Action -> row.title
        is SettingsRow.PickFile -> row.title
    }
}
