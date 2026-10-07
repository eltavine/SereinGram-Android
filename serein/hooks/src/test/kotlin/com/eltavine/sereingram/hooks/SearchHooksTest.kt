package com.eltavine.sereingram.hooks

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchHooksTest {
    @Test
    fun globalSearchStaysUnlessAPolicyTurnsItOff() {
        assertTrue(SearchHooks.allowsGlobalSearch())
        val broken = SearchHooks.globalSearchPolicies.install { throw IllegalStateException() }
        try {
            assertTrue(SearchHooks.allowsGlobalSearch())
            val off = SearchHooks.globalSearchPolicies.install { false }
            try {
                assertFalse(SearchHooks.allowsGlobalSearch())
            } finally {
                off.close()
            }
        } finally {
            broken.close()
        }
    }

    @Test
    fun theAppsTabStaysUnlessAPolicyHidesIt() {
        assertTrue(SearchHooks.showsAppsTab())
        val hidden = SearchHooks.appsTabPolicies.install { false }
        try {
            assertFalse(SearchHooks.showsAppsTab())
        } finally {
            hidden.close()
        }
    }
}
