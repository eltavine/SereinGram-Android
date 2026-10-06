package com.eltavine.sereingram.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FaultsTest {
    @Test
    fun aThrowingBlockIsReportedAndFallsBack() {
        val reported = mutableListOf<String>()
        val reporter = Faults.reporters.install { message, error -> reported += "$message: ${error.message}" }
        try {
            assertEquals(true, Faults.guard("ghost policy", fallback = true) { throw ClassCastException("upstream type changed") })
            assertEquals(2, Faults.guard("sum", fallback = 0) { 1 + 1 })
        } finally {
            reporter.close()
        }
        assertEquals(listOf("ghost policy: upstream type changed"), reported)
    }

    @Test
    fun virtualMachineErrorsAreNotSwallowed() {
        assertFailsWith<OutOfMemoryError> { Faults.guard("big", Unit) { throw OutOfMemoryError() } }
    }

    @Test
    fun aBrokenReporterDoesNotHideTheFallback() {
        val reporter = Faults.reporters.install { _, _ -> throw IllegalStateException("log is closed") }
        try {
            assertEquals("upstream", Faults.guard("menu", "upstream") { error("boom") })
        } finally {
            reporter.close()
        }
    }
}
