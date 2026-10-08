package com.eltavine.sereingram.hooks

import java.net.InetAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NetworkHooksTest {
    private val loopback = InetAddress.getByAddress("telegram.example", byteArrayOf(127, 0, 0, 1))

    @Test
    fun theFirstResolverWithAnAnswerResolvesAndABrokenOneIsSkipped() {
        assertNull(NetworkHooks.resolve("telegram.example"))
        val installs = listOf(
            NetworkHooks.resolvers.install { throw IllegalStateException() },
            NetworkHooks.resolvers.install { null },
            NetworkHooks.resolvers.install { host -> if (host == "telegram.example") listOf(loopback) else emptyList() },
        )
        try {
            assertEquals(listOf(loopback), NetworkHooks.resolve("telegram.example"))
            assertEquals(emptyList(), NetworkHooks.resolve("nowhere.example"), "no addresses is an answer too")
        } finally {
            installs.forEach(AutoCloseable::close)
        }
    }
}
