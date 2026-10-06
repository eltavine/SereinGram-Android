package com.eltavine.sereingram.ports

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecordKindTest {
    @Test
    fun storedCodesNeverChange() {
        assertEquals(listOf(1, 2), RecordKind.entries.map { it.code })
        assertEquals(RecordKind.EDITED, RecordKind.of(2))
        assertNull(RecordKind.of(0))
    }
}
