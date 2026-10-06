package com.eltavine.sereingram.hooks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsHooksTest {
    private fun entry(id: Int, opened: MutableList<Any>) =
        SettingsHooks.Entry(id, icon = 0, iconColorTop = 0, iconColorBottom = 0, { "SereinGram" }, { null }) {
            opened += it
        }

    @Test
    fun opensOnlyItsOwnEntries() {
        val opened = mutableListOf<Any>()
        val installed = SettingsHooks.mainEntries.install(entry(1001, opened))
        try {
            assertEquals(listOf(1001), SettingsHooks.mainSettingsEntries().map { it.id })
            assertTrue(SettingsHooks.openMainSettingsEntry(1001, "host"))
            assertFalse(SettingsHooks.openMainSettingsEntry(100, "host"))
            assertEquals(listOf<Any>("host"), opened)
        } finally {
            installed.close()
        }
    }

    @Test
    fun refusesIdsTelegramMayUse() {
        assertFailsWith<IllegalArgumentException> { entry(100, mutableListOf()) }
    }
}
