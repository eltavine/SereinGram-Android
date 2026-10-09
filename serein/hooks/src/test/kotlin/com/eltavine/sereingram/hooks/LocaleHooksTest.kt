package com.eltavine.sereingram.hooks

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class LocaleHooksTest {
    @Test
    fun packsLoadInTheOrderTheirSourcesWereInstalled() {
        assertEquals(emptyList(), LocaleHooks.packsFor(Locale.ENGLISH))
        val installs = listOf(
            LocaleHooks.stringPacks.install { locale -> listOf("own_${locale.language}.bin") },
            LocaleHooks.stringPacks.install { throw IllegalStateException() },
            LocaleHooks.stringPacks.install { listOf("brand.bin") },
        )
        try {
            assertEquals(listOf("own_zh.bin", "brand.bin"), LocaleHooks.packsFor(Locale.SIMPLIFIED_CHINESE))
            assertEquals(emptyList(), LocaleHooks.packsFor(null))
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }
}
