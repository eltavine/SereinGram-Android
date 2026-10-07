package com.eltavine.sereingram.features.localnames

import com.eltavine.sereingram.ports.LocalNameStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocalNamesTest {
    private class MemoryStore(initial: Map<Long, String> = emptyMap()) : LocalNameStore {
        val names = HashMap(initial)
        var reads = 0

        override fun all(): Map<Long, String> {
            reads++
            return HashMap(names)
        }

        override fun set(peerId: Long, name: String?) {
            if (name == null) names.remove(peerId) else names[peerId] = name
        }
    }

    @Test
    fun namesAreReadOncePerAccountAndWrittenThrough() {
        val stores = mapOf(0 to MemoryStore(mapOf(7L to "Mum")), 1 to MemoryStore())
        val names = LocalNames { stores.getValue(it) }
        assertEquals("Mum", names.of(0, 7))
        assertNull(names.of(1, 7))
        assertEquals("Book club", names.set(0, -100, "  Book club "))
        assertEquals("Book club", stores.getValue(0).names[-100L])
        assertEquals(mapOf(7L to "Mum", -100L to "Book club"), names.all(0))
        assertEquals(1, stores.getValue(0).reads)
    }

    @Test
    fun aBlankNameRemovesTheLocalName() {
        val store = MemoryStore(mapOf(7L to "Mum"))
        val names = LocalNames { store }
        assertNull(names.set(0, 7, "   "))
        assertNull(names.of(0, 7))
        assertEquals(emptyMap(), store.names)
    }

    @Test
    fun originalNamesAreRememberedUntilForgotten() {
        val originals = OriginalNames()
        originals.remember(0, 7, "Alice Smith")
        assertEquals("Alice Smith", originals.of(0, 7))
        assertNull(originals.of(1, 7))
        originals.forget(0, 7)
        assertNull(originals.of(0, 7))
    }
}
