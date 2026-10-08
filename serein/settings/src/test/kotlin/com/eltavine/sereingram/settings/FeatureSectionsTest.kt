package com.eltavine.sereingram.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class FeatureSectionsTest {
    private class Feature(
        title: Int,
        override val settingsCategory: SettingsCategory,
        override val settingsOrder: Int = 0,
        override val settingsTint: SettingsTint = SettingsTint.BLUE,
    ) : SettingsContributor {
        override val settingsPage = SettingsPage(title, emptyList())
        override val settingsIcon: Int = title * 10
    }

    private class Unsorted : SettingsContributor {
        override val settingsPage = SettingsPage(9, emptyList())
        override val settingsIcon: Int = 90
    }

    @Test
    fun rowsGoIntoTheirCategoriesInTheCategoriesOrder() {
        val sections = featureSections(
            listOf(
                Feature(1, SettingsCategory.MEDIA),
                Feature(2, SettingsCategory.PRIVACY),
                Unsorted(),
                Feature(3, SettingsCategory.MEDIA),
            ),
        ) { category -> 100 + category.ordinal }

        assertEquals(listOf(100, 103, 105), sections.map { it.header })
        assertEquals(listOf(listOf(2), listOf(1, 3), listOf(9)), sections.map { section -> section.rows.map { (it as SettingsRow.Subpage).page.title } })
    }

    @Test
    fun theOrderOfARowComesBeforeTheOrderOfItsModule() {
        val sections = featureSections(
            listOf(
                Feature(1, SettingsCategory.PRIVACY, settingsOrder = 2),
                Feature(2, SettingsCategory.PRIVACY),
                Feature(3, SettingsCategory.PRIVACY, settingsOrder = -1),
                Feature(4, SettingsCategory.PRIVACY),
            ),
        ) { null }

        assertEquals(listOf(3, 2, 4, 1), sections.single().rows.map { (it as SettingsRow.Subpage).page.title })
    }

    @Test
    fun rowsKeepTheIconAndTintOfTheirModule() {
        val row = featureSections(listOf(Feature(4, SettingsCategory.CHATS, settingsTint = SettingsTint.PURPLE))) { null }
            .single().rows.single() as SettingsRow.Subpage

        assertEquals(40, row.icon)
        assertEquals(SettingsTint.PURPLE, row.tint)
    }
}
