package com.eltavine.sereingram.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OptionsTest {
    private val device = MemoryKeyValueStore()
    private val accounts = mutableMapOf<Int, MemoryKeyValueStore>()
    private val options = Options { scope, account ->
        if (scope == OptionScope.DEVICE) device else accounts.getOrPut(account) { MemoryKeyValueStore() }
    }

    private val compact = booleanOption("compact_mode")
    private val limit = intOption("history_limit", default = 100)
    private val ghost = booleanOption("ghost_mode", scope = OptionScope.ACCOUNT)

    @Test
    fun secretsAreNeverBackedUp() {
        assertFalse(textOption("some_key", secret = true).backedUp)
        assertTrue(textOption("some_name").backedUp)
        assertFailsWith<IllegalArgumentException> { textOption("some_key", secret = true, backedUp = true) }
    }

    @Test
    fun unsetOptionsReadTheirDefault() {
        assertFalse(options.get(compact))
        assertEquals(100, options.get(limit))
        assertFalse(options.isModified(limit))
    }

    @Test
    fun writingTheDefaultRemovesTheStoredValue() {
        options.set(limit, 5)
        assertTrue(options.isModified(limit))
        options.set(limit, 100)
        assertFalse(options.isModified(limit))
        assertEquals(null, device.getInt("history_limit"))
    }

    @Test
    fun accountsAreStoredSeparately() {
        options.set(ghost, true, account = 0)
        assertTrue(options.get(ghost, account = 0))
        assertFalse(options.get(ghost, account = 1))
    }

    @Test
    fun deviceOptionsIgnoreTheAccount() {
        options.set(compact, true, account = 3)
        assertTrue(options.get(compact))
        assertTrue(options.get(compact, account = 0))
    }

    @Test
    fun accountOptionsNeedAnAccount() {
        assertFailsWith<IllegalArgumentException> { options.get(ghost) }
    }

    @Test
    fun listenersHearWritesAndResets() {
        val heard = mutableListOf<Pair<String, Int>>()
        val listener = options.addListener { option, account -> heard += option.key to account }
        options.set(ghost, true, account = 2)
        options.set(compact, true, account = 2)
        options.reset(ghost, account = 2)
        listener.close()
        options.set(compact, false)
        assertEquals(
            listOf("ghost_mode" to 2, "compact_mode" to Options.NO_ACCOUNT, "ghost_mode" to 2),
            heard,
        )
    }

    @Test
    fun keysMustBeLowerSnakeCase() {
        assertFailsWith<IllegalArgumentException> { booleanOption("GhostMode") }
        assertFailsWith<IllegalArgumentException> { booleanOption("ghost__mode") }
        assertFailsWith<IllegalArgumentException> { booleanOption("_ghost") }
    }

    @Test
    fun cachedOptionsFollowWritesWithoutReadingAgain() {
        val reads = mutableListOf<String>()
        val counted = Options { scope, account ->
            object : KeyValueStore by (if (scope == OptionScope.DEVICE) device else accounts.getOrPut(account) { MemoryKeyValueStore() }) {
                override fun getBoolean(key: String): Boolean? {
                    reads += key
                    return device.getBoolean(key)
                }
            }
        }
        val compactMode = counted.cached(compact)
        assertFalse(compactMode.value)
        repeat(3) { compactMode.value }
        assertEquals(listOf("compact_mode"), reads)
        counted.set(compact, true)
        assertTrue(compactMode.value)
        counted.reset(compact)
        assertFalse(compactMode.value)
        assertFailsWith<IllegalArgumentException> { counted.cached(ghost) }
    }
}
