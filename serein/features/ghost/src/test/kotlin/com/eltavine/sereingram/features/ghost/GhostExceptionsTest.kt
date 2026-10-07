package com.eltavine.sereingram.features.ghost

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GhostExceptionsTest {
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
