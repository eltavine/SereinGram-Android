package com.eltavine.sereingram.features.ghost

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GhostExceptionsTest {
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

    @Test
    fun aReadPassIsUsedOnceAndOnlyInTime() {
        var now = 1_000L
        val passes = ReadPasses(ttlMillis = 100) { now }
        assertFalse(passes.use(0, 5))
        passes.grant(0, 5)
        assertFalse(passes.use(1, 5))
        assertTrue(passes.use(0, 5))
        assertFalse(passes.use(0, 5))
        passes.grant(0, 5)
        now += 101
        assertFalse(passes.use(0, 5))
    }
}
