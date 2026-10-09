package com.eltavine.sereingram.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class TileTintsTest {
    @Test
    fun chosenColoursComeBackAsWritten() {
        val text = TileTints.with(TileTints.with("", "ghost", SettingsTint.PINK), "dns", SettingsTint.TEAL)

        assertEquals("dns=TEAL,ghost=PINK", text)
        assertEquals(mapOf("dns" to SettingsTint.TEAL, "ghost" to SettingsTint.PINK), TileTints.decode(text))
    }

    @Test
    fun aKeyWithoutAColourGoes() {
        assertEquals("dns=TEAL", TileTints.with("dns=TEAL,ghost=PINK", "ghost", null))
        assertEquals("", TileTints.with("ghost=PINK", "ghost", null))
    }

    @Test
    fun whatALaterReleaseWroteIsSkipped() {
        assertEquals(mapOf("ghost" to SettingsTint.PINK), TileTints.decode("ghost=PINK,dns=ULTRAVIOLET,broken,=,links"))
        assertEquals(emptyMap(), TileTints.decode(""))
    }
}
