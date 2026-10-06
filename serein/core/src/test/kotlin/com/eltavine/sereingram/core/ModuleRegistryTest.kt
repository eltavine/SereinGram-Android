package com.eltavine.sereingram.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ModuleRegistryTest {
    private class Module(
        override val id: String,
        override val options: List<Option<*>> = emptyList(),
        private val onStart: () -> Unit = {},
    ) : SereinModule {
        override fun start(context: ModuleContext) = onStart()
    }

    private val context = ModuleContext(Options { _, _ -> MemoryKeyValueStore() }) { _, _ -> }

    @Test
    fun rejectsDuplicateIds() {
        assertFailsWith<IllegalArgumentException> { ModuleRegistry(listOf(Module("ghost"), Module("ghost"))) }
    }

    @Test
    fun rejectsOptionKeysClaimedTwice() {
        val first = Module("ghost", listOf(booleanOption("hide_online")))
        val second = Module("online", listOf(booleanOption("hide_online")))
        assertFailsWith<IllegalArgumentException> { ModuleRegistry(listOf(first, second)) }
    }

    @Test
    fun aFailingModuleDoesNotStopTheOthers() {
        val started = mutableListOf<String>()
        val reported = mutableListOf<String>()
        val registry = ModuleRegistry(
            listOf(
                Module("broken") { throw NoSuchMethodError("upstream moved") },
                Module("ghost") { started += "ghost" },
            ),
        )
        registry.start(ModuleContext(context.options) { message, _ -> reported += message })
        assertEquals(listOf("ghost"), started)
        assertEquals(listOf("module broken failed to start"), reported)
    }

    @Test
    fun startsModulesInOrder() {
        val started = mutableListOf<String>()
        ModuleRegistry(listOf("a", "b", "c").map { id -> Module(id) { started += id } }).start(context)
        assertEquals(listOf("a", "b", "c"), started)
    }
}
