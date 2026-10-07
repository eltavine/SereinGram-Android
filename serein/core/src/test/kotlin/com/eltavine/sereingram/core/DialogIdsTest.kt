package com.eltavine.sereingram.core

import kotlin.test.Test
import kotlin.test.assertEquals

class DialogIdsTest {
    @Test
    fun chatIdsSurviveTheirTextFormAndSkipWhatIsNotAnId() {
        assertEquals(setOf(5L, -1001234567890L), DialogIds.parse("5, -1001234567890,,x"))
        assertEquals("5,-100", DialogIds.format(linkedSetOf(5L, -100L)))
        assertEquals(emptySet(), DialogIds.parse(""))
    }

    @Test
    fun chatsAreAddedOnceAndRemoved() {
        val once = DialogIds.with(DialogIds.with("", 5, included = true), 5, included = true)
        assertEquals("5", once)
        assertEquals("5,-100", DialogIds.with(once, -100, included = true))
        assertEquals("-100", DialogIds.with("5,-100", 5, included = false))
    }
}
