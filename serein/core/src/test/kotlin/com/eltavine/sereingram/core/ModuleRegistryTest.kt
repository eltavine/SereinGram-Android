package com.eltavine.sereingram.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModuleRegistryTest {
    private class Module(
        override val id: String,
        override val options: List<Option<*>> = emptyList(),
        private val onForget: (Int) -> Unit = {},
        private val onStart: () -> Unit = {},
    ) : SereinModule {
        override fun start(context: ModuleContext) = onStart()

        override fun forgetAccount(account: Int) = onForget(account)
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

    @Test
    fun forgettingAnAccountResetsItsOptionsAndReachesEveryModule() {
        val locked = textOption("locked_chats", scope = OptionScope.ACCOUNT)
        val compact = booleanOption("compact_mode")
        val forgotten = mutableListOf<String>()
        val reported = mutableListOf<String>()
        val registry = ModuleRegistry(
            listOf(
                Module("lock", listOf(locked, compact), onForget = { throw IllegalStateException("store is gone") }),
                Module("names", onForget = { forgotten += "names $it" }),
            ),
        )
        val stores = HashMap<Pair<OptionScope, Int>, KeyValueStore>()
        val options = Options { scope, account -> stores.getOrPut(scope to account, ::MemoryKeyValueStore) }.apply {
            set(locked, "12", account = 0)
            set(locked, "34", account = 1)
            set(compact, true)
        }
        registry.forgetAccount(0, ModuleContext(options) { message, _ -> reported += message })
        assertFalse(options.isModified(locked, account = 0))
        assertEquals("34", options.get(locked, account = 1))
        assertTrue(options.get(compact), "device options are not the account's")
        assertEquals(listOf("names 0"), forgotten)
        assertEquals(listOf("module lock failed to forget an account"), reported)
    }
}
