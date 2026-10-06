package com.eltavine.sereingram.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HandlersTest {
    private fun interface Handler {
        fun name(): String
    }

    @Test
    fun keepsInstallOrderAndRemovesOnlyTheClosedHandler() {
        val handlers = Handlers<Handler>()
        val first = Handler { "first" }
        val second = Handler { "second" }
        val firstInstall = handlers.install(first)
        handlers.install(second)
        assertEquals(listOf("first", "second"), handlers.all.map { it.name() })
        firstInstall.close()
        assertEquals(listOf("second"), handlers.all.map { it.name() })
    }

    @Test
    fun aSnapshotIsUnaffectedByLaterInstalls() {
        val handlers = Handlers<Handler>()
        val snapshot = handlers.all
        handlers.install { "late" }
        assertTrue(snapshot.isEmpty())
    }
}
