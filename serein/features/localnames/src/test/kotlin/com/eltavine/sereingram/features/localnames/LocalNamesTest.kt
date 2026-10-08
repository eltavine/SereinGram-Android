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

        override fun clearAll() = names.clear()
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
    fun forgettingAnAccountErasesItsNamesAndOnlyIts() {
        val stores = mapOf(0 to MemoryStore(mapOf(7L to "Mum")), 1 to MemoryStore(mapOf(7L to "Dad")))
        val names = LocalNames { stores.getValue(it) }
        assertEquals("Mum", names.of(0, 7))
        names.forget(0)
        assertNull(names.of(0, 7))
        assertEquals(emptyMap(), stores.getValue(0).names)
        assertEquals("Dad", names.of(1, 7))
        val originals = OriginalNames()
        originals.replace(0, 7, PeerName("Alice"), "Mum")
        originals.replace(1, 7, PeerName("Bob"), "Dad")
        originals.forgetAccount(0)
        assertNull(originals.of(0, 7))
        assertEquals(PeerName("Bob"), originals.of(1, 7))
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
    fun aLocalNameReplacesTelegramsNameWhichIsRemembered() {
        val originals = OriginalNames()
        assertEquals(PeerName("Boss"), originals.replace(0, 7, PeerName("Alice", "Smith"), "Boss"))
        assertEquals(PeerName("Alice", "Smith"), originals.of(0, 7))
        assertNull(originals.replace(0, 7, PeerName("Boss"), "Boss"), "a peer that shows its local name stays as it is")
        assertEquals(PeerName("Alice", "Smith"), originals.of(0, 7))
        assertNull(originals.of(1, 7))
        originals.forget(0, 7)
        assertNull(originals.of(0, 7))
    }

    @Test
    fun onlyAPeerShowingItsLocalNameHasTelegramsNameBehindIt() {
        val originals = OriginalNames()
        originals.replace(0, 7, PeerName("Alice", "Smith"), "Boss")
        assertEquals(PeerName("Alice", "Smith"), originals.behind(0, 7, PeerName("Boss")))
        assertNull(originals.behind(0, 7, PeerName("Alicia")), "a fresh name from Telegram is its own")
        assertNull(originals.behind(1, 7, PeerName("Boss")))
        originals.forget(0, 7)
        assertNull(originals.behind(0, 7, PeerName("Boss")), "without a local name nothing is behind")
    }

    @Test
    fun changingALocalNameKeepsTelegramsName() {
        val originals = OriginalNames()
        originals.replace(0, 7, PeerName("Alice", "Smith"), "Boss")
        assertEquals(PeerName("Chief"), originals.replace(0, 7, PeerName("Boss"), "Chief"))
        assertEquals(PeerName("Alice", "Smith"), originals.of(0, 7))
        assertNull(originals.behind(0, 7, PeerName("Boss")), "an earlier local name is not shown any more")
        assertEquals(PeerName("Alice", "Smith"), originals.behind(0, 7, PeerName("Chief")))
        originals.replace(0, 7, PeerName("Alicia", "Smith"), "Chief")
        assertEquals(PeerName("Alicia", "Smith"), originals.of(0, 7), "Telegram renaming the peer is remembered")
    }

    @Test
    fun fullNamesLeaveOutWhatIsBlank() {
        assertEquals("Alice Smith", PeerName("Alice", "Smith").full)
        assertEquals("Book club", PeerName("Book club").full)
        assertEquals("Smith", PeerName(" ", "Smith").full)
    }
}
