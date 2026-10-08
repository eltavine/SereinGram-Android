package com.eltavine.sereingram.settings

import com.eltavine.sereingram.core.MemoryKeyValueStore
import com.eltavine.sereingram.core.OptionScope
import com.eltavine.sereingram.core.Options
import com.eltavine.sereingram.core.booleanOption
import com.eltavine.sereingram.core.intOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsStateTest {
    private val stores = HashMap<Pair<OptionScope, Int>, MemoryKeyValueStore>()
    private val options = Options { scope, account -> stores.getOrPut(scope to account) { MemoryKeyValueStore() } }

    private val saveDeleted = booleanOption("save_deleted", scope = OptionScope.ACCOUNT)
    private val seek = intOption("seek_seconds", default = 10)

    @Test
    fun accountOptionsAreThoseOfThePagesAccount() {
        val second = SettingsState(options, account = 1)
        second[saveDeleted] = true

        assertTrue(options.get(saveDeleted, account = 1))
        assertFalse(SettingsState(options, account = 0)[saveDeleted])
        assertEquals(1, second.accountOf(saveDeleted))
    }

    @Test
    fun deviceOptionsAreTheSameOnEveryAccountsPage() {
        SettingsState(options, account = 2)[seek] = 30

        assertEquals(30, SettingsState(options, account = 0)[seek])
        assertEquals(Options.NO_ACCOUNT, SettingsState(options, account = 2).accountOf(seek))
    }

    @Test
    fun resettingRestoresTheDefault() {
        val state = SettingsState(options, account = 0)
        state[seek] = 5
        assertTrue(state.isModified(seek))
        state.reset(seek)

        assertFalse(state.isModified(seek))
        assertEquals(10, state[seek])
    }

    @Test
    fun conditionsReadThePagesAccount() {
        val shown = SettingsCondition.isOn(saveDeleted)
        options.set(saveDeleted, true, account = 3)

        assertTrue(shown.holds(SettingsState(options, account = 3)))
        assertFalse(shown.holds(SettingsState(options, account = 0)))
    }

    @Test
    fun conditionsCombine() {
        val state = SettingsState(options, account = 0)
        val atThirty = SettingsCondition.isSetTo(seek, 30)
        val saving = SettingsCondition.isOn(saveDeleted)
        state[seek] = 30

        assertTrue((atThirty or saving).holds(state))
        assertFalse((atThirty and saving).holds(state))
        assertTrue((atThirty and !saving).holds(state))
        assertTrue(SettingsCondition.ALWAYS.holds(state))
    }
}
